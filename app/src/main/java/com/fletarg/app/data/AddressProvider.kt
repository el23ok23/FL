package com.fletarg.app.data

import android.content.Context
import android.location.Geocoder
import com.fletarg.core.AddressPoint
import com.fletarg.core.Argentina
import com.fletarg.core.CabaZone
import com.fletarg.core.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Una sugerencia del autocompletado. */
data class AddressSuggestion(
    val id: String,
    val primary: String,
    val secondary: String,
    val location: LatLng? = null,
    val countryCode: String? = null,
    val adminTexts: List<String> = emptyList(),
)

interface AddressProvider {
    suspend fun suggest(query: String, sessionToken: String): List<AddressSuggestion>
    suspend fun resolve(suggestion: AddressSuggestion, sessionToken: String): AddressPoint?
}

/** Etiqueta corta: "Av. Corrientes 1234, Buenos Aires" (calle y localidad). */
internal fun buildLabel(primary: String, secondary: String): String {
    val locality = secondary.split(",").map { it.trim() }.firstOrNull { it.isNotBlank() && !it.equals("Argentina", true) }
    return if (locality != null && !primary.contains(locality, ignoreCase = true)) "$primary, $locality" else primary
}

/** Google Places API (New) vía REST, sin SDK: autocompletado real en toda la Argentina. */
class GooglePlacesProvider(
    private val apiKey: String,
    private val packageName: String,
    private val certSha1: String,
) : AddressProvider {

    private val headers: Map<String, String>
        get() = buildMap {
            put("X-Goog-Api-Key", apiKey)
            put("X-Android-Package", packageName)
            if (certSha1.isNotEmpty()) put("X-Android-Cert", certSha1)
        }

    override suspend fun suggest(query: String, sessionToken: String): List<AddressSuggestion> {
        if (query.length < 3) return emptyList()
        val body = JSONObject()
            .put("input", query)
            .put("languageCode", "es-AR")
            .put("regionCode", "ar")
            .put("includedRegionCodes", JSONArray().put("ar"))
            .put("sessionToken", sessionToken)
            .put(
                "locationBias",
                JSONObject().put(
                    "rectangle",
                    JSONObject()
                        .put("low", JSONObject().put("latitude", Argentina.MIN_LAT).put("longitude", Argentina.MIN_LNG))
                        .put("high", JSONObject().put("latitude", Argentina.MAX_LAT).put("longitude", Argentina.MAX_LNG)),
                ),
            )
        val response = Http.request(
            url = "https://places.googleapis.com/v1/places:autocomplete",
            method = "POST",
            body = body.toString(),
            headers = headers,
        ) ?: return emptyList()

        return runCatching {
            val arr = JSONObject(response).optJSONArray("suggestions") ?: JSONArray()
            buildList {
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i)?.optJSONObject("placePrediction") ?: continue
                    val id = p.optString("placeId")
                    if (id.isBlank()) continue
                    val fmt = p.optJSONObject("structuredFormat")
                    val primary = fmt?.optJSONObject("mainText")?.optString("text")
                        ?: p.optJSONObject("text")?.optString("text") ?: continue
                    val secondary = fmt?.optJSONObject("secondaryText")?.optString("text") ?: ""
                    add(AddressSuggestion(id, primary, secondary))
                }
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun resolve(suggestion: AddressSuggestion, sessionToken: String): AddressPoint? {
        val url = "https://places.googleapis.com/v1/places/${suggestion.id}?languageCode=es-AR&sessionToken=$sessionToken"
        val response = Http.request(
            url = url,
            headers = headers + ("X-Goog-FieldMask" to "location,addressComponents"),
        ) ?: return null
        return runCatching {
            val obj = JSONObject(response)
            val loc = obj.getJSONObject("location")
            val point = LatLng(loc.getDouble("latitude"), loc.getDouble("longitude"))
            var country: String? = null
            val admin = ArrayList<String>()
            obj.optJSONArray("addressComponents")?.let { comps ->
                for (i in 0 until comps.length()) {
                    val c = comps.optJSONObject(i) ?: continue
                    val types = c.optJSONArray("types")?.let { t -> (0 until t.length()).map { t.optString(it) } }.orEmpty()
                    if ("country" in types) country = c.optString("shortText")
                    if ("administrative_area_level_1" in types || "administrative_area_level_2" in types || "locality" in types) {
                        admin += c.optString("longText")
                        admin += c.optString("shortText")
                    }
                }
            }
            val inside = Argentina.isInside(country, point)
            AddressPoint(
                label = buildLabel(suggestion.primary, suggestion.secondary),
                location = point,
                insideCoverage = inside,
                insideCaba = inside && CabaZone.isInside(point, admin),
            )
        }.getOrNull()
    }
}

/**
 * Respaldo sin clave: el geocodificador del propio Android (en la mayoría de los teléfonos lo
 * provee Google Play Services). No es un autocompletado real, funciona mejor con calle, altura y localidad.
 */
class GeocoderProvider(context: Context) : AddressProvider {
    private val appContext = context.applicationContext
    private val locale = Locale("es", "AR")

    override suspend fun suggest(query: String, sessionToken: String): List<AddressSuggestion> {
        val q = query.trim()
        if (q.length < 4 || !Geocoder.isPresent()) return emptyList()
        return geocode(q).ifEmpty {
            if ("argentina" in q.lowercase()) emptyList() else geocode("$q, Argentina")
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocode(q: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(appContext, locale)
            val addresses = geocoder.getFromLocationName(
                q, 6,
                Argentina.MIN_LAT, Argentina.MIN_LNG,
                Argentina.MAX_LAT, Argentina.MAX_LNG,
            ).orEmpty()
            val seen = HashSet<String>()
            buildList {
                for (a in addresses) {
                    if (!a.hasLatitude() || !a.hasLongitude()) continue
                    val street = listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(" ").trim()
                    val primary = street.ifBlank { a.getAddressLine(0)?.substringBefore(",").orEmpty() }
                    if (primary.isBlank()) continue
                    val secondary = listOfNotNull(a.locality ?: a.subAdminArea, a.adminArea)
                        .distinct().joinToString(", ")
                    val key = "$primary|$secondary"
                    if (!seen.add(key)) continue
                    add(
                        AddressSuggestion(
                            id = key,
                            primary = primary,
                            secondary = secondary,
                            location = LatLng(a.latitude, a.longitude),
                            countryCode = a.countryCode,
                            adminTexts = listOfNotNull(a.adminArea, a.subAdminArea, a.locality),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun resolve(suggestion: AddressSuggestion, sessionToken: String): AddressPoint? {
        val point = suggestion.location ?: return null
        val inside = Argentina.isInside(suggestion.countryCode, point)
        return AddressPoint(
            label = buildLabel(suggestion.primary, suggestion.secondary),
            location = point,
            insideCoverage = inside,
            insideCaba = inside && CabaZone.isInside(point, suggestion.adminTexts),
        )
    }
}

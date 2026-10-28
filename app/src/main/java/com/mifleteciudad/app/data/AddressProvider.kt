package com.mifleteciudad.app.data

import android.content.Context
import android.location.Geocoder
import com.mifleteciudad.core.AddressPoint
import com.mifleteciudad.core.ServiceZone
import com.mifleteciudad.core.LatLng
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
    val adminTexts: List<String> = emptyList(),
)

interface AddressProvider {
    suspend fun suggest(query: String, sessionToken: String): List<AddressSuggestion>
    suspend fun resolve(suggestion: AddressSuggestion, sessionToken: String): AddressPoint?
}

/** Arma la etiqueta corta que se muestra y se manda por WhatsApp. */
internal fun buildLabel(primary: String, inside: Boolean, zone: ServiceZone): String =
    if (inside && zone.suffix.isNotBlank()) "$primary, ${zone.suffix}" else primary

/** Google Places API (New) vía REST, sin SDK: autocompletado real y detalles con coordenadas. */
class GooglePlacesProvider(
    private val apiKey: String,
    private val packageName: String,
    private val certSha1: String,
    private val zone: ServiceZone,
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
                "locationRestriction",
                JSONObject().put(
                    "rectangle",
                    JSONObject()
                        .put("low", JSONObject().put("latitude", zone.minLat).put("longitude", zone.minLng))
                        .put("high", JSONObject().put("latitude", zone.maxLat).put("longitude", zone.maxLng)),
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
            val admin = ArrayList<String>()
            obj.optJSONArray("addressComponents")?.let { comps ->
                for (i in 0 until comps.length()) {
                    val c = comps.optJSONObject(i) ?: continue
                    val types = c.optJSONArray("types")?.let { t -> (0 until t.length()).map { t.optString(it) } }.orEmpty()
                    if ("administrative_area_level_1" in types || "locality" in types || "administrative_area_level_2" in types) {
                        admin += c.optString("longText")
                        admin += c.optString("shortText")
                    }
                }
            }
            val inside = zone.isInside(point, admin)
            AddressPoint(buildLabel(suggestion.primary, inside, zone), point, inside)
        }.getOrNull()
    }
}

/**
 * Respaldo sin clave: el geocodificador del propio Android (en la mayoría de los teléfonos lo
 * provee Google Play Services). No es un autocompletado real, funciona mejor con calle y altura.
 */
class GeocoderProvider(context: Context, private val zone: ServiceZone) : AddressProvider {
    private val appContext = context.applicationContext
    private val locale = Locale("es", "AR")

    override suspend fun suggest(query: String, sessionToken: String): List<AddressSuggestion> {
        val q = query.trim()
        if (q.length < 4 || !Geocoder.isPresent()) return emptyList()
        val results = geocode(q).ifEmpty {
            val hint = zone.searchHint
            if (hint.isBlank() || hint.lowercase() in q.lowercase()) emptyList() else geocode("$q, $hint")
        }
        return results
    }

    @Suppress("DEPRECATION")
    private suspend fun geocode(q: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(appContext, locale)
            val addresses = geocoder.getFromLocationName(
                q, 6,
                zone.minLat - 0.02, zone.minLng - 0.02,
                zone.maxLat + 0.02, zone.maxLng + 0.02,
            ).orEmpty()
            val seen = HashSet<String>()
            buildList {
                for (a in addresses) {
                    if (!a.hasLatitude() || !a.hasLongitude()) continue
                    val street = listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(" ").trim()
                    val primary = street.ifBlank { a.getAddressLine(0)?.substringBefore(",").orEmpty() }
                    if (primary.isBlank()) continue
                    val secondary = listOfNotNull(a.subLocality, a.locality ?: a.adminArea)
                        .distinct().joinToString(", ")
                    val key = "$primary|$secondary"
                    if (!seen.add(key)) continue
                    add(
                        AddressSuggestion(
                            id = key,
                            primary = primary,
                            secondary = secondary,
                            location = LatLng(a.latitude, a.longitude),
                            adminTexts = listOfNotNull(a.adminArea, a.subAdminArea, a.locality),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun resolve(suggestion: AddressSuggestion, sessionToken: String): AddressPoint? {
        val point = suggestion.location ?: return null
        val inside = zone.isInside(point, suggestion.adminTexts)
        return AddressPoint(buildLabel(suggestion.primary, inside, zone), point, inside)
    }
}

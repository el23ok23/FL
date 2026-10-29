package com.fletaba.app.data

import android.content.Context
import android.location.Geocoder
import com.fletaba.core.AddressPoint
import com.fletaba.core.Argentina
import com.fletaba.core.CabaZone
import com.fletaba.core.LatLng
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

/** Dónde buscar: el origen solo en Capital Federal; paradas y destino en todo el país. */
enum class SearchScope { CABA, ARGENTINA }

interface AddressProvider {
    suspend fun suggest(query: String, sessionToken: String, scope: SearchScope): List<AddressSuggestion>
    suspend fun resolve(suggestion: AddressSuggestion, sessionToken: String): AddressPoint?
}

/** Etiqueta corta: calle y altura + barrio y localidad, p. ej. "Lima 29, Monserrat, Buenos Aires". */
internal fun buildLabel(primary: String, secondary: String): String {
    val parts = secondary.split(",").map { it.trim() }
        .filter { it.isNotBlank() && !it.equals("Argentina", true) && !primary.contains(it, ignoreCase = true) }
        .distinct()
        .take(2)
    return if (parts.isEmpty()) primary else "$primary, ${parts.joinToString(", ")}"
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

    override suspend fun suggest(query: String, sessionToken: String, scope: SearchScope): List<AddressSuggestion> {
        if (query.length < 3) return emptyList()
        fun rect(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) = JSONObject().put(
            "rectangle",
            JSONObject()
                .put("low", JSONObject().put("latitude", minLat).put("longitude", minLng))
                .put("high", JSONObject().put("latitude", maxLat).put("longitude", maxLng)),
        )
        val body = JSONObject()
            .put("input", query)
            .put("languageCode", "es-AR")
            .put("regionCode", "ar")
            .put("includedRegionCodes", JSONArray().put("ar"))
            .put("sessionToken", sessionToken)
        if (scope == SearchScope.CABA) {
            // Solo resultados dentro de Capital Federal.
            body.put("locationRestriction", rect(CabaZone.minLat, CabaZone.minLng, CabaZone.maxLat, CabaZone.maxLng))
        } else {
            body.put("locationBias", rect(Argentina.MIN_LAT, Argentina.MIN_LNG, Argentina.MAX_LAT, Argentina.MAX_LNG))
        }
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

    override suspend fun suggest(query: String, sessionToken: String, scope: SearchScope): List<AddressSuggestion> {
        val q = query.trim()
        if (q.length < 4 || !Geocoder.isPresent()) return emptyList()
        val lower = q.lowercase()
        return if (scope == SearchScope.CABA) {
            // Origen: solo Capital Federal. Se busca acotado a la Ciudad y se descarta lo que quede afuera.
            val withCity = if ("caba" in lower || "buenos aires" in lower || "capital" in lower) q else "$q, Ciudad Autónoma de Buenos Aires"
            geocode(withCity, scope).ifEmpty { geocode(q, scope) }
                .filter { s -> s.location != null && CabaZone.isInside(s.location, s.adminTexts) }
        } else {
            geocode(q, scope).ifEmpty { if ("argentina" in lower) emptyList() else geocode("$q, Argentina", scope) }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocode(q: String, scope: SearchScope): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(appContext, locale)
            val addresses = if (scope == SearchScope.CABA) {
                geocoder.getFromLocationName(q, 6, CabaZone.minLat - 0.01, CabaZone.minLng - 0.01, CabaZone.maxLat + 0.01, CabaZone.maxLng + 0.01)
            } else {
                geocoder.getFromLocationName(q, 6, Argentina.MIN_LAT, Argentina.MIN_LNG, Argentina.MAX_LAT, Argentina.MAX_LNG)
            }.orEmpty()
            val seen = HashSet<String>()
            buildList {
                for (a in addresses) {
                    if (!a.hasLatitude() || !a.hasLongitude()) continue
                    val street = listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(" ").trim()
                    val primary = street.ifBlank { a.getAddressLine(0)?.substringBefore(",").orEmpty() }
                    if (primary.isBlank()) continue
                    // Barrio (si lo hay), localidad y provincia: "Monserrat, Buenos Aires"
                    val secondary = listOfNotNull(a.subLocality, a.locality ?: a.subAdminArea, a.adminArea)
                        .filter { it.isNotBlank() }.distinct().joinToString(", ")
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

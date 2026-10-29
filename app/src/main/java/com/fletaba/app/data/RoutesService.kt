package com.fletaba.app.data

import com.fletaba.core.Geo
import com.fletaba.core.LatLng
import org.json.JSONArray
import org.json.JSONObject

/** Recorrido completo por todas las paradas. */
data class RouteInfo(
    val distanceKm: Double,
    val durationMin: Int?,
    val path: List<LatLng>,
    /** Polilínea codificada de Google (para el mapa estático). */
    val encodedPolyline: String? = null,
)

/** Google Routes API vía REST, con paradas intermedias. Devuelve null si falla (la app estima). */
class RoutesService(
    private val apiKey: String,
    private val packageName: String,
    private val certSha1: String,
) {
    suspend fun compute(points: List<LatLng>): RouteInfo? {
        if (points.size < 2) return null
        fun waypoint(p: LatLng) = JSONObject().put(
            "location",
            JSONObject().put("latLng", JSONObject().put("latitude", p.lat).put("longitude", p.lng)),
        )
        val body = JSONObject()
            .put("origin", waypoint(points.first()))
            .put("destination", waypoint(points.last()))
            .put("travelMode", "DRIVE")
            .put("routingPreference", "TRAFFIC_UNAWARE")
            .put("languageCode", "es-AR")
            .put("units", "METRIC")
        val middle = points.subList(1, points.size - 1)
        if (middle.isNotEmpty()) {
            body.put("intermediates", JSONArray().apply { middle.forEach { put(waypoint(it)) } })
        }
        val headers = buildMap {
            put("X-Goog-Api-Key", apiKey)
            put("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline")
            put("X-Android-Package", packageName)
            if (certSha1.isNotEmpty()) put("X-Android-Cert", certSha1)
        }
        val response = Http.request(
            url = "https://routes.googleapis.com/directions/v2:computeRoutes",
            method = "POST",
            body = body.toString(),
            headers = headers,
        ) ?: return null
        return runCatching {
            val route = JSONObject(response).getJSONArray("routes").getJSONObject(0)
            val meters = route.getInt("distanceMeters")
            val seconds = route.optString("duration").removeSuffix("s").toIntOrNull()
            val encoded = route.optJSONObject("polyline")?.optString("encodedPolyline").orEmpty()
            RouteInfo(
                distanceKm = meters / 1000.0,
                durationMin = seconds?.let { (it + 59) / 60 },
                path = if (encoded.isNotEmpty()) Geo.decodePolyline(encoded) else points,
                encodedPolyline = encoded.ifEmpty { null },
            )
        }.getOrNull()
    }
}

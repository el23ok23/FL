package com.mifleteciudad.app.data

import com.mifleteciudad.core.Geo
import com.mifleteciudad.core.LatLng
import org.json.JSONObject

/** Recorrido entre origen y destino. */
data class RouteInfo(
    val distanceKm: Double,
    val durationMin: Int?,
    val path: List<LatLng>,
)

/** Google Routes API vía REST. Devuelve null si no hay clave o falla (la app estima la distancia). */
class RoutesService(
    private val apiKey: String,
    private val packageName: String,
    private val certSha1: String,
) {
    suspend fun compute(origin: LatLng, destination: LatLng): RouteInfo? {
        fun waypoint(p: LatLng) = JSONObject().put(
            "location",
            JSONObject().put("latLng", JSONObject().put("latitude", p.lat).put("longitude", p.lng)),
        )
        val body = JSONObject()
            .put("origin", waypoint(origin))
            .put("destination", waypoint(destination))
            .put("travelMode", "DRIVE")
            .put("routingPreference", "TRAFFIC_UNAWARE")
            .put("languageCode", "es-AR")
            .put("units", "METRIC")
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
                path = if (encoded.isNotEmpty()) Geo.decodePolyline(encoded) else listOf(origin, destination),
            )
        }.getOrNull()
    }
}

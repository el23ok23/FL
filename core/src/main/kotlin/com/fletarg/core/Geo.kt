package com.fletarg.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Coordenada geográfica. */
data class LatLng(val lat: Double, val lng: Double)

object Geo {
    private const val EARTH_RADIUS_KM = 6371.0088

    /** Distancia en línea recta (gran círculo) en kilómetros. */
    fun haversineKm(a: LatLng, b: LatLng): Double {
        val dLat = (b.lat - a.lat).toRadians()
        val dLng = (b.lng - a.lng).toRadians()
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(a.lat.toRadians()) * cos(b.lat.toRadians()) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Decodifica una polilínea codificada de Google (formato "encoded polyline"). */
    fun decodePolyline(encoded: String): List<LatLng> {
        val out = ArrayList<LatLng>(encoded.length / 4)
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var b: Int
            do {
                if (index >= encoded.length) return out
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            result = 0
            shift = 0
            do {
                if (index >= encoded.length) return out
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            out.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return out
    }

    private fun Double.toRadians() = this * PI / 180.0
}

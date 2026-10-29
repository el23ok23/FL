package com.fletarg.core

/**
 * Ciudad Autónoma de Buenos Aires (Capital Federal): todo flete tiene que partir de acá.
 * Polígono simplificado del límite real (Av. General Paz, Riachuelo y Río de la Plata),
 * con precisión de algunos cientos de metros en los bordes.
 */
object CabaZone {
    val polygon: List<LatLng> = listOf(
        LatLng(-34.5270, -58.4600), LatLng(-34.5335, -58.4490), LatLng(-34.5385, -58.4360),
        LatLng(-34.5460, -58.4280), LatLng(-34.5520, -58.4210), LatLng(-34.5650, -58.4040),
        LatLng(-34.5720, -58.3950), LatLng(-34.5770, -58.3850), LatLng(-34.5800, -58.3650),
        LatLng(-34.5900, -58.3600), LatLng(-34.5970, -58.3480), LatLng(-34.6060, -58.3400),
        LatLng(-34.6180, -58.3380), LatLng(-34.6260, -58.3440), LatLng(-34.6330, -58.3490),
        LatLng(-34.6375, -58.3510), LatLng(-34.6400, -58.3580), LatLng(-34.6410, -58.3640),
        LatLng(-34.6470, -58.3680), LatLng(-34.6520, -58.3715), LatLng(-34.6500, -58.3830),
        LatLng(-34.6525, -58.3940), LatLng(-34.6560, -58.4040), LatLng(-34.6575, -58.4110),
        LatLng(-34.6640, -58.4220), LatLng(-34.6720, -58.4300), LatLng(-34.6830, -58.4400),
        LatLng(-34.6900, -58.4470), LatLng(-34.6980, -58.4560), LatLng(-34.7035, -58.4665),
        LatLng(-34.7000, -58.4740), LatLng(-34.6930, -58.4830), LatLng(-34.6850, -58.4950),
        LatLng(-34.6760, -58.5100), LatLng(-34.6680, -58.5210), LatLng(-34.6560, -58.5290),
        LatLng(-34.6420, -58.5310), LatLng(-34.6250, -58.5300), LatLng(-34.6090, -58.5285),
        LatLng(-34.5960, -58.5240), LatLng(-34.5855, -58.5145), LatLng(-34.5720, -58.5060),
        LatLng(-34.5600, -58.4990), LatLng(-34.5520, -58.4880), LatLng(-34.5445, -58.4790),
        LatLng(-34.5380, -58.4720), LatLng(-34.5345, -58.4680),
    )

    val minLat = polygon.minOf { it.lat }
    val maxLat = polygon.maxOf { it.lat }
    val minLng = polygon.minOf { it.lng }
    val maxLng = polygon.maxOf { it.lng }

    /** Punto dentro del polígono (ray casting). */
    fun contains(p: LatLng): Boolean {
        if (p.lat < minLat || p.lat > maxLat || p.lng < minLng || p.lng > maxLng) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            val intersects = (a.lng > p.lng) != (b.lng > p.lng) &&
                p.lat < (b.lat - a.lat) * (p.lng - a.lng) / (b.lng - a.lng) + a.lat
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    /**
     * Combina lo que informa el servicio de direcciones (provincia / localidad) con el polígono:
     * "Provincia" → fuera; "Autónoma", "CABA" o "Capital Federal" → dentro; si no, el polígono.
     */
    fun isInside(point: LatLng, adminTexts: List<String?> = emptyList()): Boolean {
        val joined = adminTexts.filterNotNull().joinToString(" ").lowercase()
        if (joined.isNotBlank()) {
            if ("provincia" in joined) return false
            if ("autónoma" in joined || "autonoma" in joined || "caba" in joined || "capital federal" in joined) return true
        }
        return contains(point)
    }
}

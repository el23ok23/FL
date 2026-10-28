package com.mifleteciudad.core

/**
 * Zona de cobertura: Ciudad Autónoma de Buenos Aires.
 *
 * El polígono es una simplificación del límite real (Av. General Paz, Riachuelo y
 * Río de la Plata) con precisión de algunos cientos de metros en los bordes.
 * Se usa para el mapa esquemático y como respaldo para decidir si una dirección
 * está dentro de la Ciudad cuando el servicio de direcciones no lo informa.
 */
object CabaZone {
    val polygon: List<LatLng> = listOf(
        LatLng(-34.5270, -58.4600), // Gral. Paz y Río de la Plata
        LatLng(-34.5335, -58.4490),
        LatLng(-34.5385, -58.4360), // Parque de la Memoria
        LatLng(-34.5460, -58.4280),
        LatLng(-34.5520, -58.4210), // Aeroparque (norte)
        LatLng(-34.5650, -58.4040),
        LatLng(-34.5720, -58.3950),
        LatLng(-34.5770, -58.3850),
        LatLng(-34.5800, -58.3650), // Puerto Nuevo
        LatLng(-34.5900, -58.3600),
        LatLng(-34.5970, -58.3480), // Reserva Ecológica (norte)
        LatLng(-34.6060, -58.3400),
        LatLng(-34.6180, -58.3380),
        LatLng(-34.6260, -58.3440),
        LatLng(-34.6330, -58.3490),
        LatLng(-34.6375, -58.3510), // Boca del Riachuelo
        LatLng(-34.6400, -58.3580),
        LatLng(-34.6410, -58.3640), // Vuelta de Rocha
        LatLng(-34.6470, -58.3680),
        LatLng(-34.6520, -58.3715), // Puente Pueyrredón
        LatLng(-34.6500, -58.3830),
        LatLng(-34.6525, -58.3940),
        LatLng(-34.6560, -58.4040),
        LatLng(-34.6575, -58.4110), // Puente Alsina
        LatLng(-34.6640, -58.4220),
        LatLng(-34.6720, -58.4300),
        LatLng(-34.6830, -58.4400),
        LatLng(-34.6900, -58.4470),
        LatLng(-34.6980, -58.4560),
        LatLng(-34.7035, -58.4665), // Puente La Noria
        LatLng(-34.7000, -58.4740),
        LatLng(-34.6930, -58.4830),
        LatLng(-34.6850, -58.4950),
        LatLng(-34.6760, -58.5100),
        LatLng(-34.6680, -58.5210), // Mataderos
        LatLng(-34.6560, -58.5290),
        LatLng(-34.6420, -58.5310), // Liniers
        LatLng(-34.6250, -58.5300),
        LatLng(-34.6090, -58.5285), // Devoto
        LatLng(-34.5960, -58.5240),
        LatLng(-34.5855, -58.5145),
        LatLng(-34.5720, -58.5060),
        LatLng(-34.5600, -58.4990),
        LatLng(-34.5520, -58.4880),
        LatLng(-34.5445, -58.4790), // Puente Saavedra
        LatLng(-34.5380, -58.4720),
        LatLng(-34.5345, -58.4680),
    )

    /** Rectángulo que contiene la Ciudad (para restringir búsquedas). */
    val minLat = polygon.minOf { it.lat }
    val maxLat = polygon.maxOf { it.lat }
    val minLng = polygon.minOf { it.lng }
    val maxLng = polygon.maxOf { it.lng }

    /** Centro aproximado (Obelisco). */
    val center = LatLng(-34.6037, -58.3816)

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
     * Decide si una dirección está en la Ciudad combinando lo que informa el servicio
     * de direcciones (provincia / área administrativa) con el polígono.
     *
     * - Si el texto administrativo menciona "Provincia" → fuera.
     * - Si menciona "Autónoma", "CABA" o "Capital Federal" → dentro.
     * - Si no hay información → se usa el polígono.
     */
    fun isInside(point: LatLng, adminTexts: List<String?> = emptyList()): Boolean {
        val joined = adminTexts.filterNotNull().joinToString(" ").lowercase()
        if (joined.isNotBlank()) {
            if (joined.contains("provincia")) return false
            if (joined.contains("autónoma") || joined.contains("autonoma") ||
                joined.contains("caba") || joined.contains("capital federal")
            ) return true
        }
        return contains(point)
    }
}

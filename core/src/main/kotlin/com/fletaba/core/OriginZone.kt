package com.fletaba.core

/**
 * Zona de ORIGEN: todo flete tiene que salir de acá (Capital Federal, Salto, etc.).
 * Polígono simplificado más palabras que ayudan a decidir con lo que informa el servicio de direcciones.
 * Se configura en config.json ("origen"); el destino y las paradas pueden ser en todo el país.
 */
class OriginZone(
    /** Nombre para mostrar: "Capital Federal", "Salto". */
    val name: String,
    /** Nombre corto para etiquetas: "CABA", "Salto". */
    val shortName: String,
    /** Texto que se agrega a la búsqueda para acotarla: "Ciudad Autónoma de Buenos Aires". */
    val searchHint: String,
    val polygon: List<LatLng>,
    val insideKeywords: List<String> = emptyList(),
    val outsideKeywords: List<String> = emptyList(),
    /** Calles para autocompletar el origen sin internet (opcional). */
    val streets: List<String> = emptyList(),
) {
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

    /** Palabras de "fuera" → fuera; palabras de "dentro" → dentro; si no, decide el polígono. */
    fun isInside(point: LatLng, adminTexts: List<String?> = emptyList()): Boolean {
        val joined = adminTexts.filterNotNull().joinToString(" ").lowercase()
        if (joined.isNotBlank()) {
            if (outsideKeywords.any { it.lowercase() in joined }) return false
            if (insideKeywords.any { it.lowercase() in joined }) return true
        }
        return contains(point)
    }

    companion object {
        /** Ciudad Autónoma de Buenos Aires (Av. General Paz, Riachuelo y Río de la Plata), simplificada. */
        val CABA = OriginZone(
            name = "Capital Federal",
            shortName = "CABA",
            searchHint = "Ciudad Autónoma de Buenos Aires",
            insideKeywords = listOf("Autónoma", "Autonoma", "CABA", "Capital Federal"),
            outsideKeywords = listOf("Provincia"),
            polygon = listOf(
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
            ),
        )

        /** Partido de Salto (Provincia de Buenos Aires), contorno aproximado. */
        val SALTO = OriginZone(
            name = "Salto",
            shortName = "Salto",
            searchHint = "Salto, Buenos Aires",
            polygon = listOf(
                LatLng(-34.0500, -60.4500), LatLng(-34.0800, -60.2500), LatLng(-34.1500, -60.0500),
                LatLng(-34.3000, -59.9800), LatLng(-34.4200, -60.0500), LatLng(-34.5000, -60.2500),
                LatLng(-34.4800, -60.5000), LatLng(-34.4000, -60.6200), LatLng(-34.2500, -60.6000),
                LatLng(-34.1200, -60.5500),
            ),
            streets = listOf(
                "Buenos Aires", "Mitre", "Belgrano", "San Martín", "25 de Mayo", "9 de Julio", "Sarmiento",
                "Rivadavia", "Moreno", "Alsina", "Pellegrini", "Italia", "Pringles", "Dorrego", "Lavalle",
                "Urquiza", "Juan B. Justo", "Hipólito Yrigoyen", "Chacabuco", "Brown", "French", "Beruti",
                "Zabala", "Azcuénaga", "Roca", "Güemes", "Lamadrid", "Las Heras", "Alvear", "Saavedra",
                "Colón", "España", "Francia", "Independencia", "Libertad", "Tucumán", "Salta", "Córdoba",
                "Mendoza", "Entre Ríos", "Corrientes", "Santa Fe", "Av. Costanera", "Ruta 31", "Ruta 32",
            ),
        )
    }
}

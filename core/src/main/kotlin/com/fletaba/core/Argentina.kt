package com.fletaba.core

/** Cobertura nacional: todo el territorio argentino. */
object Argentina {
    const val COUNTRY_CODE = "AR"

    // Rectángulo que contiene al país continental (para sesgar búsquedas y encuadrar el mapa).
    const val MIN_LAT = -55.1
    const val MAX_LAT = -21.7
    const val MIN_LNG = -73.6
    const val MAX_LNG = -53.6

    val center = LatLng(-38.4, -63.6)

    /**
     * Decide si una dirección está en Argentina a partir de lo que informa el servicio de
     * direcciones. Sin información, se confía en que el servicio ya filtró por país.
     */
    fun isInside(countryCode: String?, point: LatLng? = null): Boolean {
        val code = countryCode?.trim()?.uppercase()
        if (!code.isNullOrEmpty()) return code == COUNTRY_CODE
        if (point != null) {
            return point.lat in MIN_LAT..MAX_LAT && point.lng in MIN_LNG..MAX_LNG
        }
        return true
    }
}

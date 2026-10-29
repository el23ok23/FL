package com.fletarg.core

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Tipos de vehículo (el orden es el del carrusel). */
enum class VehicleKind {
    KANGOO,
    FURGON_MEDIANO,
    FURGON_GRANDE,
    PICKUP,
    CAMION_3_4,
    CAMION_MEDIANO_CERRADO,
    CAMION_GRANDE_CERRADO,
    CAMION_PLAYO;

    companion object {
        /** Id tal como aparece en config.json. */
        fun fromId(id: String): VehicleKind? = when (id.trim().lowercase()) {
            "kangoo" -> KANGOO
            "furgon_mediano" -> FURGON_MEDIANO
            "furgon_grande" -> FURGON_GRANDE
            "pickup" -> PICKUP
            "camion_3_4" -> CAMION_3_4
            "camion_mediano_cerrado" -> CAMION_MEDIANO_CERRADO
            "camion_grande_cerrado" -> CAMION_GRANDE_CERRADO
            "camion_playo" -> CAMION_PLAYO
            else -> null
        }
    }
}

/** Un vehículo tal como se configura en config.json. */
data class VehicleSpec(
    val kind: VehicleKind,
    val name: String,
    val description: String,
    val dimensions: String,
    val capacity: String,
    val baseFare: Int,
    val perKm: Int,
)

/** Valores generales de la tarifa (config.json → "precios"). */
data class PricingConfig(
    val perHelper: Int,
    val perStairFloor: Int,
    val perExtraStop: Int,
    val roundTo: Int,
    val urbanDistanceFactor: Double,
    val minKm: Double,
)

enum class Access { GROUND, ELEVATOR, STAIRS }

data class FloorInfo(val access: Access = Access.GROUND, val floors: Int = 0) {
    /** Pisos que se cobran (solo cuando se sube por escalera). */
    val stairFloors: Int get() = if (access == Access.STAIRS) max(0, floors) else 0

    fun describe(): String = when (access) {
        Access.GROUND -> "Planta baja"
        Access.ELEVATOR -> if (floors <= 0) "Planta baja (con ascensor)" else "Piso $floors por ascensor"
        Access.STAIRS -> if (floors <= 0) "Planta baja" else "Piso $floors por escalera"
    }
}

data class PriceBreakdown(
    val base: Int,
    val distance: Int,
    val helpers: Int,
    val stairs: Int,
    val extraStops: Int,
    val km: Double,
    val stairFloors: Int,
    val helperCount: Int,
    val extraStopCount: Int,
    val total: Int,
)

object PriceCalculator {
    /**
     * Precio = tarifa base del vehículo + valor por km (recorrido completo) + monto por ayudante
     *          + recargo por cada piso por escalera (en todas las paradas)
     *          + recargo por cada parada adicional (más allá de origen y destino).
     */
    fun calculate(
        vehicle: VehicleSpec,
        distanceKm: Double,
        helpers: Int,
        floors: List<FloorInfo>,
        config: PricingConfig,
    ): PriceBreakdown {
        val km = max(config.minKm, distanceKm)
        val distanceCost = (km * vehicle.perKm).roundToInt()
        val helperCount = helpers.coerceIn(0, 2)
        val helpersCost = helperCount * config.perHelper
        val stairFloors = floors.sumOf { it.stairFloors }
        val stairsCost = stairFloors * config.perStairFloor
        val extraStopCount = max(0, floors.size - 2)
        val extraStopsCost = extraStopCount * config.perExtraStop
        val raw = vehicle.baseFare + distanceCost + helpersCost + stairsCost + extraStopsCost
        return PriceBreakdown(
            base = vehicle.baseFare,
            distance = distanceCost,
            helpers = helpersCost,
            stairs = stairsCost,
            extraStops = extraStopsCost,
            km = km,
            stairFloors = stairFloors,
            helperCount = helperCount,
            extraStopCount = extraStopCount,
            total = roundUp(raw, config.roundTo),
        )
    }

    /** Distancia total de un recorrido con varias paradas, estimada por calle. */
    fun estimateRoadKm(points: List<LatLng>, config: PricingConfig): Double =
        points.zipWithNext().sumOf { (a, b) -> Geo.haversineKm(a, b) } * config.urbanDistanceFactor

    /** Distancia estimada por calle cuando no hay servicio de rutas. */
    fun estimateRoadKm(a: LatLng, b: LatLng, config: PricingConfig): Double =
        Geo.haversineKm(a, b) * config.urbanDistanceFactor

    fun roundUp(amount: Int, step: Int): Int {
        if (step <= 1) return amount
        return (ceil(amount / step.toDouble()) * step).toInt()
    }
}

object Money {
    /** "$ 85.000" (formato argentino, sin decimales). */
    fun formatArs(amount: Int): String {
        val digits = amount.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            sb.append(digits[i])
            count++
            if (count % 3 == 0 && i > 0) sb.append('.')
        }
        return "$ " + sb.reverse().toString()
    }

    /** "7,8 km" */
    fun formatKm(km: Double): String {
        val tenths = (km * 10).roundToInt()
        return "${tenths / 10},${tenths % 10} km"
    }
}

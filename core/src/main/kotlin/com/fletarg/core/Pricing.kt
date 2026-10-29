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
    /** Base por ayudante (lo que cobra por presentarse). */
    val perHelper: Int,
    /** Lo que suma cada ayudante por cada km del recorrido. */
    val perHelperKm: Int,
    val perStairFloor: Int,
    val perExtraStop: Int,
    val roundTo: Int,
    val urbanDistanceFactor: Double,
    val minKm: Double,
    /** Recargo operativo (nafta, seguro, peajes, desgaste) sobre el costo base, p. ej. 0.20 = 20 %. Invisible para el cliente. */
    val operatingPct: Double = 0.0,
    /** Comisión del dueño de la app sobre el precio bruto, p. ej. 0.15 = 15 %. Invisible para el cliente. */
    val commissionPct: Double = 0.0,
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

/**
 * Desglose que ve el cliente. Las líneas ya incluyen, repartidos proporcionalmente, el recargo
 * operativo y la comisión, así que suman exactamente [total] y nada de eso aparece por separado.
 * [commission], [netForDriver] y [subtotal] son solo para el dueño (mensaje privado y webhook).
 */
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
    val subtotal: Int,
    val operatingCost: Int,
    val commission: Int,
    val netForDriver: Int,
)

object PriceCalculator {
    /**
     * Precio = tarifa base del vehículo + valor por km (recorrido completo)
     *          + por cada ayudante: base + valor por km (acompaña la distancia del viaje)
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
        val helpersCost = helperCount * helperCost(km, config)
        val stairFloors = floors.sumOf { it.stairFloors }
        val stairsCost = stairFloors * config.perStairFloor
        val extraStopCount = max(0, floors.size - 2)
        val extraStopsCost = extraStopCount * config.perExtraStop
        val subtotal = vehicle.baseFare + distanceCost + helpersCost + stairsCost + extraStopsCost

        // Bruto = subtotal × (1 + operativo) / (1 − comisión): así la comisión es exactamente
        // el porcentaje pedido del precio final que paga el cliente.
        val total = roundUp((subtotal * grossFactor(config)).roundToInt(), config.roundTo)
        val commission = (total * config.commissionPct).roundToInt()
        val operatingCost = ((total - commission) - subtotal).coerceAtLeast(0)

        // Reparto proporcional del bruto entre las líneas visibles (la base absorbe el redondeo).
        val scale = if (subtotal > 0) total.toDouble() / subtotal else 1.0
        val distanceShown = (distanceCost * scale).roundToInt()
        val helpersShown = (helpersCost * scale).roundToInt()
        val stairsShown = (stairsCost * scale).roundToInt()
        val extraStopsShown = (extraStopsCost * scale).roundToInt()
        val baseShown = total - distanceShown - helpersShown - stairsShown - extraStopsShown

        return PriceBreakdown(
            base = baseShown,
            distance = distanceShown,
            helpers = helpersShown,
            stairs = stairsShown,
            extraStops = extraStopsShown,
            km = km,
            stairFloors = stairFloors,
            helperCount = helperCount,
            extraStopCount = extraStopCount,
            total = total,
            subtotal = subtotal,
            operatingCost = operatingCost,
            commission = commission,
            netForDriver = total - commission,
        )
    }

    /** Factor que convierte un costo base en precio bruto para el cliente. */
    fun grossFactor(config: PricingConfig): Double {
        val com = config.commissionPct.coerceIn(0.0, 0.9)
        return (1 + config.operatingPct.coerceAtLeast(0.0)) / (1 - com)
    }

    /** Un costo base expresado como precio para el cliente (para mostrar "desde", "+ $" de ayudantes, etc.). */
    fun gross(amount: Int, config: PricingConfig): Int = (amount * grossFactor(config)).roundToInt()

    /** Costo de un ayudante para un recorrido de [km] kilómetros: base + valor por km. */
    fun helperCost(km: Double, config: PricingConfig): Int =
        config.perHelper + (max(config.minKm, km) * config.perHelperKm).roundToInt()

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

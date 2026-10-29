package com.fletarg.core

import kotlin.math.roundToInt

/** Parámetros de tiempo (config.json → "tiempos"). */
data class TimeConfig(
    /** Minutos que suma cada piso por escalera en una parada. */
    val stairMinutesPerFloor: Int,
    /** Minutos que suma usar ascensor en una parada (esperas, viajes). */
    val elevatorMinutes: Int,
    /** Qué parte del tiempo de carga lleva una parada intermedia (0.5 = la mitad). */
    val intermediateFactor: Double,
    /** Lo que se cobra por cada 15 minutos que exceden el tiempo incluido. */
    val waitFeePer15Min: Int,
)

/** Tiempo incluido en una parada. */
data class StopTime(val index: Int, val minutes: Int, val detail: String)

/** Tiempo de carga y descarga incluido en el precio, parada por parada. */
data class LoadTimeBreakdown(
    val stops: List<StopTime>,
    val totalMinutes: Int,
    val people: Int,
)

object LoadTime {
    /**
     * Cuántas veces más rápido se trabaja según la cantidad de personas (chofer + ayudantes).
     * No es lineal: dos personas no tardan exactamente la mitad.
     */
    fun peopleFactor(people: Int): Double = when {
        people <= 1 -> 1.0
        people == 2 -> 0.6
        else -> 0.45
    }

    /**
     * Tiempo máximo de carga/descarga incluido en el precio:
     *  - cada parada parte del tiempo de llenado del vehículo (origen y destino completo,
     *    paradas intermedias una fracción), dividido por el factor de personas;
     *  - la escalera suma minutos por piso y el ascensor un fijo, ambos por parada.
     */
    fun calculate(vehicle: VehicleSpec, helpers: Int, floors: List<FloorInfo>, config: TimeConfig): LoadTimeBreakdown {
        val people = 1 + helpers.coerceIn(0, 2)
        val factor = peopleFactor(people)
        val last = floors.lastIndex
        val stops = floors.mapIndexed { i, floor ->
            val isEnd = i == 0 || i == last
            val base = vehicle.loadMinutes * (if (isEnd) 1.0 else config.intermediateFactor) * factor
            val access = when (floor.access) {
                Access.GROUND -> 0
                Access.ELEVATOR -> if (floor.floors > 0) config.elevatorMinutes else 0
                Access.STAIRS -> floor.stairFloors * config.stairMinutesPerFloor
            }
            val minutes = roundUpTo5(base.roundToInt() + access)
            val detail = buildString {
                append(if (isEnd) "carga completa" else "parada intermedia")
                when (floor.access) {
                    Access.STAIRS -> if (floor.stairFloors > 0) append(" · ${floor.stairFloors} piso${if (floor.stairFloors > 1) "s" else ""} por escalera")
                    Access.ELEVATOR -> if (floor.floors > 0) append(" · ascensor")
                    Access.GROUND -> Unit
                }
            }
            StopTime(i, minutes, detail)
        }
        return LoadTimeBreakdown(stops, stops.sumOf { it.minutes }, people)
    }

    private fun roundUpTo5(m: Int): Int = ((m + 4) / 5) * 5

    /** "1 h 20 min" / "45 min" */
    fun format(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}

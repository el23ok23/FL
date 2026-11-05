package com.fletaba.core

/**
 * Precios editables por el dueño desde una planilla (Google Sheets publicada como CSV).
 * Formato: dos columnas "clave,valor"; se ignoran encabezados, líneas vacías y claves desconocidas.
 *
 *   por_ayudante,15000          kangoo.tarifa_base,25000
 *   por_ayudante_km,500         kangoo.por_km,1800
 *   por_piso_escalera,6000      furgon_mediano.tarifa_base,35000
 *   por_parada_extra,8000       ...
 *   km_minimo,2                 espera_por_15_min,6000
 *   comision_porcentaje,0       recargo_operativo_porcentaje,0
 */
object PriceSheet {
    data class Result(val pricing: PricingConfig, val vehicles: List<VehicleSpec>, val times: TimeConfig, val applied: Int)

    fun parse(csv: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        csv.lineSequence().forEach { raw ->
            val line = raw.trim().removePrefix("﻿")
            if (line.isEmpty()) return@forEach
            // Separador: ';' o tabulación si aparecen; si no, la primera coma (así "2,5" sigue siendo un valor).
            val idx = line.indexOfFirst { it == ';' || it == '\t' }.takeIf { it >= 0 } ?: line.indexOf(',')
            if (idx <= 0) return@forEach
            val key = line.substring(0, idx).trim().trim('"').lowercase().replace(' ', '_')
            val value = line.substring(idx + 1).trim().trimEnd(',', ';', '\t').trim().trim('"')
            if (key.isEmpty() || key == "clave" || value.isEmpty()) return@forEach
            out[key] = value
        }
        return out
    }

    /** Aplica la planilla sobre la configuración actual; lo que no esté en la planilla queda igual. */
    fun apply(csv: String, pricing: PricingConfig, vehicles: List<VehicleSpec>, times: TimeConfig): Result {
        val m = parse(csv)
        var applied = 0
        fun int(key: String, current: Int): Int = m[key]?.toNumber()?.let { applied++; it.toInt() } ?: current
        fun dbl(key: String, current: Double): Double = m[key]?.toNumber()?.let { applied++; it } ?: current
        val p = pricing.copy(
            perHelper = int("por_ayudante", pricing.perHelper),
            perHelperKm = int("por_ayudante_km", pricing.perHelperKm),
            perStairFloor = int("por_piso_escalera", pricing.perStairFloor),
            perExtraStop = int("por_parada_extra", pricing.perExtraStop),
            roundTo = int("redondeo", pricing.roundTo),
            urbanDistanceFactor = dbl("factor_distancia_urbana", pricing.urbanDistanceFactor),
            minKm = dbl("km_minimo", pricing.minKm),
            operatingPct = dbl("recargo_operativo_porcentaje", pricing.operatingPct * 100) / 100,
            commissionPct = dbl("comision_porcentaje", pricing.commissionPct * 100) / 100,
        )
        val t = times.copy(
            stairMinutesPerFloor = int("minutos_por_piso_escalera", times.stairMinutesPerFloor),
            elevatorMinutes = int("minutos_ascensor", times.elevatorMinutes),
            intermediateFactor = dbl("factor_parada_intermedia", times.intermediateFactor),
            waitFeePer15Min = int("espera_por_15_min", times.waitFeePer15Min),
        )
        val v = vehicles.map { veh ->
            val id = veh.kind.name.lowercase()
            veh.copy(
                baseFare = int("$id.tarifa_base", veh.baseFare),
                perKm = int("$id.por_km", veh.perKm),
                loadMinutes = int("$id.minutos_carga", veh.loadMinutes),
                volumeM3 = dbl("$id.m3", veh.volumeM3),
            )
        }
        return Result(p, v, t, applied)
    }

    /** Acepta "25000", "25.000", "25,5" y "$ 25.000". */
    private fun String.toNumber(): Double? {
        var s = trim().removePrefix("$").trim().replace(" ", "")
        if (s.isEmpty()) return null
        s = if (s.contains(',') && s.contains('.')) s.replace(".", "").replace(',', '.')
        else if (s.contains(',')) s.replace(',', '.')
        else if (Regex("""^\d{1,3}(\.\d{3})+$""").matches(s)) s.replace(".", "")
        else s
        return s.toDoubleOrNull()
    }

    /** Planilla de ejemplo con los valores actuales, para que el dueño la copie en Google Sheets. */
    fun template(pricing: PricingConfig, vehicles: List<VehicleSpec>, times: TimeConfig): String = buildString {
        appendLine("clave,valor")
        appendLine("por_ayudante,${pricing.perHelper}")
        appendLine("por_ayudante_km,${pricing.perHelperKm}")
        appendLine("por_piso_escalera,${pricing.perStairFloor}")
        appendLine("por_parada_extra,${pricing.perExtraStop}")
        appendLine("km_minimo,${pricing.minKm}")
        appendLine("redondeo,${pricing.roundTo}")
        appendLine("espera_por_15_min,${times.waitFeePer15Min}")
        appendLine("comision_porcentaje,${(pricing.commissionPct * 100).toInt()}")
        appendLine("recargo_operativo_porcentaje,${(pricing.operatingPct * 100).toInt()}")
        vehicles.forEach { v ->
            val id = v.kind.name.lowercase()
            appendLine("$id.tarifa_base,${v.baseFare}")
            appendLine("$id.por_km,${v.perKm}")
        }
    }
}

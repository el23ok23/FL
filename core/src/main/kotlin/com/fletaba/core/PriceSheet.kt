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
            weekendPct = dbl("recargo_fin_de_semana_porcentaje", pricing.weekendPct * 100) / 100,
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

    // ---- Formulario del panel del dueño -----------------------------------------------------

    /** Un valor editable: clave de la planilla, etiqueta para mostrar y si admite decimales (o es texto). */
    data class Field(val key: String, val label: String, val decimal: Boolean = false, val unit: String = "$", val text: Boolean = false)

    // ---- Marca (también editable por el dueño) ------------------------------------------------

    const val KEY_NAME = "nombre"
    const val KEY_WHATSAPP = "whatsapp"
    const val KEY_PRIMARY = "color_primario"
    const val KEY_ACCENT = "color_acento"
    const val KEY_LOGO = "logo_url"
    /** Claves cuyo valor es texto (no se validan como número). */
    val TEXT_KEYS = setOf(KEY_NAME, KEY_WHATSAPP, KEY_PRIMARY, KEY_ACCENT, KEY_LOGO)

    /** Lo que el dueño puede cambiar de su marca desde la app. null = sin cambio. */
    data class Branding(
        val name: String? = null,
        /** Números donde llegan los pedidos (549 + área + número, solo dígitos). Vacío = los del config. */
        val whatsapps: List<String> = emptyList(),
        /** "#RRGGBB" */
        val primary: String? = null,
        val accent: String? = null,
        val logoUrl: String? = null,
    )

    fun branding(csv: String): Branding = branding(parse(csv))

    fun branding(values: Map<String, String>): Branding = Branding(
        name = values[KEY_NAME]?.takeIf { it.isNotBlank() },
        whatsapps = splitPhones(values[KEY_WHATSAPP] ?: ""),
        primary = values[KEY_PRIMARY]?.let { hex(it) },
        accent = values[KEY_ACCENT]?.let { hex(it) },
        logoUrl = values[KEY_LOGO]?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("file:") || it == "local" },
    )

    /** "5491122334455, 549 2474 12-3456 / 54 9 11 2222 3333" → números normalizados (549…), sin repetir. */
    fun splitPhones(text: String): List<String> = text
        .split(',', ';', '/', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { raw -> ArgentinePhone.normalize(raw) ?: raw.filter { it.isDigit() }.takeIf { it.length in 10..15 } }
        .distinct()

    /** "#1b7be0", "1B7BE0" → "#1B7BE0"; otra cosa → null. */
    fun hex(text: String): String? {
        val h = text.trim().removePrefix("#").uppercase()
        return if (Regex("^[0-9A-F]{6}$").matches(h)) "#$h" else null
    }

    data class Group(val title: String, val fields: List<Field>, val subtitle: String? = null)

    /**
     * Grupos y campos que ve el dueño en el panel, en orden. [advanced] agrega los porcentajes
     * invisibles para el cliente (recargo operativo y comisión).
     */
    fun form(vehicles: List<VehicleSpec>, advanced: Boolean): List<Group> {
        val out = ArrayList<Group>()
        out += Group(
            "Tu negocio",
            listOf(
                Field(KEY_NAME, "Nombre que ve el cliente", text = true, unit = ""),
                Field(KEY_WHATSAPP, "WhatsApp donde llegan los pedidos (varios: separalos con coma)", text = true, unit = ""),
            ),
        )
        vehicles.forEach { v ->
            val id = v.kind.name.lowercase()
            out += Group(
                title = v.name,
                subtitle = v.dimensions,
                fields = listOf(Field("$id.tarifa_base", "Tarifa base"), Field("$id.por_km", "Por kilómetro")),
            )
        }
        out += Group("Ayudantes", listOf(Field("por_ayudante", "Por ayudante (base)"), Field("por_ayudante_km", "Por ayudante, por km")))
        out += Group(
            "Recargos",
            listOf(
                Field("por_piso_escalera", "Por piso por escalera"),
                Field("por_parada_extra", "Por parada en el medio"),
                Field("espera_por_15_min", "Espera, cada 15 minutos"),
                Field("recargo_fin_de_semana_porcentaje", "Sábados y domingos", decimal = true, unit = "%"),
            ),
        )
        out += Group(
            "General",
            listOf(Field("km_minimo", "Kilómetros mínimos", decimal = true, unit = "km"), Field("redondeo", "Redondear el total a")),
        )
        if (advanced) {
            out += Group(
                "Avanzado",
                subtitle = "Porcentajes que el cliente no ve, incluidos en el precio.",
                fields = listOf(
                    Field("recargo_operativo_porcentaje", "Recargo operativo", decimal = true, unit = "%"),
                    Field("comision_porcentaje", "Comisión", decimal = true, unit = "%"),
                ),
            )
        }
        return out
    }

    /** Valores actuales, con las claves de la planilla, para precargar el formulario. */
    fun currentValues(pricing: PricingConfig, vehicles: List<VehicleSpec>, times: TimeConfig): Map<String, String> =
        parse(template(pricing, vehicles, times))

    /** Valores actuales de marca con las claves de la planilla (vacío = el de fábrica). */
    fun brandingValues(b: Branding): Map<String, String> = linkedMapOf(
        KEY_NAME to (b.name ?: ""), KEY_WHATSAPP to b.whatsapps.joinToString(", "),
        KEY_PRIMARY to (b.primary ?: ""), KEY_ACCENT to (b.accent ?: ""), KEY_LOGO to (b.logoUrl ?: ""),
    )

    /** true si el texto es un número que la planilla entiende. */
    fun isNumber(text: String): Boolean = text.toNumber() != null

    /** true si el valor sirve para esa clave (número, o texto no vacío en las claves de texto). */
    fun isValid(key: String, value: String): Boolean = when (key) {
        KEY_NAME -> value.isBlank() || value.trim().length in 2..40
        KEY_WHATSAPP -> value.isBlank() || value.split(',', ';', '/', '\n').filter { it.isNotBlank() }.all { ArgentinePhone.normalize(it) != null || it.filter { c -> c.isDigit() }.length in 10..15 }
        KEY_PRIMARY, KEY_ACCENT -> value.isBlank() || hex(value) != null
        KEY_LOGO -> true
        else -> isNumber(value)
    }

    /** Arma el CSV "clave,valor" a partir de lo que escribió el dueño (solo claves con valor válido). */
    fun toCsv(values: Map<String, String>): String = buildString {
        appendLine("clave,valor")
        values.forEach { (k, v) ->
            if (k.isBlank()) return@forEach
            if (k in TEXT_KEYS) {
                val t = v.trim().replace('\n', ' ').replace('\r', ' ')
                if (t.isNotEmpty()) appendLine("$k,$t")
            } else if (v.toNumber() != null) {
                appendLine("$k,${v.trim().replace(',', '.')}")
            }
        }
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
        appendLine("km_minimo,${num(pricing.minKm)}")
        appendLine("redondeo,${pricing.roundTo}")
        appendLine("espera_por_15_min,${times.waitFeePer15Min}")
        appendLine("comision_porcentaje,${num(pricing.commissionPct * 100)}")
        appendLine("recargo_operativo_porcentaje,${num(pricing.operatingPct * 100)}")
        appendLine("recargo_fin_de_semana_porcentaje,${num(pricing.weekendPct * 100)}")
        vehicles.forEach { v ->
            val id = v.kind.name.lowercase()
            appendLine("$id.tarifa_base,${v.baseFare}")
            appendLine("$id.por_km,${v.perKm}")
        }
    }

    /** 2.0 → "2", 2.5 → "2.5", 15.000001 → "15". */
    private fun num(d: Double): String {
        val r = Math.round(d * 100) / 100.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString()
    }
}

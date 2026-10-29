package com.fletarg.core

/** Dirección elegida por el cliente. */
data class AddressPoint(
    val label: String,
    val location: LatLng,
    /** Dentro de la Argentina. */
    val insideCoverage: Boolean,
    /** Dentro de Capital Federal (obligatorio para el origen). */
    val insideCaba: Boolean = false,
)

/** Una parada del recorrido: dirección más acceso (piso / ascensor / escalera). */
data class Stop(val point: AddressPoint, val floor: FloorInfo)

/** Resumen final del pedido, listo para enviar. */
data class OrderSummary(
    val businessName: String,
    val customerName: String,
    val customerPhone: String, // normalizado 549...
    val stops: List<Stop>,     // origen, paradas intermedias, destino
    val vehicle: VehicleSpec,
    val helpers: Int,
    val cargo: String,
    val schedule: Schedule,
    val distanceKm: Double,
    val price: PriceBreakdown,
    val loadTime: LoadTimeBreakdown? = null,
    val waitFeePer15Min: Int = 0,
    val travelMinutes: Int? = null,
) {
    companion object {
        const val MAX_STOPS = 6 // origen + hasta 5 paradas
    }
}

object OrderMessage {
    /** Nombre de cada parada según su posición: Origen, Parada 1…, Destino. */
    fun stopTitle(index: Int, count: Int): String = when {
        index == 0 -> "Origen"
        index == count - 1 -> "Destino"
        else -> "Parada $index"
    }

    /**
     * Texto del pedido. Con [ownerNotes] agrega al final la parte privada (comisión y neto):
     * solo para canales que no pasan por el teléfono del cliente (webhook, CallMeBot).
     */
    fun build(o: OrderSummary, today: DayDate = DayDate.today(), ownerNotes: Boolean = false): String = buildString {
        appendLine("🚚 *Nuevo pedido de flete* · ${o.businessName}")
        appendLine()
        appendLine("👤 Nombre: ${o.customerName.trim()}")
        appendLine("📱 WhatsApp: ${ArgentinePhone.format(o.customerPhone)}")
        appendLine()
        o.stops.forEachIndexed { i, s ->
            val icon = when {
                i == 0 -> "📍"
                i == o.stops.size - 1 -> "🏁"
                else -> "🔸"
            }
            appendLine("$icon ${stopTitle(i, o.stops.size)}: ${s.point.label}")
            appendLine("   ${s.floor.describe()}")
        }
        appendLine()
        appendLine("🚐 Vehículo: ${o.vehicle.name}")
        appendLine("🧑‍🔧 Ayudantes: ${o.helpers}")
        appendLine("📦 Qué lleva: ${o.cargo.trim().ifBlank { "(no indicado)" }}")
        appendLine("📅 Cuándo: ${o.schedule.describe(today)}")
        appendLine("📏 Distancia aprox.: ${Money.formatKm(o.distanceKm)}")
        o.travelMinutes?.let { appendLine("🚗 Viaje aprox.: ${LoadTime.format(it)}") }
        o.loadTime?.let { lt ->
            appendLine("⏱ Carga y descarga incluidas: hasta ${LoadTime.format(lt.totalMinutes)} (${lt.people} persona${if (lt.people > 1) "s" else ""})")
            lt.stops.forEach { st ->
                appendLine("   ${stopTitle(st.index, lt.stops.size)}: ${LoadTime.format(st.minutes)} · ${st.detail}")
            }
            if (o.waitFeePer15Min > 0) appendLine("   Espera extra: ${Money.formatArs(o.waitFeePer15Min)} cada 15 min")
        }
        appendLine()
        append("💰 Precio aproximado: ${Money.formatArs(o.price.total)}")
        if (ownerNotes && o.price.commission > 0) {
            appendLine()
            appendLine()
            appendLine("🔒 Solo para vos")
            appendLine("Comisión: ${Money.formatArs(o.price.commission)}")
            appendLine("Neto para el fletero: ${Money.formatArs(o.price.netForDriver)}")
            append("Costo base sin recargos: ${Money.formatArs(o.price.subtotal)}")
        }
    }
}

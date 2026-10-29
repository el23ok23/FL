package com.fletarg.core

/** Dirección elegida por el cliente. */
data class AddressPoint(
    val label: String,
    val location: LatLng,
    val insideCoverage: Boolean,
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

    /** Texto del pedido tal como llega al WhatsApp del dueño. */
    fun build(o: OrderSummary, today: DayDate = DayDate.today()): String = buildString {
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
        appendLine()
        append("💰 Precio aproximado: ${Money.formatArs(o.price.total)}")
    }
}

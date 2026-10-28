package com.mifleteciudad.core

/** Dirección elegida por el cliente. */
data class AddressPoint(
    val label: String,
    val location: LatLng,
    val insideCaba: Boolean,
)

/** Resumen final del pedido, listo para enviar. */
data class OrderSummary(
    val businessName: String,
    val customerName: String,
    val customerPhone: String, // normalizado 549...
    val origin: AddressPoint,
    val destination: AddressPoint,
    val vehicle: VehicleSpec,
    val helpers: Int,
    val originFloor: FloorInfo,
    val destinationFloor: FloorInfo,
    val cargo: String,
    val distanceKm: Double,
    val price: PriceBreakdown,
)

object OrderMessage {
    /** Texto del pedido tal como llega al WhatsApp del dueño. */
    fun build(o: OrderSummary): String = buildString {
        appendLine("🚚 *Nuevo pedido de flete* · ${o.businessName}")
        appendLine()
        appendLine("👤 Nombre: ${o.customerName.trim()}")
        appendLine("📱 WhatsApp: ${ArgentinePhone.format(o.customerPhone)}")
        appendLine()
        appendLine("📍 Origen: ${o.origin.label}")
        appendLine("   ${o.originFloor.describe()}")
        appendLine("🏁 Destino: ${o.destination.label}")
        appendLine("   ${o.destinationFloor.describe()}")
        appendLine()
        appendLine("🚐 Vehículo: ${o.vehicle.name}")
        appendLine("🧑‍🔧 Ayudantes: ${o.helpers}")
        appendLine("📦 Qué lleva: ${o.cargo.trim().ifBlank { "(no indicado)" }}")
        appendLine("📏 Distancia aprox.: ${Money.formatKm(o.distanceKm)}")
        appendLine()
        append("💰 Precio aproximado: ${Money.formatArs(o.price.total)}")
    }
}

package com.fletaba.core

/** Un tipo de cosa para llevar, con su volumen aproximado. */
data class CargoItem(val id: String, val name: String, val m3: Double, val emoji: String)

object CargoItems {
    val all: List<CargoItem> = listOf(
        CargoItem("cajas", "Cajas", 0.08, "📦"),
        CargoItem("bolsas", "Bolsas", 0.05, "🛍️"),
        CargoItem("heladera", "Heladera", 1.0, "🧊"),
        CargoItem("lavarropas", "Lavarropas", 0.4, "🫧"),
        CargoItem("cocina", "Cocina", 0.4, "🍳"),
        CargoItem("sommier", "Sommier", 0.9, "🛏️"),
        CargoItem("colchon", "Colchón", 0.5, "🛌"),
        CargoItem("ropero", "Ropero", 1.5, "🚪"),
        CargoItem("mesa", "Mesa", 0.6, "🪑"),
        CargoItem("sillas", "Sillas", 0.15, "💺"),
        CargoItem("sofa", "Sofá", 1.2, "🛋️"),
        CargoItem("escritorio", "Escritorio", 0.6, "🖥️"),
        CargoItem("tv", "TV", 0.15, "📺"),
        CargoItem("bici", "Bicicleta", 0.4, "🚲"),
        CargoItem("moto", "Moto", 1.2, "🏍️"),
        CargoItem("electro", "Electro chico", 0.1, "🔌"),
    )

    fun byId(id: String): CargoItem? = all.firstOrNull { it.id == id }

    /** Volumen total estimado de lo elegido (m³). */
    fun volume(counts: Map<String, Int>): Double =
        counts.entries.sumOf { (id, n) -> (byId(id)?.m3 ?: 0.0) * n }

    /** "Heladera ×1, Cajas ×10" */
    fun describe(counts: Map<String, Int>): String =
        all.filter { (counts[it.id] ?: 0) > 0 }.joinToString(", ") { "${it.name} ×${counts[it.id]}" }

    /** El vehículo más chico donde entra el volumen (o null si no entra en ninguno). */
    fun smallestThatFits(volumeM3: Double, vehicles: List<VehicleSpec>): VehicleSpec? =
        vehicles.sortedBy { it.volumeM3 }.firstOrNull { it.volumeM3 >= volumeM3 * 1.15 }

    /** "4,2 m³" */
    fun formatM3(v: Double): String {
        val tenths = Math.round(v * 10)
        return "${tenths / 10},${tenths % 10} m³"
    }
}

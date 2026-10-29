package com.fletarg.core

import java.util.Calendar
import java.util.TimeZone

/** Franja horaria del flete. */
enum class TimeSlot(val title: String, val hours: String) {
    MORNING("Mañana", "8 a 12 h"),
    AFTERNOON("Tarde", "13 a 18 h"),
    ANY("A coordinar", "cualquier horario");

    fun describe(): String = when (this) {
        MORNING -> "por la mañana (8 a 12 h)"
        AFTERNOON -> "por la tarde (13 a 18 h)"
        ANY -> "horario a coordinar"
    }
}

/** Una fecha de calendario (sin hora). month es 1..12. */
data class DayDate(val year: Int, val month: Int, val day: Int) {
    /** 0 = lunes … 6 = domingo (congruencia de Zeller). */
    val dayOfWeek: Int
        get() {
            var y = year
            var m = month
            if (m < 3) { m += 12; y -= 1 }
            val k = y % 100
            val j = y / 100
            val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7 // 0 = sábado
            return (h + 5) % 7
        }

    val dayName: String get() = DAY_NAMES[dayOfWeek]
    val monthName: String get() = MONTH_NAMES[month - 1]

    /** "mié 30/10" */
    fun short(): String = "$dayName ${day}/${month.toString().padStart(2, '0')}"

    /** "miércoles 30 de octubre" */
    fun long(): String = "${DAY_NAMES_LONG[dayOfWeek]} $day de $monthName"

    fun plusDays(n: Int): DayDate {
        val c = Calendar.getInstance(ARGENTINA)
        c.clear()
        c.set(year, month - 1, day)
        c.add(Calendar.DAY_OF_MONTH, n)
        return DayDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    companion object {
        val ARGENTINA: TimeZone = TimeZone.getTimeZone("America/Argentina/Buenos_Aires")
        val DAY_NAMES = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
        val DAY_NAMES_LONG = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
        val MONTH_NAMES = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        )

        fun today(): DayDate {
            val c = Calendar.getInstance(ARGENTINA)
            return DayDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }

        /** Los próximos [count] días a partir de hoy, inclusive. */
        fun upcoming(count: Int, from: DayDate = today()): List<DayDate> = (0 until count).map { from.plusDays(it) }
    }
}

/** Cuándo quiere el flete el cliente. date null = lo antes posible. */
data class Schedule(val date: DayDate? = null, val slot: TimeSlot = TimeSlot.ANY) {
    fun describe(today: DayDate = DayDate.today()): String {
        val when_ = when (date) {
            null -> "lo antes posible"
            today -> "hoy (${date.short()})"
            today.plusDays(1) -> "mañana (${date.short()})"
            else -> date.long()
        }
        return "$when_, ${slot.describe()}"
    }
}

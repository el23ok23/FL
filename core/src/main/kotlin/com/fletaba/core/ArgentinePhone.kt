package com.fletaba.core

/**
 * Validación y normalización de celulares argentinos.
 *
 * Acepta formatos como "11 2345 6789", "011 15 2345 6789", "+54 9 11 2345-6789",
 * "351 15 123 4567" y devuelve siempre el formato internacional sin "+": 549 + área + número
 * (10 dígitos nacionales), que es el que usa WhatsApp.
 */
object ArgentinePhone {
    // Códigos de área de 3 dígitos. El 11 tiene 2 y el resto 4.
    private val THREE_DIGIT_AREAS = setOf(
        "220", "221", "223", "230", "236", "237", "249", "260", "261", "263", "264", "266",
        "280", "291", "294", "297", "298", "299", "336", "341", "342", "343", "345", "348",
        "351", "353", "358", "362", "364", "370", "376", "379", "380", "381", "383", "385",
        "387", "388",
    )

    /** Devuelve "549XXXXXXXXXX" o null si no es un celular argentino válido. */
    fun normalize(input: String): String? {
        var d = input.filter { it.isDigit() }
        if (d.isEmpty()) return null
        if (d.startsWith("00")) d = d.drop(2)
        if (d.startsWith("54") && d.length > 10) d = d.drop(2)
        if (d.startsWith("9") && d.length == 11) d = d.drop(1)
        if (d.startsWith("0")) d = d.drop(1)
        // "15 2345 6789" sin código de área: es un celular de Buenos Aires.
        if (d.length == 10 && d.startsWith("15")) d = "11" + d.drop(2)
        if (d.length == 12) {
            // Formato local con "15": área + 15 + número.
            val areaLen = areaLength(d)
            if (d.length >= areaLen + 2 && d.substring(areaLen, areaLen + 2) == "15") {
                d = d.substring(0, areaLen) + d.substring(areaLen + 2)
            }
        }
        if (d.length != 10) return null
        if (d[0] !in '1'..'3') return null
        val areaLen = areaLength(d)
        val local = d.substring(areaLen)
        if (local.length < 6) return null
        if (local.all { it == local[0] }) return null
        return "549$d"
    }

    fun isValid(input: String): Boolean = normalize(input) != null

    /** "+54 9 11 2345-6789" para mostrar. */
    fun format(normalized: String): String {
        val n = normalized.removePrefix("549")
        val areaLen = areaLength(n)
        val area = n.take(areaLen)
        val local = n.drop(areaLen)
        val split = local.length - 4
        return "+54 9 $area ${local.take(split)}-${local.drop(split)}"
    }

    private fun areaLength(national: String): Int = when {
        national.startsWith("11") -> 2
        national.length >= 3 && national.substring(0, 3) in THREE_DIGIT_AREAS -> 3
        else -> 4
    }
}

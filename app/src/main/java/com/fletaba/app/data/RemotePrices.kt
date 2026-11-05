package com.fletaba.app.data

import android.content.Context
import android.util.Log

/**
 * Precios que el dueño edita en su planilla (Google Sheets publicada como CSV).
 * Se descargan al abrir la app; la última versión buena queda guardada en el teléfono
 * para usarla sin internet. Si nunca se pudo descargar, valen los precios del config.json.
 */
class RemotePrices(context: Context, private val url: String) {
    private val prefs = context.applicationContext.getSharedPreferences("fletaba_precios", Context.MODE_PRIVATE)

    /** CSV guardado de la última descarga exitosa (o null). */
    fun cached(): String? = prefs.getString(KEY_CSV + url.hashCode(), null)

    /** Descarga la planilla; devuelve el CSV o null si falló. Guarda la copia local. */
    suspend fun fetch(): String? {
        // cache-buster para que Google no sirva una versión vieja
        val sep = if ('?' in url) "&" else "?"
        val csv = Http.request("$url${sep}_t=${System.currentTimeMillis() / 60000}") ?: return null
        if (!looksLikeSheet(csv)) {
            Log.w(TAG, "La planilla no tiene el formato clave,valor")
            return null
        }
        prefs.edit().putString(KEY_CSV + url.hashCode(), csv).apply()
        return csv
    }

    private fun looksLikeSheet(csv: String): Boolean =
        !csv.trimStart().startsWith("<") && csv.lineSequence().any { it.contains(',') || it.contains(';') }

    private companion object {
        const val TAG = "RemotePrices"
        const val KEY_CSV = "csv_"
    }
}

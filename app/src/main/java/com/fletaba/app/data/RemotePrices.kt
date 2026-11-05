package com.fletaba.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Precios del dueño. Viven en su planilla de Google (un Apps Script gratuito la lee y la escribe):
 *  - GET  url → CSV "clave,valor" que descargan todos los teléfonos al abrir la app.
 *  - POST url {pin, csv} → guarda los precios nuevos (solo con el PIN del dueño).
 * La última versión buena queda guardada en el teléfono para usarla sin internet.
 * Sin url, el panel funciona en modo local: cambia los precios solo en este teléfono.
 */
class RemotePrices(private val context: Context, private val url: String?, configPin: String?, private val configRecoveryEmail: String?) {
    private val prefs = context.applicationContext.getSharedPreferences("fletaba_precios", Context.MODE_PRIVATE)
    private val key = KEY_CSV + (url ?: "local").hashCode()

    /** true si los cambios llegan a todos los teléfonos (hay planilla configurada). */
    val synced: Boolean get() = url != null
    /** PIN en modo local: el que cambió el dueño en este teléfono o, si no, el del config.json. */
    private val localPin: String? get() = prefs.getString(KEY_LOCAL_PIN, null) ?: configPinValue
    private val configPinValue: String? = configPin?.takeIf { it.isNotBlank() }

    /** true si el panel del dueño puede usarse (planilla o PIN local). */
    val available: Boolean get() = url != null || !localPin.isNullOrBlank()

    enum class Status { OK, WRONG_PIN, OFFLINE, NOT_CONFIGURED, INVALID, NO_EMAIL, LIMIT }

    /** Correo de recuperación conocido en este teléfono (enmascarado si vino del servidor). */
    fun recoveryEmail(): String? = prefs.getString(KEY_EMAIL, null) ?: configRecoveryEmail
    private fun rememberEmail(masked: String?) { prefs.edit().apply { if (masked.isNullOrBlank()) remove(KEY_EMAIL) else putString(KEY_EMAIL, masked) }.apply() }

    /** Cambia el PIN (en la planilla, o en este teléfono si no hay planilla). */
    suspend fun changePin(pin: String, newPin: String): Status {
        if (newPin.length !in 4..32) return Status.INVALID
        if (url == null) {
            if (pin != localPin) return Status.WRONG_PIN
            prefs.edit().putString(KEY_LOCAL_PIN, newPin).apply(); return Status.OK
        }
        return post(JSONObject().put("pin", pin).put("accion", "cambiar_pin").put("nuevo_pin", newPin))
    }

    /** Guarda el correo de recuperación. */
    suspend fun saveRecoveryEmail(pin: String, email: String): Status {
        val e = email.trim()
        if (e.isNotEmpty() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(e)) return Status.INVALID
        if (url == null) {
            if (pin != localPin) return Status.WRONG_PIN
            rememberEmail(e); return Status.OK
        }
        val json = postJson(JSONObject().put("pin", pin).put("accion", "correo").put("correo", e)) ?: return Status.OFFLINE
        return when {
            json.optBoolean("ok") -> { rememberEmail(e); Status.OK }
            json.optString("error") == "pin" -> Status.WRONG_PIN
            json.optString("error") == "correo_invalido" -> Status.INVALID
            else -> Status.OFFLINE
        }
    }

    /**
     * Recuperar el PIN. Con planilla, el servidor lo manda al correo. Sin planilla, devuelve el PIN
     * para que la app lo ponga en un correo que el dueño se manda a sí mismo (modo de prueba).
     */
    suspend fun recover(): Pair<Status, String?> {
        if (url == null) {
            val mail = recoveryEmail() ?: return Status.NO_EMAIL to null
            return Status.OK to "$mail|${localPin.orEmpty()}"
        }
        val json = postJson(JSONObject().put("accion", "recuperar")) ?: return Status.OFFLINE to null
        return when {
            json.optBoolean("ok") -> Status.OK to json.optString("correo")
            json.optString("error") == "sin_correo" -> Status.NO_EMAIL to null
            json.optString("error") == "limite" -> Status.LIMIT to null
            else -> Status.OFFLINE to null
        }
    }

    /** CSV guardado de la última descarga o del último guardado (o null). */
    fun cached(): String? = prefs.getString(key, null)

    fun saveLocal(csv: String) { prefs.edit().putString(key, csv).apply() }
    fun clearLocal() { prefs.edit().remove(key).apply() }

    /** PIN recordado en este teléfono tras entrar bien una vez. */
    fun rememberedPin(): String? = prefs.getString(KEY_PIN, null)
    fun rememberPin(pin: String?) { prefs.edit().apply { if (pin == null) remove(KEY_PIN) else putString(KEY_PIN, pin) }.apply() }

    /** Descarga la planilla; devuelve el CSV o null si falló. Guarda la copia local. */
    suspend fun fetch(): String? {
        val u = url ?: return null
        val sep = if ('?' in u) "&" else "?"
        val csv = Http.request("$u${sep}_t=${System.currentTimeMillis() / 60000}") ?: return null
        if (!looksLikeSheet(csv)) {
            Log.w(TAG, "La planilla no tiene el formato clave,valor")
            return null
        }
        saveLocal(csv)
        return csv
    }

    /** Comprueba el PIN (en la planilla, o contra el PIN local si no hay planilla). */
    suspend fun verifyPin(pin: String): Status {
        if (url == null) return if (!localPin.isNullOrBlank() && pin == localPin) Status.OK else if (localPin.isNullOrBlank()) Status.NOT_CONFIGURED else Status.WRONG_PIN
        val json = postJson(JSONObject().put("pin", pin).put("accion", "verificar")) ?: return Status.OFFLINE
        return when {
            json.optBoolean("ok") -> { json.optString("correo").takeIf { it.isNotBlank() }?.let { rememberEmail(it) }; Status.OK }
            json.optString("error") == "pin" -> Status.WRONG_PIN
            else -> Status.OFFLINE
        }
    }

    /** Guarda los precios nuevos: en la planilla (si hay) y siempre en este teléfono. */
    suspend fun publish(csv: String, pin: String): Status {
        if (url == null) {
            if (localPin.isNullOrBlank()) return Status.NOT_CONFIGURED
            if (pin != localPin) return Status.WRONG_PIN
            saveLocal(csv); return Status.OK
        }
        val st = post(JSONObject().put("pin", pin).put("accion", "guardar").put("csv", csv))
        if (st == Status.OK) saveLocal(csv)
        return st
    }

    /**
     * Sube el logo (PNG) y devuelve su dirección pública, o null si falló.
     * Con planilla: el Apps Script lo guarda en el Drive del dueño. Sin planilla: queda en este teléfono.
     */
    suspend fun uploadLogo(png: ByteArray, pin: String): String? {
        if (url == null) {
            if (pin != localPin) return null
            return withContext(Dispatchers.IO) {
                runCatching { File(filesDir, LOGO_FILE).writeBytes(png); "local" }.getOrNull()
            }
        }
        val b64 = android.util.Base64.encodeToString(png, android.util.Base64.NO_WRAP)
        val json = postJson(JSONObject().put("pin", pin).put("accion", "logo").put("imagen", b64).put("tipo", "image/png")) ?: return null
        return json.optString("url").takeIf { it.startsWith("https://") }
    }

    /** Archivo del logo guardado en este teléfono (modo local). */
    fun localLogoFile(): File = File(filesDir, LOGO_FILE)

    private val filesDir: File = context.applicationContext.filesDir

    /** POST al Apps Script. Google responde con una redirección: la seguimos a mano con un GET. */
    private suspend fun post(body: JSONObject): Status {
        val json = postJson(body) ?: return Status.OFFLINE
        return when {
            json.optBoolean("ok") -> Status.OK
            json.optString("error") == "pin" -> Status.WRONG_PIN
            else -> Status.OFFLINE
        }
    }

    private suspend fun postJson(body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        val u = url ?: return@withContext null
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(u).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = 15_000; readTimeout = 15_000
                doOutput = true
                // text/plain evita la comprobación previa de CORS y Apps Script lo lee igual.
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            var code = conn.responseCode
            var text: String? = null
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: return@withContext null
                conn.disconnect()
                conn = (URL(location).openConnection() as HttpURLConnection).apply { connectTimeout = 15_000; readTimeout = 15_000 }
                code = conn.responseCode
            }
            text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (code !in 200..299 || text == null) {
                Log.w(TAG, "POST -> $code ${text?.take(200)}"); return@withContext null
            }
            runCatching { JSONObject(text) }.getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "POST falló: ${e.message}"); null
        } finally {
            conn?.disconnect()
        }
    }

    private fun looksLikeSheet(csv: String): Boolean =
        !csv.trimStart().startsWith("<") && csv.lineSequence().any { it.contains(',') || it.contains(';') }

    private companion object {
        const val TAG = "RemotePrices"
        const val KEY_CSV = "csv_"
        const val KEY_PIN = "pin"
        const val KEY_LOCAL_PIN = "pin_local"
        const val KEY_EMAIL = "correo_recuperacion"
        const val LOGO_FILE = "logo.png"
    }
}

package com.fletaba.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Cliente HTTP mínimo sobre HttpURLConnection: sin librerías extra, APK más chico. */
object Http {
    private const val TAG = "Http"
    private const val TIMEOUT_MS = 6_000

    /**
     * POST a un Apps Script de Google. Google responde con una redirección a otro dominio: la seguimos
     * a mano con un GET, que es donde viene la respuesta. Devuelve el cuerpo o null si falló.
     */
    suspend fun postScript(url: String, body: String, timeoutMs: Int = 20_000): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = timeoutMs; readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            var code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: return@withContext null
                conn.disconnect()
                conn = (URL(location).openConnection() as HttpURLConnection).apply { connectTimeout = timeoutMs; readTimeout = timeoutMs }
                code = conn.responseCode
            }
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (code in 200..299) text else { Log.w(TAG, "POST script -> $code ${text?.take(200)}"); null }
        } catch (e: Exception) {
            Log.w(TAG, "POST script falló: ${e.message}"); null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun request(
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json, text/plain, */*")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (code in 200..299) text else {
                Log.w(TAG, "$method $url -> $code ${text?.take(300)}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "$method $url falló: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }
}

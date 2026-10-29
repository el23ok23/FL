package com.fletarg.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Cliente HTTP mínimo sobre HttpURLConnection: sin librerías extra, APK más chico. */
object Http {
    private const val TAG = "Http"
    private const val TIMEOUT_MS = 6_000

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

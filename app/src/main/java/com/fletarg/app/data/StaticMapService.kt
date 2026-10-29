package com.fletarg.app.data

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.fletarg.core.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Mapa real con Google Maps Static API: una sola imagen por recorrido, sin SDK de mapas
 * (la app sigue pesando lo mismo). Cache en memoria por URL.
 */
class StaticMapService(private val apiKey: String) {
    private val cache = LinkedHashMap<String, ImageBitmap>()

    suspend fun load(
        points: List<LatLng>,
        encodedPolyline: String?,
        widthPx: Int,
        heightPx: Int,
        primaryHex: String,
        destinationHex: String,
        dark: Boolean,
    ): ImageBitmap? {
        if (points.isEmpty()) return null
        val url = buildUrl(points, encodedPolyline, widthPx, heightPx, primaryHex, destinationHex, dark)
        cache[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                }
                if (conn.responseCode !in 200..299) {
                    Log.w(TAG, "Static map -> ${conn.responseCode}")
                    return@withContext null
                }
                val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                val img = bmp.asImageBitmap()
                synchronized(cache) {
                    if (cache.size >= 8) cache.remove(cache.keys.first())
                    cache[url] = img
                }
                img
            } catch (e: Exception) {
                Log.w(TAG, "Static map falló: ${e.message}")
                null
            } finally {
                conn?.disconnect()
            }
        }
    }

    private fun buildUrl(
        points: List<LatLng>, encoded: String?, w: Int, h: Int,
        primaryHex: String, destinationHex: String, dark: Boolean,
    ): String {
        // Static Maps acepta hasta 640×640 por lado con scale=2.
        val width = w.coerceIn(100, 640)
        val height = h.coerceIn(100, 640)
        val sb = StringBuilder("https://maps.googleapis.com/maps/api/staticmap?")
        sb.append("size=${width}x$height&scale=2&language=es&region=AR&maptype=roadmap")
        points.forEachIndexed { i, p ->
            val color = if (i == points.lastIndex && points.size > 1) destinationHex else primaryHex
            val label = ('A' + i)
            sb.append("&markers=").append(enc("color:0x$color|label:$label|${fmt(p.lat)},${fmt(p.lng)}"))
        }
        if (points.size >= 2) {
            val path = if (!encoded.isNullOrEmpty()) "color:0x${primaryHex}ff|weight:5|enc:$encoded"
            else "color:0x${primaryHex}ff|weight:4|" + points.joinToString("|") { "${fmt(it.lat)},${fmt(it.lng)}" }
            sb.append("&path=").append(enc(path))
        }
        // Estilo limpio: sin puntos de interés ni íconos de transporte.
        sb.append("&style=").append(enc("feature:poi|visibility:off"))
        sb.append("&style=").append(enc("feature:transit|visibility:off"))
        if (dark) {
            sb.append("&style=").append(enc("element:geometry|color:0x1c2435"))
            sb.append("&style=").append(enc("element:labels.text.fill|color:0x9aa7bd"))
            sb.append("&style=").append(enc("element:labels.text.stroke|color:0x0b0f1a"))
            sb.append("&style=").append(enc("feature:road|element:geometry|color:0x2b3650"))
            sb.append("&style=").append(enc("feature:water|element:geometry|color:0x0f2238"))
        }
        sb.append("&key=").append(enc(apiKey))
        return sb.toString()
    }

    private fun fmt(d: Double) = String.format(java.util.Locale.US, "%.5f", d)
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val TAG = "StaticMap"
    }
}

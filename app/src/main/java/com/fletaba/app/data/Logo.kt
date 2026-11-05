package com.fletaba.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/** Carga y prepara el logo del dueño (sin librerías de imágenes). */
object Logo {
    private const val TAG = "Logo"
    private const val MAX_SIDE = 512

    /**
     * Logo para mostrar: "local" o "file:" → archivo del teléfono; https → se descarga una vez y
     * queda guardado en caché (clave por dirección) para abrir la app sin internet.
     */
    suspend fun load(context: Context, source: String?, localFile: File): Bitmap? = withContext(Dispatchers.IO) {
        if (source.isNullOrBlank()) return@withContext null
        try {
            when {
                source == "local" -> localFile.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                source.startsWith("file:") -> File(Uri.parse(source).path ?: return@withContext null).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                source.startsWith("https://") -> {
                    val cache = File(context.cacheDir, "logo_${source.hashCode()}.png")
                    if (!cache.exists()) {
                        val bytes = download(source) ?: return@withContext null
                        cache.writeBytes(bytes)
                    }
                    BitmapFactory.decodeFile(cache.path) ?: run { cache.delete(); null }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo cargar el logo: ${e.message}"); null
        }
    }

    /** Lee la imagen elegida en la galería, la achica a 512 px y la devuelve como PNG. */
    suspend fun prepare(context: Context, uri: Uri): Pair<Bitmap, ByteArray>? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return@withContext null
            val scale = MAX_SIDE.toFloat() / max(raw.width, raw.height)
            val bmp = if (scale < 1f) Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt().coerceAtLeast(1), (raw.height * scale).toInt().coerceAtLeast(1), true) else raw
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            bmp to out.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la imagen: ${e.message}"); null
        }
    }

    private fun download(url: String): ByteArray? {
        var u = url
        repeat(4) {
            val conn = (URL(u).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 10_000; instanceFollowRedirects = false }
            try {
                val code = conn.responseCode
                if (code in 300..399) { u = conn.getHeaderField("Location") ?: return null; return@repeat }
                if (code !in 200..299) return null
                val type = conn.contentType ?: ""
                if (type.startsWith("text/html")) return null
                return conn.inputStream.use { it.readBytes() }
            } finally { conn.disconnect() }
        }
        return null
    }
}

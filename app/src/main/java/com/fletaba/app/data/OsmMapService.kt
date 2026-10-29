package com.fletaba.app.data

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.fletaba.core.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/** Mapa de OpenStreetMap ya descargado, con los datos para proyectar puntos sobre él. */
data class OsmMap(
    val image: ImageBitmap,
    val centerLat: Double,
    val centerLng: Double,
    val zoom: Int,
    val width: Int,
    val height: Int,
) {
    /** Píxel (en la imagen) de una coordenada. */
    fun toPixel(p: LatLng): Pair<Float, Float> {
        val (cx, cy) = OsmMapService.mercator(centerLat, centerLng, zoom)
        val (x, y) = OsmMapService.mercator(p.lat, p.lng, zoom)
        return Pair((width / 2.0 + (x - cx)).toFloat(), (height / 2.0 + (y - cy)).toFloat())
    }
}

/**
 * Mapa real sin clave: una imagen de OpenStreetMap (staticmap.openstreetmap.de) centrada y con el
 * zoom justo para que entren todas las paradas. El recorrido y las letras se dibujan encima.
 * Uso liviano (una imagen por recorrido) y con atribución, como pide OSM.
 */
class OsmMapService {
    private val cache = LinkedHashMap<String, OsmMap>()

    suspend fun load(points: List<LatLng>, width: Int, height: Int): OsmMap? {
        if (points.isEmpty()) return null
        val w = width.coerceIn(100, 1024)
        val h = height.coerceIn(100, 1024)
        val (zoom, centerLat, centerLng) = fit(points, w, h)
        val url = "https://staticmap.openstreetmap.de/staticmap.php?center=" +
            String.format(java.util.Locale.US, "%.5f,%.5f", centerLat, centerLng) +
            "&zoom=$zoom&size=${w}x$h&maptype=mapnik"
        cache[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "Fletaba/1.0 (app de fletes; contacto en la ficha de Google Play)")
                }
                if (conn.responseCode !in 200..299) {
                    Log.w(TAG, "OSM -> ${conn.responseCode}")
                    return@withContext null
                }
                val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                val map = OsmMap(bmp.asImageBitmap(), centerLat, centerLng, zoom, bmp.width, bmp.height)
                synchronized(cache) {
                    if (cache.size >= 8) cache.remove(cache.keys.first())
                    cache[url] = map
                }
                map
            } catch (e: Exception) {
                Log.w(TAG, "OSM falló: ${e.message}")
                null
            } finally {
                conn?.disconnect()
            }
        }
    }

    /** Zoom máximo (hasta 17) con el que todas las paradas entran con margen, y el centro. */
    private fun fit(points: List<LatLng>, w: Int, h: Int): Triple<Int, Double, Double> {
        val pad = 56.0
        for (z in 17 downTo 2) {
            val xs = points.map { mercator(it.lat, it.lng, z).first }
            val ys = points.map { mercator(it.lat, it.lng, z).second }
            val spanX = xs.max() - xs.min()
            val spanY = ys.max() - ys.min()
            if (spanX <= w - 2 * pad && spanY <= h - 2 * pad - 30) {
                val cx = (xs.max() + xs.min()) / 2
                val cy = (ys.max() + ys.min()) / 2 + 10 // deja lugar para la etiqueta de abajo
                val (lat, lng) = inverse(cx, cy, z)
                val zoom = if (points.size == 1) min(z, 15) else z
                return Triple(zoom, lat, lng)
            }
        }
        val c = points.first()
        return Triple(2, c.lat, c.lng)
    }

    companion object {
        private const val TAG = "OsmMap"
        private const val TILE = 256.0

        /** Proyección Web Mercator a píxeles del mundo para un zoom. */
        fun mercator(lat: Double, lng: Double, zoom: Int): Pair<Double, Double> {
            val n = TILE * 2.0.pow(zoom)
            val x = (lng + 180.0) / 360.0 * n
            val latRad = Math.toRadians(lat.coerceIn(-85.05, 85.05))
            val y = (1.0 - ln(tan(latRad) + 1.0 / Math.cos(latRad)) / PI) / 2.0 * n
            return Pair(x, y)
        }

        fun inverse(x: Double, y: Double, zoom: Int): Pair<Double, Double> {
            val n = TILE * 2.0.pow(zoom)
            val lng = x / n * 360.0 - 180.0
            val lat = Math.toDegrees(atan(sinh(PI * (1 - 2 * y / n))))
            return Pair(lat, lng)
        }
    }
}

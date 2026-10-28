package com.mifleteciudad.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifleteciudad.core.ServiceZone
import com.mifleteciudad.core.LatLng
import kotlin.math.cos
import kotlin.math.min

/**
 * Mapa esquemático dibujado en Canvas: contorno de la zona de cobertura, recorrido y
 * marcadores de origen (A) y destino (B). Funciona sin clave de Google ni SDK de mapas.
 */
@Composable
fun MiniMap(
    zone: ServiceZone,
    origin: LatLng?,
    destination: LatLng?,
    path: List<LatLng>,
    modifier: Modifier = Modifier,
    originInside: Boolean = true,
    destinationInside: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier) {
        // Encuadre: la zona completa, más un pequeño margen, respetando el aspecto real.
        val cosLat = cos(Math.toRadians(zone.center.lat)).toFloat()
        val pad = ((zone.maxLat - zone.minLat) * 0.07).toFloat()
        val minLng = (zone.minLng - pad).toFloat()
        val maxLng = (zone.maxLng + pad).toFloat()
        val minLat = (zone.minLat - pad).toFloat()
        val maxLat = (zone.maxLat + pad).toFloat()
        val worldW = (maxLng - minLng) * cosLat
        val worldH = maxLat - minLat
        val scale = min(size.width / worldW, size.height / worldH)
        val offX = (size.width - worldW * scale) / 2f
        val offY = (size.height - worldH * scale) / 2f

        fun project(p: LatLng): Offset {
            val x = offX + ((p.lng.toFloat() - minLng) * cosLat) * scale
            val y = offY + (maxLat - p.lat.toFloat()) * scale
            return Offset(x.coerceIn(10f, size.width - 10f), y.coerceIn(10f, size.height - 10f))
        }

        // Contorno de la zona
        val outline = Path()
        zone.polygon.forEachIndexed { i, p ->
            val o = project(p)
            if (i == 0) outline.moveTo(o.x, o.y) else outline.lineTo(o.x, o.y)
        }
        outline.close()
        drawPath(outline, colors.surfaceVariant)
        drawPath(outline, colors.outline, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))

        // Recorrido
        val o = origin?.let(::project)
        val d = destination?.let(::project)
        if (o != null && d != null) {
            if (path.size >= 2) {
                val route = Path()
                path.forEachIndexed { i, p ->
                    val pt = project(p)
                    if (i == 0) route.moveTo(pt.x, pt.y) else route.lineTo(pt.x, pt.y)
                }
                drawPath(route, colors.primary.copy(alpha = 0.25f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(route, colors.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawLine(
                    colors.primary, o, d,
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
                )
            }
        }

        // Marcadores
        o?.let { marker(it, "A", if (originInside) colors.primary else colors.error, measurer, labelStyle) }
        d?.let { marker(it, "B", if (destinationInside) colors.tertiary else colors.error, measurer, labelStyle) }
    }
}

private fun DrawScope.marker(
    at: Offset,
    label: String,
    color: Color,
    measurer: androidx.compose.ui.text.TextMeasurer,
    style: TextStyle,
) {
    val r = 11.dp.toPx()
    drawCircle(color.copy(alpha = 0.25f), r * 1.7f, at)
    drawCircle(Color.White, r + 2.dp.toPx(), at)
    drawCircle(color, r, at)
    val layout = measurer.measure(label, style)
    drawText(
        layout,
        topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f),
    )
}

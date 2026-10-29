package com.fletarg.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fletarg.core.Argentina
import com.fletarg.core.LatLng
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Tarjeta de mapa. Si hay una imagen real (Maps Static API) la muestra; si no, dibuja un
 * esquema con fondo de manzanas, el recorrido trazándose animado y una letra por parada.
 * Encima, una etiqueta con la distancia y, si falta información, un estado vacío que explica qué hacer.
 */
@Composable
fun MapCard(
    points: List<LatLng?>,
    insideFlags: List<Boolean>,
    path: List<LatLng>,
    image: ImageBitmap?,
    caption: String,
    emptyHint: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        Crossfade(targetState = image, label = "map") { img ->
            if (img != null) {
                Image(bitmap = img, contentDescription = "Mapa del recorrido", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                SchematicMap(points, insideFlags, path, Modifier.fillMaxSize())
            }
        }
        // Degradé inferior para que la etiqueta se lea sobre cualquier mapa
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.surface.copy(alpha = 0.9f))),
        )
        if (emptyHint != null) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = colors.surface.copy(alpha = 0.92f), shadowElevation = 2.dp) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(emptyHint, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = colors.surface.copy(alpha = 0.96f),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            ) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Esquema en Canvas: encuadra las paradas, fondo de manzanas, recorrido animado y letras. */
@Composable
fun SchematicMap(
    points: List<LatLng?>,
    insideFlags: List<Boolean>,
    path: List<LatLng>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val known = points.filterNotNull()
    val pathKey = "${path.size}-${known.size}-${known.lastOrNull()}"
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pathKey) {
        if (known.size >= 2) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 900))
        } else {
            progress.snapTo(0f)
        }
    }
    val measure = remember { PathMeasure() }
    val segment = remember { Path() }
    val blockColor = colors.onSurface.copy(alpha = 0.045f)
    val streetColor = colors.surface

    Canvas(modifier.background(colors.surfaceVariant.copy(alpha = 0.6f))) {
        // Fondo tipo plano de ciudad: manzanas separadas por calles.
        val block = 30.dp.toPx()
        val street = 5.dp.toPx()
        var y = -block / 2
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) -block / 3 else -block * 0.7f
            while (x < size.width) {
                drawRoundRect(blockColor, Offset(x, y), Size(block, block), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                x += block + street
            }
            y += block + street
            row++
        }
        // Avenidas más anchas
        drawLine(streetColor, Offset(0f, size.height * 0.38f), Offset(size.width, size.height * 0.38f), 7.dp.toPx())
        drawLine(streetColor, Offset(size.width * 0.62f, 0f), Offset(size.width * 0.62f, size.height), 7.dp.toPx())

        // Encuadre de las paradas
        val lats = known.map { it.lat }
        val lngs = known.map { it.lng }
        val cLat = if (known.isEmpty()) Argentina.center.lat else (lats.min() + lats.max()) / 2
        val cLng = if (known.isEmpty()) Argentina.center.lng else (lngs.min() + lngs.max()) / 2
        val cosLat = cos(Math.toRadians(cLat)).toFloat().coerceAtLeast(0.2f)
        var spanLat = if (known.size >= 2) (lats.max() - lats.min()).toFloat() else 0f
        var spanLng = if (known.size >= 2) ((lngs.max() - lngs.min()) * cosLat).toFloat() else 0f
        val minSpan = 0.02f
        spanLat = max(spanLat, minSpan)
        spanLng = max(spanLng, minSpan)
        val padPx = 40.dp.toPx()
        val scale = min((size.width - 2 * padPx) / spanLng, (size.height - 2 * padPx - 24.dp.toPx()) / spanLat)

        fun project(p: LatLng): Offset {
            val x = size.width / 2 + ((p.lng - cLng).toFloat() * cosLat) * scale
            val y2 = size.height / 2 - 10.dp.toPx() - (p.lat - cLat).toFloat() * scale
            return Offset(x, y2)
        }

        val projected = points.map { it?.let(::project) }
        val resolved = projected.filterNotNull()

        if (resolved.size >= 2) {
            val full = Path()
            val source = if (path.size >= 2) path.map(::project) else resolved
            source.forEachIndexed { i, pt -> if (i == 0) full.moveTo(pt.x, pt.y) else full.lineTo(pt.x, pt.y) }
            measure.setPath(full, false)
            segment.reset()
            measure.getSegment(0f, measure.length * progress.value, segment, true)
            drawPath(segment, colors.primary.copy(alpha = 0.18f), style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (path.size >= 2) {
                drawPath(segment, colors.primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawPath(
                    segment, colors.primary,
                    style = Stroke(
                        width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())),
                    ),
                )
            }
        }

        projected.forEachIndexed { i, pt ->
            if (pt == null) return@forEachIndexed
            val inside = insideFlags.getOrElse(i) { true }
            val color = when {
                !inside -> colors.error
                i == 0 -> colors.primary
                i == points.lastIndex -> colors.tertiary
                else -> colors.secondary
            }
            marker(pt, ('A' + i).toString(), color, measurer, labelStyle)
        }
    }
}

private fun DrawScope.marker(at: Offset, label: String, color: Color, measurer: TextMeasurer, style: TextStyle) {
    val r = 11.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.12f), r + 3.dp.toPx(), at + Offset(0f, 2.dp.toPx()))
    drawCircle(color.copy(alpha = 0.22f), r * 1.8f, at)
    drawCircle(Color.White, r + 2.dp.toPx(), at)
    drawCircle(color, r, at)
    val layout = measurer.measure(label, style)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

package com.fletarg.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.fletarg.core.VehicleKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Íconos de vehículos dibujados en código (vista lateral, frente a la derecha) con detalles:
 * ventanas, faro, paragolpes, espejo, línea de puerta, sombreado inferior y ruedas con llanta.
 * Sin imágenes: nítidos a cualquier tamaño, pesan cero y se tiñen según el tema.
 * El dibujo usa un espacio de 100 × 56 unidades que se escala al tamaño del Canvas.
 */
@Composable
fun VehicleIcon(
    kind: VehicleKind,
    modifier: Modifier = Modifier,
    body: Color = MaterialTheme.colorScheme.primary,
    detail: Color = MaterialTheme.colorScheme.onSurface,
    wheelRotation: Float = 0f,
) {
    val palette = VehiclePalette(
        body = body,
        shade = Color.Black.copy(alpha = 0.14f),
        glass = Color.White.copy(alpha = 0.66f),
        light = Color(0xFFFFE6A3),
        tire = detail,
        rim = Color.White.copy(alpha = 0.85f),
        ground = detail.copy(alpha = 0.14f),
    )
    Canvas(modifier) {
        val s = min(size.width / 100f, size.height / 56f)
        val dx = (size.width - 100f * s) / 2f
        val dy = (size.height - 56f * s) / 2f
        translate(dx, dy) {
            scale(s, s, pivot = Offset.Zero) {
                drawVehicle(kind, palette, wheelRotation)
            }
        }
    }
}

internal class VehiclePalette(
    val body: Color,
    val shade: Color,
    val glass: Color,
    val light: Color,
    val tire: Color,
    val rim: Color,
    val ground: Color,
)

private fun DrawScope.drawVehicle(kind: VehicleKind, p: VehiclePalette, rot: Float) {
    drawLine(p.ground, Offset(2f, 54f), Offset(98f, 54f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    when (kind) {
        VehicleKind.KANGOO -> {
            van(p, x0 = 14f, top = 16f, cabStart = 58f, noseStart = 70f, noseTop = 30f, x1 = 88f)
            wheels(p, listOf(30f, 74f), rot, radius = 6.5f)
        }
        VehicleKind.FURGON_MEDIANO -> {
            van(p, x0 = 6f, top = 8f, cabStart = 60f, noseStart = 73f, noseTop = 26f, x1 = 93f, doorLine = 54f)
            wheels(p, listOf(22f, 78f), rot)
        }
        VehicleKind.FURGON_GRANDE -> {
            van(p, x0 = 4f, top = 2f, cabStart = 64f, noseStart = 79f, noseTop = 24f, x1 = 95f, doorLine = 56f)
            wheels(p, listOf(22f, 78f), rot)
        }
        VehicleKind.PICKUP -> {
            // Caja abierta atrás
            drawRoundRect(p.body, Offset(4f, 24f), Size(42f, 20f), CornerRadius(3f))
            drawRoundRect(p.shade, Offset(4f, 38f), Size(42f, 6f), CornerRadius(2f))
            drawRoundRect(p.glass, Offset(8f, 26f), Size(34f, 3.5f), CornerRadius(2f))
            val cab = Path().apply {
                moveTo(46f, 44f); lineTo(46f, 14f); quadraticTo(46f, 10f, 50f, 10f)
                lineTo(68f, 10f); lineTo(80f, 24f); lineTo(92f, 24f)
                quadraticTo(95f, 24f, 95f, 27f); lineTo(95f, 44f); close()
            }
            drawPath(cab, p.body)
            drawRoundRect(p.shade, Offset(46f, 38f), Size(49f, 6f), CornerRadius(2f))
            drawPath(Path().apply { moveTo(52f, 13f); lineTo(67f, 13f); lineTo(76f, 24f); lineTo(52f, 24f); close() }, p.glass)
            drawLine(p.shade, Offset(60f, 14f), Offset(60f, 24f), strokeWidth = 1.2f)
            front(p, x = 95f, lightY = 28f, bumperY = 42f, bumperX0 = 80f)
            mirror(p, x = 79f, y = 20f)
            wheels(p, listOf(20f, 78f), rot)
        }
        VehicleKind.CAMION_3_4 -> {
            box(p, 2f, 8f, 56f, 36f)
            truckCab(p, x0 = 62f, top = 12f, x1 = 95f)
            wheels(p, listOf(20f, 78f), rot)
        }
        VehicleKind.CAMION_MEDIANO_CERRADO -> {
            box(p, 0f, 4f, 62f, 40f)
            truckCab(p, x0 = 66f, top = 10f, x1 = 97f)
            wheels(p, listOf(15f, 31f, 81f), rot)
        }
        VehicleKind.CAMION_GRANDE_CERRADO -> {
            box(p, 0f, 0f, 64f, 44f)
            truckCab(p, x0 = 67f, top = 6f, x1 = 99f)
            wheels(p, listOf(12f, 27f, 83f), rot)
        }
        VehicleKind.CAMION_PLAYO -> {
            drawRoundRect(p.body, Offset(0f, 34f), Size(64f, 10f), CornerRadius(2f))
            drawRoundRect(p.shade, Offset(0f, 40f), Size(64f, 4f), CornerRadius(2f))
            drawRoundRect(p.glass, Offset(2f, 26f), Size(60f, 8f), CornerRadius(1.5f))
            for (x in listOf(2f, 17f, 31f, 45f, 60f)) drawRect(p.body, Offset(x, 26f), Size(2f, 8f))
            truckCab(p, x0 = 66f, top = 10f, x1 = 97f)
            wheels(p, listOf(15f, 31f, 81f), rot)
        }
    }
}

/** Caja cerrada con sombreado inferior y línea de puerta trasera. */
private fun DrawScope.box(p: VehiclePalette, x: Float, y: Float, w: Float, h: Float) {
    drawRoundRect(p.body, Offset(x, y), Size(w, h), CornerRadius(3f))
    drawRoundRect(p.shade, Offset(x, y + h - 7f), Size(w, 7f), CornerRadius(2f))
    drawLine(p.glass, Offset(x + 3f, y + 3f), Offset(x + w - 3f, y + 3f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    drawLine(p.shade, Offset(x + 8f, y + 6f), Offset(x + 8f, y + h - 8f), strokeWidth = 1.2f)
}

/** Utilitario / furgón: una sola pieza con trompa inclinada. */
private fun DrawScope.van(
    p: VehiclePalette,
    x0: Float, top: Float, cabStart: Float, noseStart: Float, noseTop: Float, x1: Float,
    doorLine: Float? = null,
) {
    val path = Path().apply {
        moveTo(x0, 44f); lineTo(x0, top + 4f)
        quadraticTo(x0, top, x0 + 4f, top)
        lineTo(noseStart, top)
        lineTo(x1 - 3f, noseTop)
        quadraticTo(x1, noseTop + 2f, x1, noseTop + 5f)
        lineTo(x1, 44f); close()
    }
    drawPath(path, p.body)
    drawRoundRect(p.shade, Offset(x0, 38f), Size(x1 - x0, 6f), CornerRadius(2f))
    val window = Path().apply {
        moveTo(cabStart, top + 3f); lineTo(noseStart - 1f, top + 3f)
        lineTo(x1 - 6f, noseTop + 1f); lineTo(cabStart, noseTop + 1f); close()
    }
    drawPath(window, p.glass)
    // Ventana lateral trasera pequeña y línea de puerta corrediza
    if (doorLine != null) {
        drawLine(p.shade, Offset(doorLine, top + 4f), Offset(doorLine, 40f), strokeWidth = 1.4f)
        drawRoundRect(p.glass, Offset(doorLine + 4f, top + 4f), Size(cabStart - doorLine - 8f, 9f), CornerRadius(1.5f))
        drawLine(p.shade, Offset(doorLine + 4f, 30f), Offset(doorLine + 10f, 30f), strokeWidth = 1.6f, cap = StrokeCap.Round)
    }
    drawLine(p.glass, Offset(x0 + 4f, top + 1.5f), Offset(noseStart - 2f, top + 1.5f), strokeWidth = 1f, cap = StrokeCap.Round)
    front(p, x = x1, lightY = noseTop + 7f, bumperY = 42f, bumperX0 = x1 - 14f)
    mirror(p, x = noseStart - 2f, y = noseTop - 1f)
}

/** Cabina de camión separada de la caja. */
private fun DrawScope.truckCab(p: VehiclePalette, x0: Float, top: Float, x1: Float) {
    val path = Path().apply {
        moveTo(x0, 44f); lineTo(x0, top + 4f)
        quadraticTo(x0, top, x0 + 4f, top)
        lineTo(x1 - 14f, top)
        lineTo(x1 - 4f, top + 12f)
        quadraticTo(x1, top + 13f, x1, top + 17f)
        lineTo(x1, 44f); close()
    }
    drawPath(path, p.body)
    drawRoundRect(p.shade, Offset(x0, 38f), Size(x1 - x0, 6f), CornerRadius(2f))
    val window = Path().apply {
        moveTo(x0 + 3f, top + 3f); lineTo(x1 - 15f, top + 3f)
        lineTo(x1 - 7f, top + 12f); lineTo(x0 + 3f, top + 12f); close()
    }
    drawPath(window, p.glass)
    drawLine(p.shade, Offset(x0 + 12f, top + 14f), Offset(x0 + 12f, 37f), strokeWidth = 1.2f)
    drawLine(p.shade, Offset(x0 + 14f, top + 20f), Offset(x0 + 19f, top + 20f), strokeWidth = 1.6f, cap = StrokeCap.Round)
    front(p, x = x1, lightY = top + 20f, bumperY = 42f, bumperX0 = x1 - 10f)
    mirror(p, x = x1 - 15f, y = top + 10f)
}

/** Faro y paragolpes. */
private fun DrawScope.front(p: VehiclePalette, x: Float, lightY: Float, bumperY: Float, bumperX0: Float) {
    drawRoundRect(p.light, Offset(x - 4.5f, lightY), Size(3.5f, 4.5f), CornerRadius(1.2f))
    drawRoundRect(p.shade, Offset(bumperX0, bumperY), Size(x - bumperX0 + 1f, 3f), CornerRadius(1.5f))
}

private fun DrawScope.mirror(p: VehiclePalette, x: Float, y: Float) {
    drawRoundRect(p.body, Offset(x + 1.5f, y), Size(3f, 4f), CornerRadius(1f))
}

private fun DrawScope.wheels(p: VehiclePalette, xs: List<Float>, rotation: Float, radius: Float = 7f) {
    xs.forEach { x ->
        val c = Offset(x, 46f)
        drawCircle(p.tire, radius, c)
        drawCircle(p.rim, radius * 0.5f, c)
        drawCircle(p.tire.copy(alpha = 0.8f), radius * 0.16f, c)
        // Rayos de la llanta: giran al animar.
        val rad = Math.toRadians(rotation.toDouble())
        for (i in 0 until 3) {
            val a = rad + i * (Math.PI * 2 / 3)
            val end = Offset(c.x + (radius * 0.45f * cos(a)).toFloat(), c.y + (radius * 0.45f * sin(a)).toFloat())
            drawLine(p.tire.copy(alpha = 0.7f), c, end, strokeWidth = 1.1f, cap = StrokeCap.Round)
        }
    }
}

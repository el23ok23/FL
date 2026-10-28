package com.mifleteciudad.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.mifleteciudad.core.VehicleKind
import kotlin.math.min

/**
 * Íconos de vehículos dibujados en código (vista lateral, frente a la derecha).
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
    val glass = Color.White.copy(alpha = 0.62f)
    Canvas(modifier) {
        val s = min(size.width / 100f, size.height / 56f)
        val dx = (size.width - 100f * s) / 2f
        val dy = (size.height - 56f * s) / 2f
        translate(dx, dy) {
            scale(s, s, pivot = Offset.Zero) {
                drawVehicle(kind, body, glass, detail, wheelRotation)
            }
        }
    }
}

private fun DrawScope.drawVehicle(kind: VehicleKind, body: Color, glass: Color, detail: Color, wheelRotation: Float) {
    when (kind) {
        VehicleKind.KANGOO -> {
            van(x0 = 14f, top = 16f, cabStart = 60f, noseStart = 72f, noseTop = 30f, x1 = 88f, body = body, glass = glass)
            wheels(listOf(30f, 74f), detail, body, wheelRotation, radius = 6.5f)
        }
        VehicleKind.FURGON_MEDIANO -> {
            van(x0 = 6f, top = 8f, cabStart = 62f, noseStart = 74f, noseTop = 26f, x1 = 93f, body = body, glass = glass, doorLine = 56f)
            wheels(listOf(22f, 78f), detail, body, wheelRotation)
        }
        VehicleKind.FURGON_GRANDE -> {
            van(x0 = 4f, top = 2f, cabStart = 66f, noseStart = 80f, noseTop = 24f, x1 = 95f, body = body, glass = glass, doorLine = 58f)
            wheels(listOf(22f, 78f), detail, body, wheelRotation)
        }
        VehicleKind.PICKUP -> {
            // Caja abierta atrás
            drawRoundRect(body, Offset(4f, 24f), Size(42f, 20f), CornerRadius(3f))
            drawRoundRect(glass, Offset(8f, 26f), Size(34f, 4f), CornerRadius(2f))
            // Cabina y trompa
            val cab = Path().apply {
                moveTo(46f, 44f); lineTo(46f, 14f); quadraticTo(46f, 10f, 50f, 10f)
                lineTo(68f, 10f); lineTo(80f, 24f); lineTo(92f, 24f)
                quadraticTo(95f, 24f, 95f, 27f); lineTo(95f, 44f); close()
            }
            drawPath(cab, body)
            drawPath(Path().apply { moveTo(52f, 13f); lineTo(67f, 13f); lineTo(76f, 24f); lineTo(52f, 24f); close() }, glass)
            wheels(listOf(20f, 78f), detail, body, wheelRotation)
        }
        VehicleKind.CAMION_3_4 -> {
            drawRoundRect(body, Offset(2f, 8f), Size(56f, 36f), CornerRadius(3f))
            truckCab(x0 = 62f, top = 12f, x1 = 95f, body = body, glass = glass)
            wheels(listOf(20f, 78f), detail, body, wheelRotation)
        }
        VehicleKind.CAMION_MEDIANO_CERRADO -> {
            drawRoundRect(body, Offset(0f, 4f), Size(62f, 40f), CornerRadius(3f))
            truckCab(x0 = 66f, top = 10f, x1 = 97f, body = body, glass = glass)
            wheels(listOf(15f, 31f, 81f), detail, body, wheelRotation)
        }
        VehicleKind.CAMION_GRANDE_CERRADO -> {
            drawRoundRect(body, Offset(0f, 0f), Size(64f, 44f), CornerRadius(3f))
            truckCab(x0 = 67f, top = 6f, x1 = 99f, body = body, glass = glass)
            wheels(listOf(12f, 27f, 83f), detail, body, wheelRotation)
        }
        VehicleKind.CAMION_PLAYO -> {
            // Plataforma con barandas bajas
            drawRoundRect(body, Offset(0f, 34f), Size(64f, 10f), CornerRadius(2f))
            drawRoundRect(glass, Offset(2f, 26f), Size(60f, 8f), CornerRadius(1.5f))
            drawRect(body, Offset(2f, 26f), Size(2f, 8f))
            drawRect(body, Offset(60f, 26f), Size(2f, 8f))
            drawRect(body, Offset(31f, 26f), Size(2f, 8f))
            truckCab(x0 = 66f, top = 10f, x1 = 97f, body = body, glass = glass)
            wheels(listOf(15f, 31f, 81f), detail, body, wheelRotation)
        }
    }
}

/** Utilitario / furgón: una sola pieza con trompa inclinada. */
private fun DrawScope.van(
    x0: Float, top: Float, cabStart: Float, noseStart: Float, noseTop: Float, x1: Float,
    body: Color, glass: Color, doorLine: Float? = null,
) {
    val path = Path().apply {
        moveTo(x0, 44f); lineTo(x0, top + 4f)
        quadraticTo(x0, top, x0 + 4f, top)
        lineTo(noseStart, top)
        lineTo(x1 - 3f, noseTop)
        quadraticTo(x1, noseTop + 2f, x1, noseTop + 5f)
        lineTo(x1, 44f); close()
    }
    drawPath(path, body)
    val window = Path().apply {
        moveTo(cabStart, top + 3f); lineTo(noseStart - 1f, top + 3f)
        lineTo(x1 - 6f, noseTop + 1f); lineTo(cabStart, noseTop + 1f); close()
    }
    drawPath(window, glass)
    if (doorLine != null) {
        drawLine(glass, Offset(doorLine, top + 4f), Offset(doorLine, 42f), strokeWidth = 1.5f)
    }
}

/** Cabina de camión separada de la caja. */
private fun DrawScope.truckCab(x0: Float, top: Float, x1: Float, body: Color, glass: Color) {
    val path = Path().apply {
        moveTo(x0, 44f); lineTo(x0, top + 4f)
        quadraticTo(x0, top, x0 + 4f, top)
        lineTo(x1 - 14f, top)
        lineTo(x1 - 4f, top + 12f)
        quadraticTo(x1, top + 13f, x1, top + 17f)
        lineTo(x1, 44f); close()
    }
    drawPath(path, body)
    val window = Path().apply {
        moveTo(x0 + 3f, top + 3f); lineTo(x1 - 15f, top + 3f)
        lineTo(x1 - 7f, top + 12f); lineTo(x0 + 3f, top + 12f); close()
    }
    drawPath(window, glass)
}

private fun DrawScope.wheels(xs: List<Float>, tire: Color, hub: Color, rotation: Float, radius: Float = 7f) {
    xs.forEach { x ->
        val c = Offset(x, 46f)
        drawCircle(tire, radius, c)
        drawCircle(hub.copy(alpha = 0.9f), radius * 0.45f, c)
        if (rotation != 0f) {
            // Marca en la llanta para que se note el giro al animar.
            val rad = Math.toRadians(rotation.toDouble())
            val end = Offset(c.x + (radius * 0.4f * Math.cos(rad)).toFloat(), c.y + (radius * 0.4f * Math.sin(rad)).toFloat())
            drawLine(tire, c, end, strokeWidth = 1.5f)
        }
    }
    drawLine(tire.copy(alpha = 0.15f), Offset(2f, 54f), Offset(98f, 54f), strokeWidth = 1.2f)
}

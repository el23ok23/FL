package com.fletarg.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Círculo que aparece con rebote y tilde que se dibuja de un trazo. ~700 ms en total. */
@Composable
fun AnimatedCheck(modifier: Modifier = Modifier) {
    val circle = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { circle.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
        launch { tick.animateTo(1f, tween(durationMillis = 450, delayMillis = 180, easing = FastOutSlowInEasing)) }
    }
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val measure = remember { PathMeasure() }
    val segment = remember { Path() }

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * circle.value
        drawCircle(primary.copy(alpha = 0.18f), r * 1.18f, c)
        drawCircle(primary, r, c)

        val w = size.minDimension
        val full = Path().apply {
            moveTo(c.x - w * 0.22f, c.y + w * 0.02f)
            lineTo(c.x - w * 0.06f, c.y + w * 0.17f)
            lineTo(c.x + w * 0.24f, c.y - w * 0.15f)
        }
        measure.setPath(full, false)
        segment.reset()
        measure.getSegment(0f, measure.length * tick.value, segment, true)
        drawPath(
            segment, onPrimary,
            style = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

package com.fletaba.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fletaba.app.ui.components.VehicleIcon
import com.fletaba.app.ui.theme.FletabaOrange
import com.fletaba.core.VehicleKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de inicio: la camioneta entra desde la izquierda con las ruedas girando, frena con
 * un pequeño rebote y "deja" el nombre. Dura ~1,3 s y pasa sola (o antes, si se toca).
 */
@Composable
fun IntroScreen(title: String, onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val truckX = remember { Animatable(-1.2f) } // en fracciones del ancho
    val wheel = remember { Animatable(0f) }
    val road = remember { Animatable(0f) }
    var titleVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { wheel.animateTo(900f, tween(durationMillis = 850)) }
        launch { road.animateTo(1f, tween(durationMillis = 850)) }
        launch { truckX.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow)) }
        delay(430)
        titleVisible = true
        delay(900)
        finish()
    }

    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(colors.primaryContainer.copy(alpha = 0.95f), colors.primaryContainer.copy(alpha = 0f)))),
                )
                // Ruta: líneas discontinuas que corren debajo de la camioneta
                val dash = colors.onSurface.copy(alpha = 0.18f)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .offset(y = 44.dp),
                ) {
                    val y = size.height - 4.dp.toPx()
                    val dashW = 26.dp.toPx()
                    val gap = 18.dp.toPx()
                    val shift = (road.value * (dashW + gap) * 6f) % (dashW + gap)
                    var x = -shift
                    while (x < size.width) {
                        drawLine(dash, Offset(x, y), Offset(x + dashW, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                        x += dashW + gap
                    }
                }
                Box(Modifier.offset { IntOffset((truckX.value * widthPx).toInt(), 0) }) {
                    VehicleIcon(
                        kind = VehicleKind.FURGON_MEDIANO,
                        modifier = Modifier.size(width = 180.dp, height = 100.dp),
                        wheelRotation = wheel.value,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        title,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Desde Capital Federal a todo el país",
                        style = MaterialTheme.typography.titleMedium,
                        color = FletabaOrange,
                    )
                }
            }
        }
    }
}

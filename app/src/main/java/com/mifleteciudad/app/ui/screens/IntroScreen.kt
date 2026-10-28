package com.mifleteciudad.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.ui.components.VehicleIcon
import com.mifleteciudad.core.VehicleKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de inicio: la camioneta entra desde la izquierda, frena y "deja" el nombre.
 * Dura ~1,3 s y pasa sola a la pantalla principal (o antes, si se toca).
 */
@Composable
fun IntroScreen(title: String, subtitle: String, onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val truckX = remember { Animatable(-1.2f) } // en fracciones del ancho
    val wheel = remember { Animatable(0f) }
    var titleVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { wheel.animateTo(720f, tween(durationMillis = 800)) }
        launch { truckX.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)) }
        delay(420)
        titleVisible = true
        delay(880)
        finish()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.offset { androidx.compose.ui.unit.IntOffset((truckX.value * widthPx).toInt(), 0) },
            ) {
                VehicleIcon(
                    kind = VehicleKind.FURGON_MEDIANO,
                    modifier = Modifier.size(width = 150.dp, height = 84.dp),
                    wheelRotation = wheel.value,
                )
            }
            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        title,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

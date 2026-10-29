package com.fletaba.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fletaba.app.OrderViewModel
import com.fletaba.app.Step
import com.fletaba.app.ui.screens.DetailsScreen
import com.fletaba.app.ui.screens.DoneScreen
import com.fletaba.app.ui.screens.IntroScreen
import com.fletaba.app.ui.screens.QuoteScreen
import com.fletaba.app.ui.screens.RouteScreen
import com.fletaba.app.ui.screens.VehicleScreen

/** Pasos que cuentan para la barra de progreso (sin intro ni confirmación). */
private const val TOTAL_STEPS = 4

/**
 * Navegación del flujo. Sin librería de navegación: un solo estado y una transición
 * animada entre pantallas (deslizamiento corto + fundido + leve zoom) para que sea liviano y fluido.
 */
@Composable
fun FletabaApp(vm: OrderViewModel = viewModel()) {
    val step = vm.step

    BackHandler(enabled = step != Step.INTRO && step != Step.ROUTE) { vm.back() }

    AnimatedContent(
        targetState = step,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        transitionSpec = {
            val forward = targetState.ordinal >= initialState.ordinal
            val dir = if (forward) 1 else -1
            if (initialState == Step.INTRO || targetState == Step.DONE || initialState == Step.DONE) {
                (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith fadeOut(tween(200))
            } else {
                (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { dir * it / 5 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -dir * it / 5 } + fadeOut(tween(160)))
            }
        },
        label = "steps",
    ) { current ->
        when (current) {
            Step.INTRO -> IntroScreen(title = vm.config.businessName, onFinished = { vm.finishIntro() })
            Step.ROUTE -> RouteScreen(vm, step = 1, total = TOTAL_STEPS)
            Step.VEHICLE -> VehicleScreen(vm, step = 2, total = TOTAL_STEPS)
            Step.DETAILS -> DetailsScreen(vm, step = 3, total = TOTAL_STEPS)
            Step.QUOTE -> QuoteScreen(vm, step = 4, total = TOTAL_STEPS)
            Step.DONE -> DoneScreen(vm)
        }
    }
}

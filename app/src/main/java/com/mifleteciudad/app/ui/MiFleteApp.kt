package com.mifleteciudad.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.Step
import com.mifleteciudad.app.ui.screens.CargoScreen
import com.mifleteciudad.app.ui.screens.ContactScreen
import com.mifleteciudad.app.ui.screens.DoneScreen
import com.mifleteciudad.app.ui.screens.FloorsScreen
import com.mifleteciudad.app.ui.screens.HelpersScreen
import com.mifleteciudad.app.ui.screens.IntroScreen
import com.mifleteciudad.app.ui.screens.PriceScreen
import com.mifleteciudad.app.ui.screens.RouteScreen
import com.mifleteciudad.app.ui.screens.VehicleScreen
import com.mifleteciudad.app.ui.screens.WhenScreen

/** Pasos que cuentan para la barra de progreso (sin intro ni confirmación). */
private const val TOTAL_STEPS = 8

/**
 * Navegación del flujo. Sin librería de navegación: un solo estado y una transición
 * animada entre pantallas (deslizamiento corto + fundido) para que sea liviano y fluido.
 */
@Composable
fun MiFleteApp(vm: OrderViewModel = viewModel()) {
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
                fadeIn(tween(280)) togetherWith fadeOut(tween(200))
            } else {
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { dir * it / 5 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -dir * it / 5 } + fadeOut(tween(160)))
            }
        },
        label = "steps",
    ) { current ->
        when (current) {
            Step.INTRO -> IntroScreen(
                title = vm.config.businessName,
                subtitle = "Fletes en ${vm.config.zone.name}",
                onFinished = { vm.finishIntro() },
            )
            Step.ROUTE -> RouteScreen(vm, step = 1, total = TOTAL_STEPS)
            Step.VEHICLE -> VehicleScreen(vm, step = 2, total = TOTAL_STEPS)
            Step.HELPERS -> HelpersScreen(vm, step = 3, total = TOTAL_STEPS)
            Step.FLOORS -> FloorsScreen(vm, step = 4, total = TOTAL_STEPS)
            Step.CARGO -> CargoScreen(vm, step = 5, total = TOTAL_STEPS)
            Step.WHEN -> WhenScreen(vm, step = 6, total = TOTAL_STEPS)
            Step.PRICE -> PriceScreen(vm, step = 7, total = TOTAL_STEPS)
            Step.CONTACT -> ContactScreen(vm, step = 8, total = TOTAL_STEPS)
            Step.DONE -> DoneScreen(vm)
        }
    }
}

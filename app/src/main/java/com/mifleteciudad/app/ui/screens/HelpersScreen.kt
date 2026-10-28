package com.mifleteciudad.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.ChoiceCard
import com.mifleteciudad.app.ui.components.PriceBar
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.core.Money

@Composable
fun HelpersScreen(vm: OrderViewModel, step: Int, total: Int) {
    val perHelper = vm.config.pricing.perHelper
    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿Necesitás ayudantes?",
        subtitle = "Para cargar y descargar. El chofer siempre va incluido.",
        onBack = { vm.back() },
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Continuar", onClick = { vm.next() })
        },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (n in 0..2) {
                HelperOption(
                    count = n,
                    extra = if (n == 0) "Sin cargo" else "+ ${Money.formatArs(perHelper * n)}",
                    selected = vm.helpers == n,
                    onClick = { vm.selectHelpers(n) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        AnimatedContent(
            targetState = vm.helpers,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "helpersHint",
        ) { n ->
            Text(
                when (n) {
                    0 -> "Vos cargás y descargás. Ideal para cajas y cosas livianas."
                    1 -> "Un ayudante para muebles medianos, electrodomésticos y cajas."
                    else -> "Dos ayudantes para mudanzas o cosas muy pesadas."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HelperOption(
    count: Int,
    extra: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    ChoiceCard(selected = selected, onClick = onClick, modifier = modifier, padding = 12.dp) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    repeat(maxOf(count, 1)) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = when {
                                count == 0 -> colors.outline
                                selected -> colors.primary
                                else -> colors.onSurfaceVariant
                            },
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = if (selected) colors.primary else colors.onSurface,
            )
            Text(
                if (count == 1) "ayudante" else "ayudantes",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                extra,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.primary else colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

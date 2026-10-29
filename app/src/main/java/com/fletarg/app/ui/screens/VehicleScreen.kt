package com.fletarg.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.fletarg.app.OrderViewModel
import com.fletarg.app.ui.components.ChoiceCard
import com.fletarg.app.ui.components.PriceBar
import com.fletarg.app.ui.components.StepScaffold
import com.fletarg.app.ui.components.VehicleIcon
import com.fletarg.core.Money
import kotlin.math.absoluteValue

@Composable
fun VehicleScreen(vm: OrderViewModel, step: Int, total: Int) {
    val vehicles = vm.config.vehicles
    val colors = MaterialTheme.colorScheme
    val pagerState = rememberPagerState(initialPage = vm.vehicleIndex) { vehicles.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { vm.selectVehicle(it) }
    }

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "Vehículo y ayudantes",
        subtitle = "Deslizá para elegir el vehículo. El precio se actualiza solo.",
        onBack = { vm.back() },
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Continuar", onClick = { vm.next() })
        },
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 14.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val v = vehicles[page]
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val scale = lerp(1f, 0.9f, offset)
            val alpha = lerp(1f, 0.5f, offset)
            val selected = page == pagerState.currentPage
            val border by animateColorAsState(if (selected) colors.primary else colors.outlineVariant, label = "border")
            val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "bw")

            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surface,
                border = BorderStroke(borderWidth, border),
                shadowElevation = if (selected) 6.dp else 0.dp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .fillMaxWidth()
                    .height(272.dp),
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.primaryContainer.copy(alpha = 0.7f), colors.primaryContainer.copy(alpha = 0.15f)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        VehicleIcon(kind = v.kind, modifier = Modifier.size(width = 190.dp, height = 104.dp))
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(v.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Text(v.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text(v.dimensions, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Text(v.capacity, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Desde ${Money.formatArs(v.baseFare)} + ${Money.formatArs(v.perKm)}/km",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(vehicles.size) { i ->
                val active = i == pagerState.currentPage
                val w by animateDpAsState(if (active) 18.dp else 6.dp, label = "dot")
                val c by animateColorAsState(if (active) colors.primary else colors.outlineVariant, label = "dotColor")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = w, height = 6.dp)
                        .clip(CircleShape)
                        .background(c),
                )
            }
        }

        Spacer(Modifier.height(26.dp))
        Text(
            "¿Necesitás ayudantes?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            "Para cargar y descargar. El chofer va incluido.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        val perHelper = vm.config.pricing.perHelper
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (n in 0..2) {
                val selected = vm.helpers == n
                ChoiceCard(selected = selected, onClick = { vm.selectHelpers(n) }, modifier = Modifier.weight(1f), padding = 10.dp) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), modifier = Modifier.height(28.dp)) {
                            repeat(maxOf(n, 1)) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = when {
                                        n == 0 -> colors.outline
                                        selected -> colors.primary
                                        else -> colors.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        Text(
                            when (n) { 0 -> "Sin"; 1 -> "1"; else -> "2" },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (selected) colors.primary else colors.onSurface,
                        )
                        Text(
                            if (n == 0) "Sin cargo" else "+ ${Money.formatArs(perHelper * n)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AnimatedContent(targetState = vm.helpers, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "hint") { n ->
            Text(
                when (n) {
                    0 -> "Vos cargás y descargás. Ideal para cajas y cosas livianas."
                    1 -> "Un ayudante para muebles medianos, electrodomésticos y cajas."
                    else -> "Dos ayudantes para mudanzas o cosas muy pesadas."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

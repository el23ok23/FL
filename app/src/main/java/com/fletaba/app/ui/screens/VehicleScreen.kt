package com.fletaba.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import com.fletaba.app.OrderViewModel
import com.fletaba.app.ui.components.ChoiceCard
import com.fletaba.app.ui.components.PriceBar
import com.fletaba.app.ui.components.StepScaffold
import com.fletaba.app.ui.components.VehicleIcon
import com.fletaba.core.LoadTime
import com.fletaba.core.Money
import com.fletaba.core.PriceCalculator
import kotlin.math.absoluteValue

@Composable
fun VehicleScreen(vm: OrderViewModel, step: Int, total: Int) {
    val vehicles = vm.config.vehicles
    val colors = MaterialTheme.colorScheme
    val pagerState = rememberPagerState(initialPage = vm.vehicleIndex) { vehicles.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { vm.selectVehicle(it) }
    }

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿Qué tamaño necesitás?",
        subtitle = "De la Kangoo al camión grande. Deslizá o tocá las flechas; el precio se actualiza solo.",
        onBack = { vm.back() },
        stepName = "Vehículo",
        centered = false,
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Continuar", onClick = { vm.next() })
        },
    ) {
        // Escala de tamaño: de chico a grande, con el vehículo elegido resaltado.
        SizeScale(count = vehicles.size, current = pagerState.currentPage, onPick = { scope.launch { pagerState.animateScrollToPage(it) } })
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 54.dp),
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
            val v = vehicles[page]
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val scale = lerp(1f, 0.9f, offset)
            val alpha = lerp(1f, 0.6f, offset)
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
                    .height(250.dp),
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.primaryContainer.copy(alpha = 0.7f), colors.primaryContainer.copy(alpha = 0.15f)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        VehicleIcon(kind = v.kind, modifier = Modifier.size(width = 180.dp, height = 98.dp))
                        Text(
                            "${page + 1} de ${vehicles.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                        )
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
                        Text(
                            "Cargarlo lleno: ~${LoadTime.format(v.loadMinutes)} por persona",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Desde ${Money.formatArs(PriceCalculator.gross(v.baseFare, vm.config.pricing))} + ${Money.formatArs(PriceCalculator.gross(v.perKm, vm.config.pricing))}/km",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
            }
            }
            // Flechas: dejan claro que hay más tamaños a los costados.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilledTonalIconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Más chico") }
                FilledTonalIconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = pagerState.currentPage < vehicles.lastIndex,
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Más grande") }
            }
        }

        Spacer(Modifier.height(10.dp))
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

        Spacer(Modifier.height(22.dp))
        Text(
            "¿Necesitás ayudantes?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        val helperCost = PriceCalculator.gross(PriceCalculator.helperCost(vm.distanceKm, vm.config.pricing), vm.config.pricing)
        Text(
            "Para cargar y descargar. El chofer va incluido. Cada ayudante: ${Money.formatArs(helperCost)} para este recorrido.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
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
                            if (n == 0) "Sin cargo" else "+ ${Money.formatArs(helperCost * n)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val includedMinutes = vm.loadTime.totalMinutes
        AnimatedContent(targetState = vm.helpers to includedMinutes, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "hint") { (n, minutes) ->
            Text(
                when (n) {
                    0 -> "Vos cargás y descargás con el chofer. Tiempo de carga y descarga incluido: hasta ${LoadTime.format(minutes)}."
                    1 -> "Un ayudante para muebles medianos, electrodomésticos y cajas. Tiempo incluido: hasta ${LoadTime.format(minutes)}."
                    else -> "Dos ayudantes para mudanzas o cosas muy pesadas. Tiempo incluido: hasta ${LoadTime.format(minutes)}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Barra de tamaños: ocho segmentos que crecen de chico a grande; el elegido se pinta. Tocar uno lo selecciona. */
@Composable
private fun SizeScale(count: Int, current: Int, onPick: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(count) { i ->
                val active = i <= current
                val h = 8.dp + (16.dp * i / (count - 1).coerceAtLeast(1))
                val c by animateColorAsState(if (active) colors.primary else colors.outlineVariant, label = "bar")
                Box(
                    Modifier
                        .weight(1f)
                        .height(h)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(c)
                        .clickable { onPick(i) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Chico", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text("Grande", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

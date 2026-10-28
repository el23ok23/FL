package com.mifleteciudad.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.PriceBar
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.app.ui.components.VehicleIcon
import com.mifleteciudad.core.Money
import kotlin.math.absoluteValue

@Composable
fun VehicleScreen(vm: OrderViewModel, step: Int, total: Int) {
    val vehicles = vm.config.vehicles
    val pagerState = rememberPagerState(initialPage = vm.vehicleIndex) { vehicles.size }

    // La tarjeta centrada es la elegida.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { vm.selectVehicle(it) }
    }

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿Qué vehículo necesitás?",
        subtitle = "Deslizá para ver las opciones.",
        onBack = { vm.back() },
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Continuar", onClick = { vm.next() })
        },
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val v = vehicles[page]
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val scale = lerp(1f, 0.9f, offset)
            val alpha = lerp(1f, 0.55f, offset)
            val selected = page == pagerState.currentPage
            val border by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "border",
            )
            val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "bw")

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(borderWidth, border),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        VehicleIcon(kind = v.kind, modifier = Modifier.size(width = 170.dp, height = 96.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(v.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(v.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    SpecRow(label = "Medidas", value = v.dimensions)
                    Spacer(Modifier.height(4.dp))
                    SpecRow(label = "Carga", value = v.capacity)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Desde ${Money.formatArs(v.baseFare)} + ${Money.formatArs(v.perKm)}/km",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(vehicles.size) { i ->
                val active = i == pagerState.currentPage
                val w by animateDpAsState(if (active) 18.dp else 6.dp, label = "dot")
                val c by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    label = "dotColor",
                )
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = w, height = 6.dp)
                        .clip(CircleShape)
                        .background(c),
                )
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

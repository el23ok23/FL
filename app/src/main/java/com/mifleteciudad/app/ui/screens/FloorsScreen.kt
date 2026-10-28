package com.mifleteciudad.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.PriceBar
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.app.ui.components.Stepper
import com.mifleteciudad.core.Access
import com.mifleteciudad.core.FloorInfo
import com.mifleteciudad.core.Money

@Composable
fun FloorsScreen(vm: OrderViewModel, step: Int, total: Int) {
    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿Hay pisos que subir?",
        subtitle = "Por escalera se cobra ${Money.formatArs(vm.config.pricing.perStairFloor)} por piso. Con ascensor no hay recargo.",
        onBack = { vm.back() },
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Continuar", onClick = { vm.next() })
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val perFloor = vm.config.pricing.perStairFloor
            FloorCard(title = "Origen", info = vm.originFloor, perFloor = perFloor, onChange = { vm.setOriginFloor(it) })
            FloorCard(title = "Destino", info = vm.destinationFloor, perFloor = perFloor, onChange = { vm.setDestinationFloor(it) })
        }
    }
}

@Composable
private fun FloorCard(title: String, info: FloorInfo, perFloor: Int, onChange: (FloorInfo) -> Unit) {
    val options = listOf(Access.GROUND to "Planta baja", Access.ELEVATOR to "Ascensor", Access.STAIRS to "Escalera")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (access, label) ->
                    SegmentedButton(
                        selected = info.access == access,
                        onClick = {
                            val floors = if (access == Access.GROUND) 0 else info.floors.coerceAtLeast(1)
                            onChange(FloorInfo(access, floors))
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        icon = {},
                    ) {
                        Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            AnimatedVisibility(
                visible = info.access != Access.GROUND,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("¿Qué piso?", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (info.access == Access.STAIRS) "Recargo: ${Money.formatArs(info.stairFloors * perFloor)}" else "Sin recargo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Stepper(
                            value = info.floors.coerceAtLeast(1),
                            onChange = { onChange(info.copy(floors = it)) },
                        )
                    }
                }
            }
        }
    }
}

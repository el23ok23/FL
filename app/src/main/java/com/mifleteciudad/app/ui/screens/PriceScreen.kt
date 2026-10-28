package com.mifleteciudad.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.AnimatedMoney
import com.mifleteciudad.app.ui.components.PrimaryButton
import com.mifleteciudad.app.ui.components.SecondaryButton
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.core.Money

@Composable
fun PriceScreen(vm: OrderViewModel, step: Int, total: Int) {
    val p = vm.price
    StepScaffold(
        step = step,
        totalSteps = total,
        title = "Tu precio aproximado",
        subtitle = "Lo confirmamos por WhatsApp. No se paga nada en la app.",
        onBack = { vm.back() },
        bottomBar = {
            PrimaryButton(text = "Me interesa, que me contacten", onClick = { vm.next() })
            SecondaryButton(text = "Cambiar algo", onClick = { vm.back() })
        },
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total estimado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                AnimatedMoney(
                    amount = p.total,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${vm.vehicle.name} · ${Money.formatKm(p.km)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                BreakdownRow("Tarifa base · ${vm.vehicle.name}", p.base)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BreakdownRow("Recorrido · ${Money.formatKm(p.km)}", p.distance)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BreakdownRow(
                    if (p.helperCount == 0) "Sin ayudantes" else "${p.helperCount} ayudante${if (p.helperCount > 1) "s" else ""}",
                    p.helpers,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BreakdownRow(
                    if (p.stairFloors == 0) "Sin pisos por escalera" else "${p.stairFloors} piso${if (p.stairFloors > 1) "s" else ""} por escalera",
                    p.stairs,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryLine("Origen", "${vm.origin?.label.orEmpty()} · ${vm.originFloor.describe()}")
            SummaryLine("Destino", "${vm.destination?.label.orEmpty()} · ${vm.destinationFloor.describe()}")
            if (vm.cargo.isNotBlank()) SummaryLine("Llevás", vm.cargo)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "El precio final puede variar según el volumen real, la espera y el acceso. Te lo confirmamos antes del flete.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            if (amount == 0) "—" else Money.formatArs(amount),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

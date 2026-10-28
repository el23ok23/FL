package com.mifleteciudad.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.AnimatedMoney
import com.mifleteciudad.app.ui.components.PrimaryButton
import com.mifleteciudad.app.ui.components.SecondaryButton
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.app.ui.components.VehicleIcon
import com.mifleteciudad.core.Money

@Composable
fun PriceScreen(vm: OrderViewModel, step: Int, total: Int) {
    val p = vm.price
    val colors = MaterialTheme.colorScheme
    StepScaffold(
        step = step,
        totalSteps = total,
        title = "Tu precio aproximado",
        subtitle = "Lo confirmamos por WhatsApp. No se paga nada en la app.",
        onBack = { vm.back() },
        centered = false,
        bottomBar = {
            PrimaryButton(text = "Me interesa, que me contacten", onClick = { vm.next() })
            SecondaryButton(text = "Cambiar algo", onClick = { vm.back() })
        },
    ) {
        // Tarjeta principal con degradé de la marca
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        listOf(colors.primary, colors.primary.copy(alpha = 0.78f)),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VehicleIcon(
                kind = vm.vehicle.kind,
                modifier = Modifier.width(120.dp).height(66.dp),
                body = Color.White,
                detail = colors.onPrimary.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            Text("Total estimado", style = MaterialTheme.typography.labelLarge, color = colors.onPrimary.copy(alpha = 0.85f))
            AnimatedMoney(
                amount = p.total,
                style = MaterialTheme.typography.displayMedium,
                color = colors.onPrimary,
            )
            Text(
                "${vm.vehicle.name} · ${Money.formatKm(p.km)}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                BreakdownRow("Tarifa base · ${vm.vehicle.name}", p.base)
                HorizontalDivider(color = colors.outlineVariant)
                BreakdownRow("Recorrido · ${Money.formatKm(p.km)}", p.distance)
                HorizontalDivider(color = colors.outlineVariant)
                BreakdownRow(
                    if (p.helperCount == 0) "Sin ayudantes" else "${p.helperCount} ayudante${if (p.helperCount > 1) "s" else ""}",
                    p.helpers,
                )
                HorizontalDivider(color = colors.outlineVariant)
                BreakdownRow(
                    if (p.stairFloors == 0) "Sin pisos por escalera" else "${p.stairFloors} piso${if (p.stairFloors > 1) "s" else ""} por escalera",
                    p.stairs,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = colors.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryLine("A", colors.primary, "${vm.origin?.label.orEmpty()} · ${vm.originFloor.describe()}")
                SummaryLine("B", colors.tertiary, "${vm.destination?.label.orEmpty()} · ${vm.destinationFloor.describe()}")
                SummaryLine("📅", null, vm.schedule.describe().replaceFirstChar { it.uppercase() })
                if (vm.cargo.isNotBlank()) SummaryLine("📦", null, vm.cargo)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "El precio final puede variar según el volumen real, la espera y el acceso. Te lo confirmamos antes del flete.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            if (amount == 0) "—" else Money.formatArs(amount),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SummaryLine(badge: String, badgeColor: Color?, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (badgeColor != null) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = badgeColor) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
        } else {
            Text(badge, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

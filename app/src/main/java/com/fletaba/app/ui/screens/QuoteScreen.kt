package com.fletaba.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fletaba.app.OrderViewModel
import com.fletaba.app.ui.components.AnimatedMoney
import com.fletaba.app.ui.components.PrimaryButton
import com.fletaba.app.ui.components.StepScaffold
import com.fletaba.app.ui.components.VehicleIcon
import com.fletaba.core.ArgentinePhone
import com.fletaba.core.LoadTime
import com.fletaba.core.Money
import com.fletaba.core.OrderMessage

/** Paso 4: precio con desglose y datos de contacto para enviar el pedido. */
@Composable
fun QuoteScreen(vm: OrderViewModel, step: Int, total: Int) {
    val p = vm.price
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val normalized = vm.phoneNormalized
    val phoneTouched = vm.customerPhone.filter(Char::isDigit).length >= 8
    val count = vm.stops.size

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "Tu precio aproximado",
        subtitle = "Lo confirmamos por WhatsApp. No se paga nada en la app.",
        onBack = { vm.back() },
        stepName = "Precio y contacto",
        centered = false,
        bottomBar = {
            PrimaryButton(
                text = "Enviar pedido",
                enabled = vm.canSend,
                loading = vm.sending,
                onClick = { focus.clearFocus(); vm.send() },
            )
        },
    ) {
        // Tarjeta principal con degradé de la marca
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(colors.primary, colors.primary.copy(alpha = 0.75f))))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VehicleIcon(
                kind = vm.vehicle.kind,
                modifier = Modifier.width(130.dp).height(72.dp),
                body = Color.White,
                detail = colors.onPrimary.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(8.dp))
            Text("Total estimado", style = MaterialTheme.typography.labelLarge, color = colors.onPrimary.copy(alpha = 0.85f))
            AnimatedMoney(amount = p.total, style = MaterialTheme.typography.displayMedium, color = colors.onPrimary)
            Text(
                "${vm.vehicle.name} · ${Money.formatKm(p.km)} · ${count} paradas",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                BreakdownRow("Tarifa base · ${vm.vehicle.name}", p.base)
                HorizontalDivider(color = colors.outlineVariant)
                BreakdownRow("Recorrido · ${Money.formatKm(p.km)}", p.distance)
                HorizontalDivider(color = colors.outlineVariant)
                BreakdownRow(
                    if (p.helperCount == 0) "Sin ayudantes" else "${p.helperCount} ayudante${if (p.helperCount > 1) "s" else ""} · ${Money.formatKm(p.km)}",
                    p.helpers,
                )
                HorizontalDivider(color = colors.outlineVariant)
                BreakdownRow(
                    if (p.stairFloors == 0) "Sin pisos por escalera" else "${p.stairFloors} piso${if (p.stairFloors > 1) "s" else ""} por escalera",
                    p.stairs,
                )
                if (p.extraStopCount > 0) {
                    HorizontalDivider(color = colors.outlineVariant)
                    BreakdownRow("${p.extraStopCount} parada${if (p.extraStopCount > 1) "s" else ""} adicional${if (p.extraStopCount > 1) "es" else ""}", p.extraStops)
                }
            }
        }

        // Tiempo incluido: útil para el cliente y para el conductor
        val lt = vm.loadTime
        val waitFee = vm.config.times.waitFeePer15Min
        Spacer(Modifier.height(14.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = colors.primaryContainer.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tiempo de carga y descarga incluido", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Con ${lt.people} persona${if (lt.people > 1) "s" else ""} trabajando" +
                                (vm.route?.durationMin?.let { " · viaje ~${LoadTime.format(it)}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Text("hasta ${LoadTime.format(lt.totalMinutes)}", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                }
                Spacer(Modifier.height(10.dp))
                lt.stops.forEach { st ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(stopColor(st.index, lt.stops.size)),
                            contentAlignment = Alignment.Center,
                        ) { Text(('A' + st.index).toString(), style = MaterialTheme.typography.labelSmall, color = Color.White) }
                        Spacer(Modifier.width(10.dp))
                        Text(st.detail.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = colors.onSurfaceVariant)
                        Text(LoadTime.format(st.minutes), style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (waitFee > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Si se excede, la espera se cobra ${Money.formatArs(waitFee)} cada 15 minutos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                vm.stops.forEachIndexed { i, s ->
                    SummaryLine(
                        badge = ('A' + i).toString(),
                        badgeColor = stopColor(i, count),
                        value = "${OrderMessage.stopTitle(i, count)}: ${s.point?.label.orEmpty()} · ${s.floor.describe()}",
                    )
                }
                SummaryLine("📅", null, vm.schedule.describe().replaceFirstChar { it.uppercase() })
                if (vm.cargo.isNotBlank()) SummaryLine("📦", null, vm.cargo)
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("¿Cómo te contactamos?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(
            "Te escribimos por WhatsApp para confirmar el precio y coordinar.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = vm.customerName,
            onValueChange = { vm.setName(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu nombre") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = vm.customerPhone,
            onValueChange = { vm.setPhone(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu WhatsApp") },
            placeholder = { Text("11 2345 6789") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            singleLine = true,
            isError = phoneTouched && normalized == null,
            shape = MaterialTheme.shapes.medium,
            supportingText = {
                Text(
                    when {
                        normalized != null -> "${ArgentinePhone.format(normalized)} ✓"
                        phoneTouched -> "Ingresá un celular argentino válido (código de área + número)."
                        else -> "Celular argentino con código de área. Sin 0 ni 15."
                    },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); if (vm.canSend) vm.send() }),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Usamos tu nombre y tu WhatsApp únicamente para contactarte por este pedido. No se guardan en la app ni se comparten.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
        Text(if (amount == 0) "—" else Money.formatArs(amount), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SummaryLine(badge: String, badgeColor: Color?, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (badgeColor != null) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        } else {
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                Text(badge, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

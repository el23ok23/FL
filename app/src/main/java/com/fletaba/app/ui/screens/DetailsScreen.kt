package com.fletaba.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fletaba.app.OrderViewModel
import com.fletaba.app.ui.components.ChoiceCard
import com.fletaba.app.ui.components.PriceBar
import com.fletaba.app.ui.components.StepScaffold
import com.fletaba.app.ui.components.Stepper
import com.fletaba.core.Access
import com.fletaba.core.CargoItem
import com.fletaba.core.CargoItems
import com.fletaba.core.DayDate
import com.fletaba.core.FloorInfo
import com.fletaba.core.LoadTime
import com.fletaba.core.Money
import com.fletaba.core.OrderMessage
import com.fletaba.core.PriceCalculator
import com.fletaba.core.TimeSlot


/** Paso 3: pisos de cada parada, qué lleva y cuándo. */
@Composable
fun DetailsScreen(vm: OrderViewModel, step: Int, total: Int) {
    val focus = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val perFloor = PriceCalculator.gross(vm.config.pricing.perStairFloor, vm.config.pricing)
    val today = remember { DayDate.today() }
    val days = remember(today) { DayDate.upcoming(14, today) }
    val schedule = vm.schedule
    val count = vm.stops.size

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "Detalles del flete",
        subtitle = "Pisos en cada parada, qué llevás y cuándo lo necesitás.",
        onBack = { vm.back() },
        stepName = "Detalles",
        centered = false,
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Ver precio", onClick = { focus.clearFocus(); vm.next() })
        },
    ) {
        // ---- Pisos por parada ----
        SectionTitle("Pisos", "Por escalera se cobran ${Money.formatArs(perFloor)} por piso y suma tiempo de carga. Con ascensor no hay recargo.")
        val loadTime = vm.loadTime
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            vm.stops.forEachIndexed { index, draft ->
                FloorCard(
                    letter = ('A' + index).toString(),
                    title = OrderMessage.stopTitle(index, count),
                    address = draft.point?.label ?: draft.query,
                    color = stopColor(index, count),
                    info = draft.floor,
                    perFloor = perFloor,
                    includedMinutes = loadTime.stops.getOrNull(index)?.minutes ?: 0,
                    onChange = { vm.updateFloor(index, it) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Tiempo de carga y descarga incluido: hasta ${LoadTime.format(loadTime.totalMinutes)} con ${loadTime.people} persona${if (loadTime.people > 1) "s" else ""}.",
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        // ---- Qué lleva ----
        SectionTitle("¿Qué vas a llevar?", "Tocá + en cada cosa. Con eso estimamos el volumen y te decimos si entra en el vehículo.")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CargoItems.all.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { item ->
                        CargoRow(
                            item = item,
                            count = vm.cargoCounts[item.id] ?: 0,
                            onChange = { vm.changeCargo(item.id, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = vm.cargoNote,
            onValueChange = { vm.setCargoText(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Otras cosas o aclaraciones (opcional)") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            supportingText = { Text("${vm.cargoNote.length}/${OrderViewModel.CARGO_MAX}") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        )
        val volume = vm.cargoVolume
        if (volume > 0) {
            val fits = vm.vehicle.volumeM3 >= volume * 1.15
            val suggested = CargoItems.smallestThatFits(volume, vm.config.vehicles)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (fits) colors.primaryContainer.copy(alpha = 0.45f) else colors.tertiaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        if (fits) "Volumen estimado: ${CargoItems.formatM3(volume)} · entra en ${vm.vehicle.name} (${CargoItems.formatM3(vm.vehicle.volumeM3)})"
                        else "Volumen estimado: ${CargoItems.formatM3(volume)} · no entra en ${vm.vehicle.name} (${CargoItems.formatM3(vm.vehicle.volumeM3)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (fits) colors.onPrimaryContainer else colors.onTertiaryContainer,
                    )
                    if (!fits) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (suggested != null) "Te conviene ${suggested.name} (${CargoItems.formatM3(suggested.volumeM3)})." else "Supera el vehículo más grande: conviene dividir en dos viajes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onTertiaryContainer,
                        )
                        if (suggested != null) {
                            androidx.compose.material3.TextButton(onClick = {
                                vm.selectVehicle(vm.config.vehicles.indexOf(suggested))
                            }) { Text("Cambiar a ${suggested.name}") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---- Cuándo ----
        SectionTitle("¿Cuándo lo necesitás?", "Elegí el día y la franja. Lo confirmamos por WhatsApp.")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                ChoiceCard(
                    selected = schedule.date == null,
                    onClick = { vm.updateSchedule(schedule.copy(date = null)) },
                    modifier = Modifier.width(96.dp),
                    padding = 8.dp,
                ) {
                    Column(Modifier.fillMaxWidth().height(62.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Lo antes", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Text("posible", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    }
                }
            }
            items(days, key = { "${it.year}-${it.month}-${it.day}" }) { d ->
                val selected = schedule.date == d
                ChoiceCard(
                    selected = selected,
                    onClick = { vm.updateSchedule(schedule.copy(date = d)) },
                    modifier = Modifier.width(72.dp),
                    padding = 8.dp,
                ) {
                    Column(Modifier.fillMaxWidth().height(62.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            when (d) { today -> "hoy"; today.plusDays(1) -> "mañ."; else -> d.dayName },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                        Text(
                            d.day.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (selected) colors.primary else colors.onSurface,
                        )
                        Text(d.monthName.take(3), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeSlot.entries.forEach { slot ->
                val selected = schedule.slot == slot
                ChoiceCard(
                    selected = selected,
                    onClick = { vm.updateSchedule(schedule.copy(slot = slot)) },
                    modifier = Modifier.weight(1f),
                    padding = 12.dp,
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            slot.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) colors.primary else colors.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(slot.hours, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            schedule.describe(today).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun FloorCard(
    letter: String,
    title: String,
    address: String,
    color: Color,
    info: FloorInfo,
    perFloor: Int,
    includedMinutes: Int,
    onChange: (FloorInfo) -> Unit,
) {
    val options = listOf(Access.GROUND to "Planta baja", Access.ELEVATOR to "Ascensor", Access.STAIRS to "Escalera")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
                    Text(
                        "⏱ ${LoadTime.format(includedMinutes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
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
                        Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
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
                        Stepper(value = info.floors.coerceAtLeast(1), onChange = { onChange(info.copy(floors = it)) })
                    }
                }
            }
        }
    }
}

/** Una cosa para llevar con su contador − / +. */
@Composable
private fun CargoRow(item: CargoItem, count: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val active = count > 0
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (active) colors.primaryContainer.copy(alpha = 0.45f) else colors.surface,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) colors.primary else colors.outlineVariant),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(6.dp))
            Text(item.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (active) {
                androidx.compose.material3.IconButton(onClick = { onChange(-1) }, modifier = Modifier.size(28.dp)) {
                    Text("−", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                }
                Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = colors.primary)
            }
            androidx.compose.material3.IconButton(onClick = { onChange(+1) }, modifier = Modifier.size(28.dp)) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = colors.primary)
            }
        }
    }
}

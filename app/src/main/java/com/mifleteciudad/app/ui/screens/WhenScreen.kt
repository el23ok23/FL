package com.mifleteciudad.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.ChoiceCard
import com.mifleteciudad.app.ui.components.PriceBar
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.core.DayDate
import com.mifleteciudad.core.TimeSlot

/** Paso: día y franja horaria del flete. */
@Composable
fun WhenScreen(vm: OrderViewModel, step: Int, total: Int) {
    val today = remember { DayDate.today() }
    val days = remember(today) { DayDate.upcoming(14, today) }
    val schedule = vm.schedule
    val listState = rememberLazyListState()

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿Cuándo lo necesitás?",
        subtitle = "Elegí el día y la franja. Lo confirmamos por WhatsApp.",
        onBack = { vm.back() },
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Ver precio", onClick = { vm.next() })
        },
    ) {
        SectionLabel("Día")
        Spacer(Modifier.height(10.dp))
        LazyRow(
            state = listState,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            d.day.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            d.monthName.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Franja horaria")
        Spacer(Modifier.height(10.dp))
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
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            slot.hours,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            schedule.describe(today).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

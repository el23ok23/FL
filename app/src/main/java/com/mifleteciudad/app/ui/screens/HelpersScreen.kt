package com.mifleteciudad.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HelperOption(
                count = 0,
                title = "Sin ayudantes",
                detail = "Vos cargás y descargás.",
                extra = "Sin cargo",
                selected = vm.helpers == 0,
                onClick = { vm.selectHelpers(0) },
            )
            HelperOption(
                count = 1,
                title = "1 ayudante",
                detail = "Ideal para muebles medianos y cajas.",
                extra = "+ ${Money.formatArs(perHelper)}",
                selected = vm.helpers == 1,
                onClick = { vm.selectHelpers(1) },
            )
            HelperOption(
                count = 2,
                title = "2 ayudantes",
                detail = "Para mudanzas o cosas pesadas.",
                extra = "+ ${Money.formatArs(perHelper * 2)}",
                selected = vm.helpers == 2,
                onClick = { vm.selectHelpers(2) },
            )
        }
    }
}

@Composable
private fun HelperOption(
    count: Int,
    title: String,
    detail: String,
    extra: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ChoiceCard(selected = selected, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.width(56.dp)) {
                if (count == 0) {
                    Icon(
                        Icons.Filled.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(28.dp),
                    )
                } else {
                    repeat(count) {
                        Icon(
                            Icons.Filled.Person, contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                extra,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

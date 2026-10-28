package com.mifleteciudad.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.PriceBar
import com.mifleteciudad.app.ui.components.StepScaffold

private val QUICK = listOf("Heladera", "Lavarropas", "Cajas", "Sommier y colchón", "Mudanza chica", "Muebles")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CargoScreen(vm: OrderViewModel, step: Int, total: Int) {
    val focus = LocalFocusManager.current
    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿Qué vas a llevar?",
        subtitle = "Opcional. Un detalle corto nos ayuda a mandarte el flete justo.",
        onBack = { vm.back() },
        bottomBar = {
            PriceBar(total = vm.price.total, buttonText = "Ver precio", onClick = { focus.clearFocus(); vm.next() })
        },
    ) {
        OutlinedTextField(
            value = vm.cargo,
            onValueChange = { vm.setCargoText(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Qué llevás") },
            placeholder = { Text("Ej.: heladera y 10 cajas") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            supportingText = { Text("${vm.cargo.length}/${OrderViewModel.CARGO_MAX}") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            QUICK.forEach { item ->
                SuggestionChip(
                    onClick = {
                        val current = vm.cargo.trim()
                        val next = when {
                            current.isEmpty() -> item
                            current.contains(item, ignoreCase = true) -> current
                            else -> "$current, ${item.lowercase()}"
                        }
                        vm.setCargoText(next)
                    },
                    label = { Text(item) },
                    shape = MaterialTheme.shapes.small,
                )
            }
        }
    }
}

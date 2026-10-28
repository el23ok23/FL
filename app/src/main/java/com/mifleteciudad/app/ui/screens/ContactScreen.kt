package com.mifleteciudad.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.ui.components.PrimaryButton
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.core.ArgentinePhone
import com.mifleteciudad.core.Money

@Composable
fun ContactScreen(vm: OrderViewModel, step: Int, total: Int) {
    val focus = LocalFocusManager.current
    val normalized = vm.phoneNormalized
    val phoneTouched = vm.customerPhone.filter(Char::isDigit).length >= 8

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿Cómo te contactamos?",
        subtitle = "Te escribimos por WhatsApp para confirmar el flete de ${Money.formatArs(vm.price.total)}.",
        onBack = { vm.back() },
        bottomBar = {
            PrimaryButton(
                text = "Enviar pedido",
                enabled = vm.canSend,
                loading = vm.sending,
                onClick = { focus.clearFocus(); vm.send() },
            )
        },
    ) {
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
        Spacer(Modifier.height(12.dp))
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
                        else -> "Celular argentino, con código de área. Sin 0 ni 15."
                    },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); if (vm.canSend) vm.send() }),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Usamos tu nombre y tu WhatsApp únicamente para contactarte por este pedido. No se guardan en la app ni se comparten con nadie más.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

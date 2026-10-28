package com.mifleteciudad.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mifleteciudad.app.Field
import com.mifleteciudad.app.OrderViewModel
import com.mifleteciudad.app.data.AddressSuggestion
import com.mifleteciudad.app.ui.components.MiniMap
import com.mifleteciudad.app.ui.components.PrimaryButton
import com.mifleteciudad.app.ui.components.StepScaffold
import com.mifleteciudad.core.AddressPoint
import com.mifleteciudad.core.Money

@Composable
fun RouteScreen(vm: OrderViewModel, step: Int, total: Int) {
    val focus = LocalFocusManager.current

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿De dónde a dónde?",
        subtitle = "Solo dentro de la Ciudad de Buenos Aires.",
        bottomBar = {
            PrimaryButton(
                text = "Continuar",
                enabled = vm.canContinueRoute,
                onClick = { focus.clearFocus(); vm.next() },
            )
        },
    ) {
        AddressField(
            label = "Origen",
            value = vm.originQuery,
            point = vm.origin,
            dotColor = MaterialTheme.colorScheme.primary,
            onValueChange = { vm.onQueryChange(Field.ORIGIN, it) },
            onFocus = { vm.focusField(Field.ORIGIN) },
            onClear = { vm.clearField(Field.ORIGIN) },
            imeAction = ImeAction.Next,
        )
        SuggestionsPanel(
            visible = vm.activeField == Field.ORIGIN && (vm.suggestions.isNotEmpty() || vm.searching || vm.noResults),
            suggestions = vm.suggestions,
            searching = vm.searching,
            onPick = { vm.selectSuggestion(it) },
        )

        Spacer(Modifier.height(12.dp))

        AddressField(
            label = "Destino",
            value = vm.destinationQuery,
            point = vm.destination,
            dotColor = MaterialTheme.colorScheme.tertiary,
            onValueChange = { vm.onQueryChange(Field.DESTINATION, it) },
            onFocus = { vm.focusField(Field.DESTINATION) },
            onClear = { vm.clearField(Field.DESTINATION) },
            imeAction = ImeAction.Done,
            onDone = { focus.clearFocus() },
        )
        SuggestionsPanel(
            visible = vm.activeField == Field.DESTINATION && (vm.suggestions.isNotEmpty() || vm.searching || vm.noResults),
            suggestions = vm.suggestions,
            searching = vm.searching,
            onPick = { vm.selectSuggestion(it) },
        )

        AnimatedVisibility(visible = vm.outOfZone, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Esa dirección está fuera de la zona. Por ahora solo hacemos fletes dentro de la Ciudad de Buenos Aires.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                MiniMap(
                    origin = vm.origin?.location,
                    destination = vm.destination?.location,
                    path = vm.route?.path.orEmpty(),
                    originInside = vm.origin?.insideCaba != false,
                    destinationInside = vm.destination?.insideCaba != false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(12.dp),
                )
                val km = vm.distanceKm
                val info = when {
                    vm.origin == null || vm.destination == null -> "Elegí origen y destino para ver el recorrido."
                    !vm.bothInside -> "Recorrido no disponible fuera de la Ciudad."
                    vm.route != null -> buildString {
                        append(Money.formatKm(km))
                        vm.route?.durationMin?.let { append(" · ~$it min") }
                    }
                    else -> "≈ ${Money.formatKm(km)} (estimado)"
                }
                Text(
                    info,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun AddressField(
    label: String,
    value: String,
    point: AddressPoint?,
    dotColor: Color,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit,
    onClear: () -> Unit,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
) {
    val isError = point?.insideCaba == false
    val support: (@Composable () -> Unit)? = when {
        isError -> { { Text("Fuera de la Ciudad de Buenos Aires") } }
        point != null -> { { Text("Dentro de la Ciudad ✓") } }
        else -> null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() },
        label = { Text(label) },
        placeholder = { Text("Calle y altura") },
        singleLine = true,
        isError = isError,
        shape = MaterialTheme.shapes.medium,
        leadingIcon = {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isError) MaterialTheme.colorScheme.error else dotColor),
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar")
                }
            }
        },
        supportingText = support,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
private fun SuggestionsPanel(
    visible: Boolean,
    suggestions: List<AddressSuggestion>,
    searching: Boolean,
    onPick: (AddressSuggestion) -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        ) {
            Column {
                if (searching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )
                }
                suggestions.take(5).forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(s) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(s.primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (s.secondary.isNotBlank()) {
                                Text(
                                    s.secondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                if (!searching && suggestions.isEmpty()) {
                    Text(
                        "Sin resultados. Probá con calle y altura.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }
}

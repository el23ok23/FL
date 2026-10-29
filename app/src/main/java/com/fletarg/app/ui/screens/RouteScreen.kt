package com.fletarg.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fletarg.app.OrderViewModel
import com.fletarg.app.data.AddressSuggestion
import com.fletarg.app.ui.components.MiniMap
import com.fletarg.app.ui.components.PrimaryButton
import com.fletarg.app.ui.components.StepScaffold
import com.fletarg.core.Money
import com.fletarg.core.OrderMessage
import com.fletarg.core.OrderSummary

/** Color de cada parada: origen azul, destino naranja, intermedias gris oscuro. */
@Composable
fun stopColor(index: Int, count: Int): Color = when (index) {
    0 -> MaterialTheme.colorScheme.primary
    count - 1 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.secondary
}

@Composable
fun RouteScreen(vm: OrderViewModel, step: Int, total: Int) {
    val focus = LocalFocusManager.current
    val stops = vm.stops
    val count = stops.size
    val requesters = remember { mutableMapOf<Long, FocusRequester>() }

    // Cuando se confirma una dirección, el foco salta solo al siguiente campo vacío.
    LaunchedEffect(vm.focusRequestId) {
        if (vm.focusRequestId == 0) return@LaunchedEffect
        val target = stops.getOrNull(vm.activeStop)
        if (target == null || target.point != null) focus.clearFocus()
        else runCatching { requesters[target.id]?.requestFocus() }
    }

    StepScaffold(
        step = step,
        totalSteps = total,
        title = "¿A dónde va el flete?",
        subtitle = "Salimos desde Capital Federal a cualquier punto del país. Podés sumar hasta ${OrderSummary.MAX_STOPS - 2} paradas en el medio.",
        centered = false,
        bottomBar = {
            PrimaryButton(
                text = "Continuar",
                enabled = vm.canContinueRoute,
                onClick = { focus.clearFocus(); vm.next() },
            )
        },
    ) {
        Column(Modifier.animateContentSize()) {
            stops.forEachIndexed { index, draft ->
                val isLast = index == count - 1
                val originProblem = index == 0 && draft.point?.let { it.insideCoverage && !it.insideCaba } == true
                StopField(
                    letter = ('A' + index).toString(),
                    label = if (index == 0) "Origen (Capital Federal)" else OrderMessage.stopTitle(index, count),
                    value = draft.query,
                    inside = draft.point?.insideCoverage,
                    originProblem = originProblem,
                    focusRequester = requesters.getOrPut(draft.id) { FocusRequester() },
                    color = stopColor(index, count),
                    removable = index != 0 && !isLast,
                    onValueChange = { vm.onQueryChange(index, it) },
                    onFocus = { vm.focusStop(index) },
                    onClear = { vm.clearStop(index) },
                    onRemove = { vm.removeStop(index) },
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                    onSubmit = { vm.submitActive() },
                )
                SuggestionsPanel(
                    visible = vm.activeStop == index && (vm.suggestions.isNotEmpty() || vm.searching || vm.noResults),
                    suggestions = vm.suggestions,
                    searching = vm.searching,
                    onPick = { vm.selectSuggestion(it) },
                )
                if (!isLast) Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(visible = vm.canAddStop) {
            OutlinedButton(
                onClick = { vm.addStop() },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Agregar una parada", style = MaterialTheme.typography.labelLarge)
            }
        }

        AnimatedVisibility(
            visible = vm.outOfCoverage || vm.originOutsideCaba,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (vm.outOfCoverage) "Esa dirección queda fuera de la Argentina. Por ahora solo hacemos fletes dentro del país."
                            else "El origen tiene que estar en Capital Federal. Las paradas y el destino pueden ser en cualquier lugar del país.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                MiniMap(
                    points = vm.resolvedPoints.map { it?.location },
                    insideFlags = vm.resolvedPoints.mapIndexed { i, p -> p?.insideCoverage != false && !(i == 0 && p != null && !p.insideCaba) },
                    path = vm.route?.path.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.large)
                        .padding(10.dp),
                )
                val km = vm.distanceKm
                val resolvedCount = vm.resolvedPoints.count { it != null }
                val info = when {
                    resolvedCount < 2 -> "Elegí las direcciones para ver el recorrido."
                    vm.outOfCoverage -> "Recorrido no disponible fuera del país."
                    vm.originOutsideCaba -> "El origen tiene que estar en Capital Federal."
                    vm.route != null -> buildString {
                        append("${Money.formatKm(km)} en total")
                        vm.route?.durationMin?.let { append(" · ~${formatMinutes(it)}") }
                    }
                    else -> "≈ ${Money.formatKm(km)} en total (estimado)"
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

private fun formatMinutes(min: Int): String =
    if (min < 60) "$min min" else "${min / 60} h ${(min % 60).toString().padStart(2, '0')}"

@Composable
private fun StopField(
    letter: String,
    label: String,
    value: String,
    inside: Boolean?,
    originProblem: Boolean,
    focusRequester: FocusRequester,
    color: Color,
    removable: Boolean,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit,
    onClear: () -> Unit,
    onRemove: () -> Unit,
    imeAction: ImeAction,
    onSubmit: () -> Unit,
) {
    val isError = inside == false || originProblem
    val support: (@Composable () -> Unit)? = when {
        inside == false -> { { Text("Fuera de la Argentina") } }
        originProblem -> { { Text("Tiene que ser en Capital Federal") } }
        inside == true -> { { Text("Dirección confirmada ✓") } }
        else -> null
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocus() },
            label = { Text(label) },
            placeholder = { Text(if (letter == "A") "Calle y altura, CABA" else "Calle, altura y localidad") },
            singleLine = true,
            isError = isError,
            shape = MaterialTheme.shapes.medium,
            leadingIcon = {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isError) MaterialTheme.colorScheme.error else color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) { Icon(Icons.Filled.Close, contentDescription = "Borrar") }
                }
            },
            supportingText = support,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }, onNext = { onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        if (removable) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Quitar parada", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
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
            shadowElevation = 6.dp,
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
                            tint = MaterialTheme.colorScheme.primary,
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
                        "Sin resultados. Probá con calle, altura y localidad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }
}

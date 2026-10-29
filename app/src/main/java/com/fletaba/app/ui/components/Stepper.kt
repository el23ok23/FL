package com.fletaba.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Control − / + para elegir un número (pisos). */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 1,
    max: Int = 40,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = value > min,
            modifier = Modifier.size(40.dp),
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        SlidingNumber(value, Modifier.width(52.dp))
        FilledTonalIconButton(
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = value < max,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Más")
        }
    }
}

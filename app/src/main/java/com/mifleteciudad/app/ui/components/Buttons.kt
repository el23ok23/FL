package com.mifleteciudad.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mifleteciudad.core.Money

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth().height(48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Monto que "cuenta" suavemente hasta el nuevo valor. */
@Composable
fun AnimatedMoney(
    amount: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val animated by animateIntAsState(
        targetValue = amount,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "money",
    )
    Text(Money.formatArs(animated), style = style, color = color, modifier = modifier, maxLines = 1)
}

/** Barra inferior con el precio estimado en vivo y el botón para seguir. */
@Composable
fun PriceBar(
    total: Int,
    buttonText: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    label: String = "Precio estimado",
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedMoney(total, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.width(16.dp))
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(buttonText, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Número que cambia con un deslizamiento corto (para contadores). */
@Composable
fun SlidingNumber(value: Int, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically(tween(180)) { if (up) it else -it } + fadeIn(tween(180))) togetherWith
                (slideOutVertically(tween(180)) { if (up) -it else it } + fadeOut(tween(120)))
        },
        label = "number",
    ) { n ->
        Box(contentAlignment = Alignment.Center) {
            Text(n.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

package com.fletaba.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tarjeta seleccionable: borde y fondo animados, y un pequeño rebote al elegirla. */
@Composable
fun ChoiceCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val border by animateColorAsState(
        if (selected) colors.primary else colors.outlineVariant,
        animationSpec = tween(220),
        label = "border",
    )
    val container by animateColorAsState(
        if (selected) colors.primaryContainer.copy(alpha = 0.45f) else colors.surface,
        animationSpec = tween(220),
        label = "container",
    )
    val width by animateDpAsState(if (selected) 2.dp else 1.dp, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "width")
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale",
    )
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pressScale(interaction, 0.97f),
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(width, border),
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

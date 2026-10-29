package com.fletarg.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fletarg.app.OrderViewModel
import com.fletarg.app.data.OrderSender
import com.fletarg.app.ui.components.AnimatedCheck
import com.fletarg.app.ui.components.Confetti
import com.fletarg.app.ui.components.PrimaryButton
import com.fletarg.core.Money
import kotlinx.coroutines.delay

@Composable
fun DoneScreen(vm: OrderViewModel) {
    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); textVisible = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Confetti(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            AnimatedCheck(Modifier.size(140.dp))
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(
                visible = textVisible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¡Listo!", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Te contactamos en breve",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (vm.sendResult) {
                            OrderSender.Result.OPENED_WHATSAPP ->
                                "Se abrió WhatsApp con tu pedido armado: tocá enviar y te respondemos enseguida."
                            OrderSender.Result.NOT_CONFIGURED ->
                                "Modo de prueba: todavía no hay un número de WhatsApp configurado para recibir pedidos."
                            else -> "Recibimos tu pedido. Te escribimos por WhatsApp para confirmar el precio y coordinar el horario."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${vm.vehicle.name} · ${Money.formatArs(vm.price.total)} · ${vm.schedule.describe()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "Hacer otro pedido", onClick = { vm.reset() })
            Spacer(Modifier.height(16.dp))
        }
    }
}

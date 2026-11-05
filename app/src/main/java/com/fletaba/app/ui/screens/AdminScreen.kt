package com.fletaba.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fletaba.app.OrderViewModel
import com.fletaba.app.data.AppConfig
import com.fletaba.app.ui.components.PrimaryButton
import com.fletaba.app.ui.components.SecondaryButton
import com.fletaba.app.ui.components.VehicleIcon
import com.fletaba.core.PriceSheet
import com.fletaba.core.VehicleKind

/**
 * Panel del dueño: se abre manteniendo apretado el título de la primera pantalla.
 * Pide el PIN y deja cambiar precios, nombre, WhatsApp, colores y logo. Con planilla configurada,
 * lo que guarda rige en todos los teléfonos; sin planilla, solo en este.
 */
@Composable
fun AdminScreen(vm: OrderViewModel) {
    if (vm.adminUnlocked) OwnerPanel(vm) else PinGate(vm)
}

@Composable
private fun PinGate(vm: OrderViewModel) {
    var pin by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(38.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("Panel del dueño", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "Acá cambiás precios, nombre, colores y logo de tu app.",
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PIN") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            isError = vm.adminMessage == "PIN incorrecto",
            supportingText = { vm.adminMessage?.let { Text(it, color = colors.error) } },
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(text = "Entrar", onClick = { vm.unlockAdmin(pin) }, enabled = pin.length >= 4, loading = vm.adminBusy)
        Spacer(Modifier.height(6.dp))
        SecondaryButton(text = "Volver", onClick = { vm.closeAdmin() })
    }
}

@Composable
private fun OwnerPanel(vm: OrderViewModel) {
    val colors = MaterialTheme.colorScheme
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.pickLogo(it) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Encabezado
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.closeAdmin() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.onSurface) }
            Column(Modifier.weight(1f)) {
                Text("Tu app", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.onBackground)
                Text(
                    if (vm.adminSynced) "Lo que guardes rige en todos los teléfonos" else "Modo local: los cambios valen solo en este teléfono",
                    style = MaterialTheme.typography.labelSmall, color = if (vm.adminSynced) colors.primary else colors.tertiary,
                )
            }
            IconButton(onClick = { vm.lockAdmin() }) { Icon(Icons.Filled.Lock, contentDescription = "Cerrar sesión", tint = colors.onSurfaceVariant) }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            LogoCard(vm, onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            ColorsCard(vm)
            vm.adminForm.forEach { group -> GroupCard(vm, group) }
            Spacer(Modifier.height(4.dp))
            SecondaryButton(text = "Volver a los valores originales", onClick = { vm.resetAdminValues() })
            Spacer(Modifier.height(8.dp))
        }

        // Barra inferior
        Surface(color = colors.surface, tonalElevation = 3.dp, shadowElevation = 10.dp) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                val msg = vm.adminMessage
                if (msg != null) {
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.startsWith("Guardado") || msg.startsWith("Logo listo")) colors.primary else colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                } else if (!vm.adminValid) {
                    Text("Revisá los valores marcados en rojo.", style = MaterialTheme.typography.bodySmall, color = colors.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
                PrimaryButton(text = "Guardar", onClick = { vm.saveAdmin() }, enabled = vm.adminValid, loading = vm.adminBusy)
            }
        }
    }
}

@Composable
private fun PanelCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = colors.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun LogoCard(vm: OrderViewModel, onPick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hasLogo = (vm.adminValues[PriceSheet.KEY_LOGO] ?: "").isNotBlank()
    val bitmap = vm.adminLogoPreview ?: vm.logo?.takeIf { hasLogo }
    PanelCard("Logo", "Se muestra al abrir la app. Elegí una imagen cuadrada de tu galería.") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), contentDescription = "Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    VehicleIcon(kind = VehicleKind.FURGON_MEDIANO, modifier = Modifier.size(width = 66.dp, height = 38.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                SecondaryButton(text = if (hasLogo) "Cambiar logo" else "Elegir de la galería", onClick = onPick)
                if (hasLogo) SecondaryButton(text = "Usar el de fábrica", onClick = { vm.removeLogo() })
            }
        }
    }
}

private val PRIMARY_PALETTE = listOf("#1B7BE0", "#0F5FB8", "#0B1F3A", "#0E7C4A", "#0891B2", "#D97706", "#DC2626", "#BE185D", "#7C3AED", "#374151")
private val ACCENT_PALETTE = listOf("#F6B40E", "#F97316", "#22C55E", "#38BDF8", "#F43F5E", "#A78BFA", "#FACC15", "#E2E8F0")

@Composable
private fun ColorsCard(vm: OrderViewModel) {
    PanelCard("Colores", "El principal pinta botones y títulos; el de acento, los detalles.") {
        Text("Principal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Swatches(vm, PriceSheet.KEY_PRIMARY, PRIMARY_PALETTE, vm.config.brand.primary)
        Spacer(Modifier.height(12.dp))
        Text("Acento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Swatches(vm, PriceSheet.KEY_ACCENT, ACCENT_PALETTE, vm.config.brand.accent)
    }
}

@Composable
private fun Swatches(vm: OrderViewModel, key: String, palette: List<String>, factory: Long) {
    val current = PriceSheet.hex(vm.adminValues[key] ?: "")
    val factoryHex = "#%06X".format(factory and 0xFFFFFF)
    // El de fábrica va primero; si ya está en la paleta no se repite.
    val options = listOf(factoryHex) + palette.filter { !it.equals(factoryHex, ignoreCase = true) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        options.take(6).forEach { hex -> Swatch(hex, selected = (current ?: factoryHex).equals(hex, ignoreCase = true)) { vm.setAdminValue(key, if (hex.equals(factoryHex, true)) "" else hex) } }
    }
    if (options.size > 6) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            options.drop(6).forEach { hex -> Swatch(hex, selected = (current ?: factoryHex).equals(hex, ignoreCase = true)) { vm.setAdminValue(key, hex) } }
        }
    }
}

@Composable
private fun Swatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = Color(AppConfig.parseColor(hex, 0xFF1B7BE0))
    val ring = MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, if (selected) ring else ring.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            val dark = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) < 0.6
            Icon(Icons.Filled.Check, contentDescription = null, tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun GroupCard(vm: OrderViewModel, group: PriceSheet.Group) {
    PanelCard(group.title, group.subtitle) {
        val twoColumns = group.fields.size == 2 && group.fields.none { it.text }
        if (twoColumns) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                group.fields.forEach { f -> ValueField(vm, f, Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                group.fields.forEach { f -> ValueField(vm, f, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun ValueField(vm: OrderViewModel, field: PriceSheet.Field, modifier: Modifier) {
    val value = vm.adminValues[field.key] ?: ""
    val valid = PriceSheet.isValid(field.key, value)
    OutlinedTextField(
        value = value,
        onValueChange = { vm.setAdminValue(field.key, it) },
        modifier = modifier,
        label = { Text(field.label) },
        singleLine = true,
        isError = !valid,
        shape = MaterialTheme.shapes.medium,
        prefix = if (field.unit == "$") ({ Text("$ ") }) else null,
        suffix = if (field.unit.isNotEmpty() && field.unit != "$") ({ Text(field.unit) }) else null,
        placeholder = when (field.key) {
            PriceSheet.KEY_WHATSAPP -> ({ Text("549 + área + número") })
            PriceSheet.KEY_NAME -> ({ Text(vm.config.businessName) })
            else -> null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                field.key == PriceSheet.KEY_WHATSAPP -> KeyboardType.Phone
                field.text -> KeyboardType.Text
                field.decimal -> KeyboardType.Decimal
                else -> KeyboardType.Number
            },
            imeAction = ImeAction.Next,
        ),
    )
}

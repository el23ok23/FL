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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fletaba.app.OrderViewModel
import com.fletaba.app.data.AppConfig
import com.fletaba.app.ui.components.PrimaryButton
import com.fletaba.app.ui.components.SecondaryButton
import com.fletaba.app.ui.components.VehicleIcon
import androidx.compose.foundation.horizontalScroll
import com.fletaba.core.OrderStatus
import com.fletaba.core.Money
import androidx.compose.material3.Switch
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
    var show by rememberSaveable { mutableStateOf(false) }
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
                .background(colors.primary),
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
            onValueChange = { if (it.length <= 32) pin = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PIN") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "Ocultar" else "Ver") } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (pin.length >= 4) vm.unlockAdmin(pin) }),
            isError = vm.adminMessage == "PIN incorrecto",
            supportingText = { vm.adminMessage?.let { Text(it, color = if (it.startsWith("Te mandamos") || it.startsWith("Se abrió")) colors.primary else colors.error) } },
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(text = "Entrar", onClick = { vm.unlockAdmin(pin) }, enabled = pin.length >= 4, loading = vm.adminBusy)
        Spacer(Modifier.height(6.dp))
        SecondaryButton(text = "Olvidé mi PIN", onClick = { vm.recoverPin() })
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

        // Pestañas
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Ajustes", "Pedidos").forEachIndexed { i, title ->
                val on = vm.adminTab == i
                Surface(
                    shape = CircleShape,
                    color = if (on) colors.primary else colors.surfaceVariant,
                    modifier = Modifier.weight(1f).clickable { vm.selectAdminTab(i) },
                ) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = if (on) colors.onPrimary else colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            if (vm.adminTab == 1) {
                OrdersList(vm)
            } else {
                LogoCard(vm, onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                ColorsCard(vm)
                vm.adminForm.forEach { group -> GroupCard(vm, group) }
                SecondaryButton(text = "+ Agregar un vehículo", onClick = { vm.addVehicleSlot() })
                SecurityCard(vm)
                Spacer(Modifier.height(4.dp))
                SecondaryButton(text = "Volver a los valores originales", onClick = { vm.resetAdminValues() })
            }
            Spacer(Modifier.height(8.dp))
        }

        // Barra inferior
        if (vm.adminTab == 0) Surface(color = colors.surface, tonalElevation = 3.dp, shadowElevation = 10.dp) {
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
private fun SecurityCard(vm: OrderViewModel) {
    val colors = MaterialTheme.colorScheme
    var email by rememberSaveable(vm.recoveryEmailShown) { mutableStateOf(vm.recoveryEmailShown?.takeIf { !it.contains("•") } ?: "") }
    var changing by rememberSaveable { mutableStateOf(false) }
    var current by rememberSaveable { mutableStateOf("") }
    var new1 by rememberSaveable { mutableStateOf("") }
    var new2 by rememberSaveable { mutableStateOf("") }
    PanelCard("Seguridad", "Tu PIN y el correo donde lo recuperás si lo olvidás.") {
        val shown = vm.recoveryEmailShown
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo de recuperación") },
            placeholder = { Text(shown?.takeIf { it.contains("•") } ?: "tu@correo.com") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            supportingText = { if (!shown.isNullOrBlank()) Text("Actual: $shown") },
        )
        SecondaryButton(text = "Guardar correo", onClick = { vm.saveRecoveryEmail(email) })
        Spacer(Modifier.height(4.dp))
        if (!changing) {
            SecondaryButton(text = "Cambiar PIN", onClick = { changing = true })
        } else {
            OutlinedTextField(value = current, onValueChange = { current = it.trim() }, modifier = Modifier.fillMaxWidth(), label = { Text("PIN actual") }, singleLine = true, shape = MaterialTheme.shapes.medium, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = new1, onValueChange = { new1 = it.trim() }, modifier = Modifier.fillMaxWidth(), label = { Text("PIN nuevo (4 a 32 caracteres)") }, singleLine = true, shape = MaterialTheme.shapes.medium, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = new2, onValueChange = { new2 = it.trim() }, modifier = Modifier.fillMaxWidth(), label = { Text("Repetí el PIN nuevo") }, singleLine = true, shape = MaterialTheme.shapes.medium, isError = new2.isNotEmpty() && new1 != new2, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = "Cancelar", onClick = { changing = false; current = ""; new1 = ""; new2 = "" }, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Confirmar",
                    onClick = { vm.changePin(current, new1); changing = false; current = ""; new1 = ""; new2 = "" },
                    enabled = current.length >= 4 && new1.length in 4..32 && new1 == new2,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            "Guardá el PIN en un lugar seguro. Si lo perdés y no cargaste un correo, vas a necesitar ayuda de quien te entregó la app.",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Pedidos recibidos (del backend del dueño o, en modo local, los hechos desde este teléfono). */
@Composable
private fun OrdersList(vm: OrderViewModel) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (vm.adminSynced) "Los pedidos de la app y la web, con su estado." else "Modo local: solo los pedidos hechos desde este teléfono.",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f),
        )
        SecondaryButton(text = if (vm.ordersLoading) "Cargando…" else "Actualizar", onClick = { vm.loadOrders() }, modifier = Modifier.width(120.dp))
    }
    if (vm.adminOrders.any { it.optString("estado") in setOf("hecho", "cancelado") }) {
        SecondaryButton(text = "Borrar los pedidos hechos y cancelados", onClick = { vm.clearFinishedOrders() })
    }
    vm.ordersMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    vm.adminOrders.forEach { o ->
        val st = OrderStatus.parse(o.optString("estado")) ?: OrderStatus.NEW
        PanelCard("${o.optString("cliente").ifBlank { "Cliente" }} · ${Money.formatArs(o.optDouble("precio", 0.0).toInt())}", "${formatDate(o.opt("fecha"))} · pedido ${o.optString("id")}") {
            Text("${o.optString("origen")} → ${o.optString("destino")}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Text("${o.optString("vehiculo")} · ${o.optInt("ayudantes")} ayud. · ${o.optString("cuando")}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            val wa = o.optString("whatsapp").filter { it.isDigit() }
            if (wa.length >= 10) {
                SecondaryButton(text = "Escribirle por WhatsApp", onClick = { vm.openWhatsAppTo(wa, "Hola ${o.optString("cliente")}, te escribimos por tu pedido de flete ${o.optString("id")}.") })
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Estado", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (st == OrderStatus.DONE || st == OrderStatus.CANCELLED) {
                    TextButton(onClick = { vm.deleteOrder(o.optString("id")) }) { Text("Borrar", color = colors.error) }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OrderStatus.entries.forEach { option ->
                    val on = option == st
                    Surface(
                        shape = CircleShape,
                        color = if (on) colors.primary else colors.surfaceVariant,
                        modifier = Modifier.clickable(enabled = !on) { vm.setOrderStatus(o.optString("id"), option) },
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelMedium, color = if (on) colors.onPrimary else colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}

private fun formatDate(raw: Any?): String {
    val t = raw?.toString() ?: return ""
    // Del backend viene ISO ("2026-11-05T13:20:00.000Z"); en modo local ya viene "dd/MM/yyyy HH:mm".
    val m = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})""").find(t) ?: return t
    return runCatching {
        val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear(); set(m.groupValues[1].toInt(), m.groupValues[2].toInt() - 1, m.groupValues[3].toInt(), m.groupValues[4].toInt(), m.groupValues[5].toInt())
        }
        java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("es", "AR")).apply { timeZone = java.util.TimeZone.getTimeZone("America/Argentina/Buenos_Aires") }.format(utc.time)
    }.getOrDefault(t)
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
    val colors = MaterialTheme.colorScheme
    PanelCard(group.title, group.subtitle) {
        val numeric = group.fields.filter { !it.text && !it.bool && !it.icon }
        val others = group.fields.filter { it.text || it.bool || it.icon }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            others.forEach { f ->
                when {
                    f.bool -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(f.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                        val on = (vm.adminValues[f.key] ?: "1").trim().lowercase() !in setOf("0", "no", "false", "off")
                        Switch(checked = on, onCheckedChange = { vm.setAdminValue(f.key, if (it) "1" else "0") })
                    }
                    f.icon -> Column {
                        Text(f.label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VehicleKind.entries.forEach { k ->
                                val sel = (vm.adminValues[f.key] ?: "") == k.name.lowercase()
                                Box(
                                    Modifier
                                        .size(width = 76.dp, height = 48.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(if (sel) colors.primaryContainer else colors.surfaceVariant)
                                        .border(if (sel) 2.dp else 0.dp, if (sel) colors.primary else Color.Transparent, MaterialTheme.shapes.small)
                                        .clickable { vm.setAdminValue(f.key, k.name.lowercase()) },
                                    contentAlignment = Alignment.Center,
                                ) { VehicleIcon(kind = k, modifier = Modifier.size(width = 60.dp, height = 34.dp)) }
                            }
                        }
                    }
                    else -> ValueField(vm, f, Modifier.fillMaxWidth())
                }
            }
            if (numeric.size == 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { numeric.forEach { f -> ValueField(vm, f, Modifier.weight(1f)) } }
            } else {
                numeric.forEach { f -> ValueField(vm, f, Modifier.fillMaxWidth()) }
            }
            val vid = group.vehicleId
            if (vid != null && vid.startsWith("extra")) {
                SecondaryButton(text = "Quitar este vehículo", onClick = { vm.removeVehicleSlot(vid) })
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
                field.key.endsWith(".activo") -> KeyboardType.Number
                field.text -> KeyboardType.Text
                field.decimal -> KeyboardType.Decimal
                else -> KeyboardType.Number
            },
            imeAction = ImeAction.Next,
        ),
    )
}

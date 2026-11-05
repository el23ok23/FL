package com.fletaba.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fletaba.app.data.AddressProvider
import com.fletaba.app.data.AddressSuggestion
import com.fletaba.app.data.AppConfig
import com.fletaba.app.data.AppSignature
import com.fletaba.app.data.BrandColors
import com.fletaba.app.data.LocationHelper
import com.fletaba.app.data.Logo
import com.fletaba.app.data.CabaStreets
import com.fletaba.app.data.GeocoderProvider
import com.fletaba.app.data.GooglePlacesProvider
import com.fletaba.app.data.OrderSender
import com.fletaba.app.data.OsmMapService
import com.fletaba.app.data.RemotePrices
import com.fletaba.app.data.RouteInfo
import com.fletaba.app.data.RoutesService
import com.fletaba.app.data.SearchScope
import com.fletaba.app.data.StaticMapService
import com.fletaba.core.AddressPoint
import com.fletaba.core.ArgentinePhone
import com.fletaba.core.CargoItems
import com.fletaba.core.FloorInfo
import com.fletaba.core.LoadTime
import com.fletaba.core.LoadTimeBreakdown
import com.fletaba.core.OrderMessage
import com.fletaba.core.OrderSummary
import com.fletaba.core.PriceBreakdown
import com.fletaba.core.PriceCalculator
import com.fletaba.core.PriceSheet
import com.fletaba.core.PricingConfig
import com.fletaba.core.TimeConfig
import com.fletaba.core.Schedule
import com.fletaba.core.Stop
import com.fletaba.core.VehicleSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** Pantallas del flujo, en orden. */
enum class Step { INTRO, ROUTE, VEHICLE, DETAILS, QUOTE, DONE }

/** Una parada en edición: lo que escribió el usuario, el punto resuelto y el acceso. */
data class StopDraft(
    val id: Long,
    val query: String = "",
    val point: AddressPoint? = null,
    val floor: FloorInfo = FloorInfo(),
)

class OrderViewModel(app: Application) : AndroidViewModel(app) {

    val config: AppConfig = AppConfig.load(app)

    private val signature = AppSignature.sha1(app)
    private val addresses: AddressProvider =
        config.googleApiKey?.let { GooglePlacesProvider(it, app.packageName, signature, config.origin) } ?: GeocoderProvider(app, config.origin)
    private val routes: RoutesService? =
        config.googleApiKey?.let { RoutesService(it, app.packageName, signature) }
    private val sender = OrderSender(app, config)

    // ---- Precios (editables por el dueño desde su planilla) ---------------------------------
    var pricing: PricingConfig by mutableStateOf(config.pricing)
        private set
    var vehicles: List<VehicleSpec> by mutableStateOf(config.vehicles)
        private set
    var times: TimeConfig by mutableStateOf(config.times)
        private set
    /** true cuando los precios vienen de la planilla del dueño (descargada o en caché). */
    var pricesFromSheet by mutableStateOf(false)
        private set

    private val remotePrices = RemotePrices(app, config.remotePricesUrl, config.localOwnerPin, config.recoveryEmail)

    // ---- Marca (también editable por el dueño) -------------------------------------------------
    var businessName: String by mutableStateOf(config.businessName)
        private set
    var ownerWhatsApps: List<String> by mutableStateOf(config.ownerWhatsApps)
        private set
    var brand: BrandColors by mutableStateOf(config.brand)
        private set
    /** Dirección del logo del dueño ("local", https…) o null si usa el de fábrica. */
    var logoSource: String? by mutableStateOf(null)
        private set
    var logo: Bitmap? by mutableStateOf(null)
        private set
    private var branding = PriceSheet.Branding()

    private fun applySheet(csv: String) {
        val r = PriceSheet.apply(csv, config.pricing, config.vehicles, config.times)
        if (r.applied > 0) {
            pricing = r.pricing; vehicles = r.vehicles; times = r.times
            pricesFromSheet = true
            if (vehicleIndex > vehicles.lastIndex) vehicleIndex = vehicles.lastIndex
        }
        applyBranding(PriceSheet.branding(csv))
    }

    private fun applyBranding(b: PriceSheet.Branding) {
        branding = b
        businessName = b.name ?: config.businessName
        ownerWhatsApps = b.whatsapps.ifEmpty { config.ownerWhatsApps }
        brand = BrandColors(
            primary = AppConfig.parseColor(b.primary, config.brand.primary),
            accent = AppConfig.parseColor(b.accent, config.brand.accent),
        )
        if (b.logoUrl != logoSource || (b.logoUrl != null && logo == null)) {
            logoSource = b.logoUrl
            if (b.logoUrl == null) logo = null
            else viewModelScope.launch { logo = Logo.load(getApplication(), b.logoUrl, remotePrices.localLogoFile()) }
        }
    }
    // ---- Panel del dueño ----------------------------------------------------------------------
    /** El panel se abre manteniendo apretado el título de la primera pantalla. */
    var adminOpen by mutableStateOf(false)
        private set
    /** true una vez que el PIN fue aceptado. */
    var adminUnlocked by mutableStateOf(false)
        private set
    var adminBusy by mutableStateOf(false)
        private set
    /** Mensaje de estado del panel (error de PIN, sin internet, guardado). */
    var adminMessage by mutableStateOf<String?>(null)
        private set
    /** Valores que está editando el dueño, por clave de la planilla. */
    val adminValues = mutableStateMapOf<String, String>()
    val adminAvailable: Boolean get() = remotePrices.available
    /** true si los cambios llegan a todos los teléfonos; false si valen solo en este. */
    val adminSynced: Boolean get() = remotePrices.synced
    val adminForm: List<PriceSheet.Group>
        get() = PriceSheet.form(config.vehicles, advanced = config.pricing.commissionPct > 0 || config.pricing.operatingPct > 0)

    fun openAdmin() {
        adminMessage = null
        loadAdminValues(pricing, vehicles, times)
        adminOpen = true
        val pin = remotePrices.rememberedPin()
        if (pin != null && !adminUnlocked) unlockAdmin(pin, remember = true)
    }
    fun closeAdmin() { adminOpen = false; adminMessage = null }
    fun lockAdmin() { adminUnlocked = false; remotePrices.rememberPin(null); adminMessage = null }

    private fun loadAdminValues(p: PricingConfig, v: List<VehicleSpec>, t: TimeConfig, b: PriceSheet.Branding = branding) {
        adminValues.clear(); adminValues.putAll(PriceSheet.currentValues(p, v, t)); adminValues.putAll(PriceSheet.brandingValues(b))
        adminLogoPreview = null
    }
    /** Logo recién elegido en la galería, antes de guardar. */
    var adminLogoPreview: Bitmap? by mutableStateOf(null)
        private set

    /** El dueño eligió una imagen de la galería: se achica, se sube y queda lista para Guardar. */
    fun pickLogo(uri: Uri) {
        if (adminBusy) return
        val pin = remotePrices.rememberedPin() ?: run { adminUnlocked = false; return }
        adminBusy = true; adminMessage = null
        viewModelScope.launch {
            val prepared = Logo.prepare(getApplication(), uri)
            if (prepared == null) { adminMessage = "No se pudo leer esa imagen."; adminBusy = false; return@launch }
            val url = remotePrices.uploadLogo(prepared.second, pin)
            if (url == null) {
                adminMessage = "No se pudo subir el logo. Revisá la conexión y probá de nuevo."
            } else {
                adminLogoPreview = prepared.first
                adminValues[PriceSheet.KEY_LOGO] = url
                adminMessage = "Logo listo. Tocá Guardar para que se vea."
            }
            adminBusy = false
        }
    }
    fun removeLogo() { adminValues[PriceSheet.KEY_LOGO] = ""; adminLogoPreview = null }

    fun unlockAdmin(pin: String, remember: Boolean = true) {
        if (adminBusy) return
        adminBusy = true; adminMessage = null
        viewModelScope.launch {
            when (remotePrices.verifyPin(pin.trim())) {
                RemotePrices.Status.OK -> { adminUnlocked = true; if (remember) remotePrices.rememberPin(pin.trim()) }
                RemotePrices.Status.WRONG_PIN -> { adminMessage = "PIN incorrecto"; remotePrices.rememberPin(null) }
                RemotePrices.Status.OFFLINE -> adminMessage = "Sin conexión. Hace falta internet para entrar."
                RemotePrices.Status.NOT_CONFIGURED -> adminMessage = "El panel todavía no está configurado."
                else -> adminMessage = "No se pudo entrar. Probá de nuevo."
            }
            adminBusy = false
        }
    }

    fun setAdminValue(key: String, value: String) { adminValues[key] = value }

    // ---- Seguridad: PIN y correo de recuperación ----------------------------------------------
    val recoveryEmail: String? get() = remotePrices.recoveryEmail()
    private var emailTick by mutableIntStateOf(0) // fuerza recomposición al guardar el correo
    val recoveryEmailShown: String? get() { emailTick; return remotePrices.recoveryEmail() }

    fun changePin(current: String, newPin: String) {
        if (adminBusy) return
        adminBusy = true; adminMessage = null
        viewModelScope.launch {
            adminMessage = when (remotePrices.changePin(current.trim(), newPin.trim())) {
                RemotePrices.Status.OK -> { remotePrices.rememberPin(newPin.trim()); "PIN cambiado." }
                RemotePrices.Status.WRONG_PIN -> "El PIN actual no es correcto."
                RemotePrices.Status.INVALID -> "El PIN nuevo tiene que tener entre 4 y 32 caracteres."
                else -> "Sin conexión. No se pudo cambiar el PIN."
            }
            adminBusy = false
        }
    }

    fun saveRecoveryEmail(email: String) {
        if (adminBusy) return
        val pin = remotePrices.rememberedPin() ?: run { adminUnlocked = false; return }
        adminBusy = true; adminMessage = null
        viewModelScope.launch {
            adminMessage = when (remotePrices.saveRecoveryEmail(pin, email)) {
                RemotePrices.Status.OK -> { emailTick++; if (email.isBlank()) "Correo de recuperación quitado." else "Correo de recuperación guardado." }
                RemotePrices.Status.INVALID -> "Ese correo no parece válido."
                RemotePrices.Status.WRONG_PIN -> { adminUnlocked = false; "Volvé a entrar con tu PIN." }
                else -> "Sin conexión. No se pudo guardar el correo."
            }
            adminBusy = false
        }
    }

    /** "Olvidé mi PIN": lo manda al correo de recuperación (o abre el correo en modo local). */
    fun recoverPin() {
        if (adminBusy) return
        adminBusy = true; adminMessage = null
        viewModelScope.launch {
            val (st, info) = remotePrices.recover()
            adminMessage = when (st) {
                RemotePrices.Status.OK -> {
                    if (remotePrices.synced) "Te mandamos el PIN a $info."
                    else {
                        val (mail, pin) = info.orEmpty().split("|").let { it[0] to it.getOrElse(1) { "" } }
                        openMail(mail, "Tu PIN del panel · $businessName", "Tu PIN para entrar al panel del dueño es: $pin")
                        "Se abrió tu correo con el PIN para $mail. Enviátelo y listo."
                    }
                }
                RemotePrices.Status.NO_EMAIL -> "No hay un correo de recuperación cargado. Pedile el PIN a quien te entregó la app."
                RemotePrices.Status.LIMIT -> "Ya se mandó el PIN tres veces en la última hora. Revisá el correo (y el spam)."
                else -> "Sin conexión. Probá de nuevo con internet."
            }
            adminBusy = false
        }
    }

    private fun openMail(to: String, subject: String, body: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            putExtra(android.content.Intent.EXTRA_TEXT, body)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { getApplication<Application>().startActivity(intent) }
    }
    /** Vuelve a cargar en el formulario los precios originales de la app (no guarda hasta tocar Guardar). */
    fun resetAdminValues() { loadAdminValues(config.pricing, config.vehicles, config.times, PriceSheet.Branding()); adminMessage = "Precios originales cargados. Tocá Guardar para aplicarlos." }
    val adminValid: Boolean get() = adminForm.flatMap { it.fields }.all { PriceSheet.isValid(it.key, adminValues[it.key] ?: "") }

    fun saveAdmin() {
        if (adminBusy || !adminValid) return
        val pin = remotePrices.rememberedPin() ?: run { adminUnlocked = false; return }
        val csv = PriceSheet.toCsv(adminValues)
        adminBusy = true; adminMessage = null
        viewModelScope.launch {
            when (remotePrices.publish(csv, pin)) {
                RemotePrices.Status.OK -> {
                    applySheet(csv)
                    if (adminLogoPreview != null) { logo = adminLogoPreview }
                    adminMessage = if (remotePrices.synced) "Guardado. Ya rige en todos los teléfonos." else "Guardado en este teléfono."
                }
                RemotePrices.Status.WRONG_PIN -> { adminMessage = "PIN incorrecto"; adminUnlocked = false; remotePrices.rememberPin(null) }
                RemotePrices.Status.OFFLINE -> adminMessage = "Sin conexión. No se pudo guardar; probá de nuevo."
                RemotePrices.Status.NOT_CONFIGURED -> adminMessage = "El panel todavía no está configurado."
                else -> adminMessage = "No se pudo guardar. Probá de nuevo."
            }
            adminBusy = false
        }
    }

    /** Mapa real de Google (solo con clave). */
    val staticMaps: StaticMapService? = config.staticMapsKey?.let { StaticMapService(it) }
    /** Mapa real de OpenStreetMap, sin clave (respaldo). */
    val osmMaps = OsmMapService()

    // ---- Navegación -------------------------------------------------------------------------
    var step by mutableStateOf(Step.INTRO)
        private set

    fun next() { if (step.ordinal < Step.DONE.ordinal) step = Step.entries[step.ordinal + 1] }
    fun back() {
        when (step) {
            Step.INTRO, Step.ROUTE -> Unit
            Step.DONE -> reset()
            else -> step = Step.entries[step.ordinal - 1]
        }
    }
    fun finishIntro() { if (step == Step.INTRO) step = Step.ROUTE }

    // ---- Paradas ----------------------------------------------------------------------------
    private var nextId = 1L
    private fun newStop() = StopDraft(id = nextId++)

    /** Origen, paradas intermedias y destino (mínimo 2, máximo [OrderSummary.MAX_STOPS]). */
    val stops = mutableStateListOf(newStop(), newStop())

    var activeStop by mutableIntStateOf(0)
        private set
    var suggestions by mutableStateOf<List<AddressSuggestion>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var noResults by mutableStateOf(false)
        private set
    var route by mutableStateOf<RouteInfo?>(null)
        private set
    var routeLoading by mutableStateOf(false)
        private set

    private var searchJob: Job? = null
    private var routeJob: Job? = null
    private var sessionToken = UUID.randomUUID().toString()

    /** Cambia cada vez que la app quiere que el foco pase al campo [activeStop] (o se cierre si no queda ninguno). */
    var focusRequestId by mutableIntStateOf(0)
        private set

    private fun scopeFor(index: Int) = if (index == 0) SearchScope.ORIGIN else SearchScope.ARGENTINA

    val canAddStop: Boolean get() = stops.size < OrderSummary.MAX_STOPS

    /** Calles de Capital que coinciden con lo escrito en el origen (sin altura todavía). */
    val streetHints: List<String>
        get() {
            if (activeStop != 0) return emptyList()
            val s = stops.firstOrNull() ?: return emptyList()
            if (s.point != null) return emptyList()
            val list = config.origin.streets.ifEmpty { if (config.origin.shortName == "CABA") CabaStreets.names else emptyList() }
            if (list.isEmpty()) return emptyList()
            return CabaStreets.suggest(s.query, list = list)
        }

    /** Completa el origen con la calle elegida; falta escribir la altura. */
    fun pickStreet(name: String) {
        val s = stops.firstOrNull() ?: return
        stops[0] = s.copy(query = "$name ", point = null)
        activeStop = 0
        clearSuggestions()
        invalidateRoute()
    }

    fun addStop() {
        if (!canAddStop) return
        // La nueva parada se inserta antes del destino.
        stops.add(stops.size - 1, newStop())
        activeStop = stops.size - 2
        clearSuggestions()
    }

    fun removeStop(index: Int) {
        if (stops.size <= 2 || index !in stops.indices) return
        stops.removeAt(index)
        activeStop = activeStop.coerceIn(0, stops.lastIndex)
        clearSuggestions()
        recomputeRoute()
    }

    fun focusStop(index: Int) {
        if (activeStop != index) {
            activeStop = index
            clearSuggestions()
            val s = stops.getOrNull(index) ?: return
            if (s.point == null && s.query.isNotBlank()) search(s.query)
        }
    }

    fun onQueryChange(index: Int, text: String) {
        val s = stops.getOrNull(index) ?: return
        activeStop = index
        val lostPoint = s.point != null && s.point.label != text
        stops[index] = s.copy(query = text, point = if (lostPoint) null else s.point)
        if (lostPoint) invalidateRoute()
        search(text)
    }

    private fun clearSuggestions() {
        searchJob?.cancel()
        suggestions = emptyList()
        searching = false
        noResults = false
    }

    private fun search(text: String) {
        searchJob?.cancel()
        noResults = false
        val q = text.trim()
        if (q.length < 3) { suggestions = emptyList(); searching = false; return }
        val index = activeStop
        searchJob = viewModelScope.launch {
            delay(350) // espera a que deje de tipear
            searching = true
            val result = addresses.suggest(q, sessionToken, scopeFor(index))
            val stillCurrent = activeStop == index && stops.getOrNull(index)?.query?.trim() == q
            if (stillCurrent) { suggestions = result; noResults = result.isEmpty() }
            searching = false
        }
    }

    /**
     * Enter en el teclado: busca ya mismo y, si hay una sola coincidencia, la toma directamente
     * (con barrio y localidad) y pasa al siguiente campo. Si hay varias, las muestra.
     */
    fun submitActive() {
        val index = activeStop
        val draft = stops.getOrNull(index) ?: return
        val q = draft.query.trim()
        if (draft.point != null) { advanceFocus(); return }
        if (q.length < 3) return
        if (suggestions.size == 1) { selectSuggestion(suggestions.first()); return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searching = true
            val result = addresses.suggest(q, sessionToken, scopeFor(index))
            searching = false
            if (activeStop != index) return@launch
            if (result.size == 1) selectSuggestion(result.first())
            else { suggestions = result; noResults = result.isEmpty() }
        }
    }

    private fun advanceFocus() {
        val next = stops.indexOfFirst { it.point == null }
        if (next >= 0) activeStop = next
        focusRequestId++
    }

    fun selectSuggestion(s: AddressSuggestion) {
        val index = activeStop
        val draft = stops.getOrNull(index) ?: return
        clearSuggestions()
        val provisional = "${s.primary}${if (s.secondary.isNotBlank()) ", ${s.secondary}" else ""}"
        stops[index] = draft.copy(query = provisional)
        viewModelScope.launch {
            val point = addresses.resolve(s, sessionToken)
            sessionToken = UUID.randomUUID().toString()
            if (point == null) return@launch
            val current = stops.getOrNull(index) ?: return@launch
            stops[index] = current.copy(query = point.label, point = point)
            advanceFocus() // pasa al siguiente campo vacío (o cierra el teclado si no queda ninguno)
            recomputeRoute()
        }
    }

    fun clearStop(index: Int) {
        val s = stops.getOrNull(index) ?: return
        stops[index] = s.copy(query = "", point = null)
        activeStop = index
        clearSuggestions()
        invalidateRoute()
    }

    fun updateFloor(index: Int, floor: FloorInfo) {
        val s = stops.getOrNull(index) ?: return
        stops[index] = s.copy(floor = floor)
    }

    private fun invalidateRoute() {
        routeJob?.cancel()
        route = null
        routeLoading = false
    }

    private fun recomputeRoute() {
        invalidateRoute()
        if (!allResolved) return
        val service = routes ?: return
        val points = stops.map { it.point!!.location }
        routeJob = viewModelScope.launch {
            routeLoading = true
            route = service.compute(points)
            routeLoading = false
        }
    }

    val allResolved: Boolean get() = stops.all { it.point != null }
    val allInside: Boolean get() = stops.all { it.point?.insideCoverage == true }
    val outOfCoverage: Boolean get() = stops.any { it.point?.insideCoverage == false }
    /** El origen está resuelto pero fuera de la zona de origen configurada. */
    val originOutsideCaba: Boolean get() = stops.firstOrNull()?.point?.let { it.insideCoverage && !it.insideOrigin } == true
    val canContinueRoute: Boolean
        get() = allResolved && allInside && !originOutsideCaba &&
            stops.map { it.point!!.location }.distinct().size == stops.size

    /** Puntos ya resueltos, en orden (para el mapa). */
    val resolvedPoints: List<AddressPoint?> get() = stops.map { it.point }

    /** Distancia usada para el precio: la del recorrido real o una estimación inmediata. */
    val distanceKm: Double
        get() {
            route?.let { return it.distanceKm }
            val pts = stops.mapNotNull { it.point?.location }
            if (pts.size < 2) return 0.0
            return PriceCalculator.estimateRoadKm(pts, pricing)
        }

    // ---- Vehículo, ayudantes, carga, cuándo -------------------------------------------------
    var vehicleIndex by mutableIntStateOf(DEFAULT_VEHICLE.coerceAtMost(vehicles.lastIndex))
        private set
    fun selectVehicle(index: Int) { vehicleIndex = index.coerceIn(0, vehicles.lastIndex) }
    val vehicle: VehicleSpec get() = vehicles[vehicleIndex]

    var helpers by mutableIntStateOf(0)
        private set
    fun selectHelpers(n: Int) { helpers = n.coerceIn(0, 2) }

    /** Cantidad de cada cosa elegida (id → cantidad). */
    val cargoCounts = mutableStateMapOf<String, Int>()
    fun changeCargo(id: String, delta: Int) {
        val next = ((cargoCounts[id] ?: 0) + delta).coerceIn(0, 99)
        if (next == 0) cargoCounts.remove(id) else cargoCounts[id] = next
    }

    /** Texto libre adicional ("otras cosas"). */
    var cargoNote by mutableStateOf("")
        private set
    fun setCargoText(text: String) { cargoNote = text.take(CARGO_MAX) }

    /** Descripción completa de la carga: ítems con cantidad + nota. */
    val cargo: String
        get() = listOf(CargoItems.describe(cargoCounts), cargoNote.trim()).filter { it.isNotBlank() }.joinToString(". ")

    /** Volumen estimado de lo elegido. */
    val cargoVolume: Double get() = CargoItems.volume(cargoCounts)

    var schedule by mutableStateOf(Schedule())
        private set
    fun updateSchedule(s: Schedule) { schedule = s }

    /** Precio aproximado, siempre al día con las elecciones actuales. */
    val price: PriceBreakdown
        get() = PriceCalculator.calculate(vehicle, distanceKm, helpers, stops.map { it.floor }, pricing, weekend = schedule.isWeekend())

    /** Tiempo de carga y descarga incluido en el precio, parada por parada. */
    val loadTime: LoadTimeBreakdown
        get() = LoadTime.calculate(vehicle, helpers, stops.map { it.floor }, times)

    // ---- Contacto y envío --------------------------------------------------------------------
    private val customerPrefs = app.getSharedPreferences("fletaba_cliente", android.content.Context.MODE_PRIVATE)
    /** Nombre y WhatsApp quedan recordados en el teléfono para el próximo pedido. */
    var customerName by mutableStateOf(customerPrefs.getString("nombre", "") ?: "")
        private set
    var customerPhone by mutableStateOf(customerPrefs.getString("whatsapp", "") ?: "")
        private set
    fun setName(text: String) { customerName = text.take(60) }
    fun setPhone(text: String) { customerPhone = text.filter { it.isDigit() || it in " +-()" }.take(24) }

    val phoneNormalized: String? get() = ArgentinePhone.normalize(customerPhone)
    val nameValid: Boolean get() = customerName.trim().length >= 2
    val canSend: Boolean get() = nameValid && phoneNormalized != null && !sending && allResolved

    var sending by mutableStateOf(false)
        private set
    var sendResult by mutableStateOf<OrderSender.Result?>(null)
        private set
    /** Último pedido armado, para abrir WhatsApp con otro de los números del dueño. */
    var lastMessage: String = ""
        private set
    fun openWhatsApp(number: String) { sender.openWhatsApp(number, lastMessage) }

    // ---- Ubicación actual como origen ---------------------------------------------------------
    var locating by mutableStateOf(false)
        private set
    var locationMessage by mutableStateOf<String?>(null)
        private set
    fun clearLocationMessage() { locationMessage = null }
    fun useMyLocationDenied() { locationMessage = "Sin permiso de ubicación. Escribí la dirección de salida." }

    /** Usa la ubicación del teléfono como origen (ya con el permiso concedido). */
    fun useMyLocation() {
        if (locating) return
        locating = true; locationMessage = null
        viewModelScope.launch {
            val here = LocationHelper.current(getApplication())
            if (here == null) {
                locationMessage = "No pudimos obtener tu ubicación. Activá el GPS y probá de nuevo."
                locating = false; return@launch
            }
            if (!config.origin.contains(here)) {
                locationMessage = "Tu ubicación no está en ${config.origin.name}. Escribí la dirección de salida."
                locating = false; return@launch
            }
            val label = LocationHelper.describe(getApplication(), here)
                ?: "Mi ubicación (%.4f, %.4f)".format(java.util.Locale.US, here.lat, here.lng)
            val point = AddressPoint(label = label, location = here, insideCoverage = true, insideOrigin = true)
            val s = stops.firstOrNull() ?: return@launch
            stops[0] = s.copy(query = label, point = point)
            activeStop = 0
            clearSuggestions()
            invalidateRoute()
            advanceFocus()
            locating = false
        }
    }

    fun send() {
        val phone = phoneNormalized ?: return
        if (sending || !allResolved) return
        val summary = OrderSummary(
            businessName = businessName,
            customerName = customerName,
            customerPhone = phone,
            stops = stops.map { Stop(it.point!!, it.floor) },
            vehicle = vehicle,
            helpers = helpers,
            cargo = cargo,
            schedule = schedule,
            distanceKm = distanceKm,
            price = price,
            loadTime = loadTime,
            waitFeePer15Min = times.waitFeePer15Min,
            travelMinutes = route?.durationMin,
        )
        customerPrefs.edit().putString("nombre", customerName.trim()).putString("whatsapp", customerPhone).apply()
        lastMessage = OrderMessage.build(summary)
        sending = true
        viewModelScope.launch {
            sendResult = sender.send(summary, ownerWhatsApps, config.remotePricesUrl)
            sending = false
            step = Step.DONE
        }
    }

    fun reset() {
        searchJob?.cancel(); routeJob?.cancel()
        stops.clear(); stops.add(newStop()); stops.add(newStop())
        activeStop = 0
        suggestions = emptyList(); searching = false; noResults = false
        route = null; routeLoading = false
        vehicleIndex = DEFAULT_VEHICLE.coerceAtMost(vehicles.lastIndex)
        helpers = 0
        cargoCounts.clear(); cargoNote = ""
        schedule = Schedule()
        sending = false; sendResult = null
        sessionToken = UUID.randomUUID().toString()
        step = Step.ROUTE
    }

    // Va al final: los bloques init corren en orden de declaración y este usa propiedades de arriba.
    init {
        remotePrices.cached()?.let { applySheet(it) }
        viewModelScope.launch { remotePrices.fetch()?.let { applySheet(it) } }
    }

    companion object {
        const val CARGO_MAX = 80
        /** Vehículo seleccionado al entrar (índice en config.json): el furgón mediano. */
        private const val DEFAULT_VEHICLE = 1
    }
}

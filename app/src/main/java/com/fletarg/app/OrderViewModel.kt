package com.fletarg.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fletarg.app.data.AddressProvider
import com.fletarg.app.data.AddressSuggestion
import com.fletarg.app.data.AppConfig
import com.fletarg.app.data.AppSignature
import com.fletarg.app.data.GeocoderProvider
import com.fletarg.app.data.GooglePlacesProvider
import com.fletarg.app.data.OrderSender
import com.fletarg.app.data.RouteInfo
import com.fletarg.app.data.RoutesService
import com.fletarg.app.data.SearchScope
import com.fletarg.app.data.StaticMapService
import com.fletarg.core.AddressPoint
import com.fletarg.core.ArgentinePhone
import com.fletarg.core.FloorInfo
import com.fletarg.core.LoadTime
import com.fletarg.core.LoadTimeBreakdown
import com.fletarg.core.OrderSummary
import com.fletarg.core.PriceBreakdown
import com.fletarg.core.PriceCalculator
import com.fletarg.core.Schedule
import com.fletarg.core.Stop
import com.fletarg.core.VehicleSpec
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
        config.googleApiKey?.let { GooglePlacesProvider(it, app.packageName, signature) } ?: GeocoderProvider(app)
    private val routes: RoutesService? =
        config.googleApiKey?.let { RoutesService(it, app.packageName, signature) }
    private val sender = OrderSender(app, config)
    /** Mapa real (solo con clave de Google); si es null la app dibuja el esquema. */
    val staticMaps: StaticMapService? = config.staticMapsKey?.let { StaticMapService(it) }

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

    private fun scopeFor(index: Int) = if (index == 0) SearchScope.CABA else SearchScope.ARGENTINA

    val canAddStop: Boolean get() = stops.size < OrderSummary.MAX_STOPS

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
    /** El origen está resuelto pero fuera de Capital Federal. */
    val originOutsideCaba: Boolean get() = stops.firstOrNull()?.point?.let { it.insideCoverage && !it.insideCaba } == true
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
            return PriceCalculator.estimateRoadKm(pts, config.pricing)
        }

    // ---- Vehículo, ayudantes, carga, cuándo -------------------------------------------------
    var vehicleIndex by mutableIntStateOf(DEFAULT_VEHICLE.coerceAtMost(config.vehicles.lastIndex))
        private set
    fun selectVehicle(index: Int) { vehicleIndex = index.coerceIn(0, config.vehicles.lastIndex) }
    val vehicle: VehicleSpec get() = config.vehicles[vehicleIndex]

    var helpers by mutableIntStateOf(0)
        private set
    fun selectHelpers(n: Int) { helpers = n.coerceIn(0, 2) }

    var cargo by mutableStateOf("")
        private set
    fun setCargoText(text: String) { cargo = text.take(CARGO_MAX) }

    var schedule by mutableStateOf(Schedule())
        private set
    fun updateSchedule(s: Schedule) { schedule = s }

    /** Precio aproximado, siempre al día con las elecciones actuales. */
    val price: PriceBreakdown
        get() = PriceCalculator.calculate(vehicle, distanceKm, helpers, stops.map { it.floor }, config.pricing)

    /** Tiempo de carga y descarga incluido en el precio, parada por parada. */
    val loadTime: LoadTimeBreakdown
        get() = LoadTime.calculate(vehicle, helpers, stops.map { it.floor }, config.times)

    // ---- Contacto y envío --------------------------------------------------------------------
    var customerName by mutableStateOf("")
        private set
    var customerPhone by mutableStateOf("")
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

    fun send() {
        val phone = phoneNormalized ?: return
        if (sending || !allResolved) return
        val summary = OrderSummary(
            businessName = config.businessName,
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
            waitFeePer15Min = config.times.waitFeePer15Min,
            travelMinutes = route?.durationMin,
        )
        sending = true
        viewModelScope.launch {
            sendResult = sender.send(summary)
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
        vehicleIndex = DEFAULT_VEHICLE.coerceAtMost(config.vehicles.lastIndex)
        helpers = 0
        cargo = ""
        schedule = Schedule()
        customerName = ""; customerPhone = ""
        sending = false; sendResult = null
        sessionToken = UUID.randomUUID().toString()
        step = Step.ROUTE
    }

    companion object {
        const val CARGO_MAX = 80
        /** Vehículo seleccionado al entrar (índice en config.json): el furgón mediano. */
        private const val DEFAULT_VEHICLE = 1
    }
}

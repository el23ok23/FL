package com.mifleteciudad.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mifleteciudad.app.data.AddressProvider
import com.mifleteciudad.app.data.AddressSuggestion
import com.mifleteciudad.app.data.AppConfig
import com.mifleteciudad.app.data.AppSignature
import com.mifleteciudad.app.data.GeocoderProvider
import com.mifleteciudad.app.data.GooglePlacesProvider
import com.mifleteciudad.app.data.OrderSender
import com.mifleteciudad.app.data.RouteInfo
import com.mifleteciudad.app.data.RoutesService
import com.mifleteciudad.core.AddressPoint
import com.mifleteciudad.core.ArgentinePhone
import com.mifleteciudad.core.FloorInfo
import com.mifleteciudad.core.OrderSummary
import com.mifleteciudad.core.PriceBreakdown
import com.mifleteciudad.core.PriceCalculator
import com.mifleteciudad.core.VehicleSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** Pantallas del flujo, en orden. */
enum class Step { INTRO, ROUTE, VEHICLE, HELPERS, FLOORS, CARGO, PRICE, CONTACT, DONE }

enum class Field { ORIGIN, DESTINATION }

class OrderViewModel(app: Application) : AndroidViewModel(app) {

    val config: AppConfig = AppConfig.load(app)

    private val signature = AppSignature.sha1(app)
    private val addresses: AddressProvider =
        config.googleApiKey?.let { GooglePlacesProvider(it, app.packageName, signature, config.zone) }
            ?: GeocoderProvider(app, config.zone)
    private val routes: RoutesService? =
        config.googleApiKey?.let { RoutesService(it, app.packageName, signature) }
    private val sender = OrderSender(app, config)

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

    // ---- Origen y destino ----------------------------------------------------------------------
    var originQuery by mutableStateOf("")
        private set
    var destinationQuery by mutableStateOf("")
        private set
    var origin by mutableStateOf<AddressPoint?>(null)
        private set
    var destination by mutableStateOf<AddressPoint?>(null)
        private set
    var activeField by mutableStateOf(Field.ORIGIN)
        private set
    var suggestions by mutableStateOf<List<AddressSuggestion>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    /** La última búsqueda terminó sin resultados (para avisar al usuario). */
    var noResults by mutableStateOf(false)
        private set
    var route by mutableStateOf<RouteInfo?>(null)
        private set
    var routeLoading by mutableStateOf(false)
        private set

    private var searchJob: Job? = null
    private var routeJob: Job? = null
    private var sessionToken = UUID.randomUUID().toString()

    fun focusField(field: Field) {
        if (activeField != field) {
            activeField = field
            suggestions = emptyList()
            noResults = false
            val q = if (field == Field.ORIGIN) originQuery else destinationQuery
            val chosen = if (field == Field.ORIGIN) origin else destination
            if (chosen == null && q.isNotBlank()) search(q)
        }
    }

    fun onQueryChange(field: Field, text: String) {
        activeField = field
        if (field == Field.ORIGIN) {
            originQuery = text
            if (origin != null && origin?.label != text) { origin = null; invalidateRoute() }
        } else {
            destinationQuery = text
            if (destination != null && destination?.label != text) { destination = null; invalidateRoute() }
        }
        search(text)
    }

    private fun search(text: String) {
        searchJob?.cancel()
        val q = text.trim()
        noResults = false
        if (q.length < 3) { suggestions = emptyList(); searching = false; return }
        val field = activeField
        searchJob = viewModelScope.launch {
            delay(350) // espera a que deje de tipear
            searching = true
            val result = addresses.suggest(q, sessionToken)
            val stillCurrent = activeField == field &&
                (if (field == Field.ORIGIN) originQuery else destinationQuery).trim() == q
            if (stillCurrent) { suggestions = result; noResults = result.isEmpty() }
            searching = false
        }
    }

    fun selectSuggestion(s: AddressSuggestion) {
        val field = activeField
        searchJob?.cancel()
        suggestions = emptyList()
        searching = false
        noResults = false
        val provisional = "${s.primary}${if (s.secondary.isNotBlank()) ", ${s.secondary}" else ""}"
        if (field == Field.ORIGIN) originQuery = provisional else destinationQuery = provisional
        viewModelScope.launch {
            val point = addresses.resolve(s, sessionToken)
            sessionToken = UUID.randomUUID().toString()
            if (point == null) return@launch
            if (field == Field.ORIGIN) { origin = point; originQuery = point.label } else { destination = point; destinationQuery = point.label }
            if (field == Field.ORIGIN && destination == null) activeField = Field.DESTINATION
            recomputeRoute()
        }
    }

    fun clearField(field: Field) {
        if (field == Field.ORIGIN) { originQuery = ""; origin = null } else { destinationQuery = ""; destination = null }
        activeField = field
        suggestions = emptyList()
        noResults = false
        invalidateRoute()
    }

    private fun invalidateRoute() {
        routeJob?.cancel()
        route = null
        routeLoading = false
    }

    private fun recomputeRoute() {
        invalidateRoute()
        val o = origin ?: return
        val d = destination ?: return
        if (!o.insideCaba || !d.insideCaba) return
        val service = routes ?: return
        routeJob = viewModelScope.launch {
            routeLoading = true
            route = service.compute(o.location, d.location)
            routeLoading = false
        }
    }

    val bothInside: Boolean get() = origin?.insideCaba == true && destination?.insideCaba == true
    val outOfZone: Boolean get() = origin?.insideCaba == false || destination?.insideCaba == false
    val canContinueRoute: Boolean get() = bothInside && origin?.location != destination?.location

    /** Distancia usada para el precio: la del recorrido real o una estimación inmediata. */
    val distanceKm: Double
        get() {
            route?.let { return it.distanceKm }
            val o = origin ?: return 0.0
            val d = destination ?: return 0.0
            return PriceCalculator.estimateRoadKm(o.location, d.location, config.pricing)
        }

    // ---- Vehículo, ayudantes, pisos, carga ---------------------------------------------------
    var vehicleIndex by mutableIntStateOf(DEFAULT_VEHICLE.coerceAtMost(config.vehicles.lastIndex))
        private set
    fun selectVehicle(index: Int) { vehicleIndex = index.coerceIn(0, config.vehicles.lastIndex) }
    val vehicle: VehicleSpec get() = config.vehicles[vehicleIndex]

    var helpers by mutableIntStateOf(0)
        private set
    fun selectHelpers(n: Int) { helpers = n.coerceIn(0, 2) }

    var originFloor by mutableStateOf(FloorInfo())
        private set
    var destinationFloor by mutableStateOf(FloorInfo())
        private set
    fun updateOriginFloor(f: FloorInfo) { originFloor = f }
    fun updateDestinationFloor(f: FloorInfo) { destinationFloor = f }

    var cargo by mutableStateOf("")
        private set
    fun setCargoText(text: String) { cargo = text.take(CARGO_MAX) }

    /** Precio aproximado, siempre al día con las elecciones actuales. */
    val price: PriceBreakdown
        get() = PriceCalculator.calculate(vehicle, distanceKm, helpers, originFloor, destinationFloor, config.pricing)

    // ---- Contacto y envío --------------------------------------------------------------------
    var customerName by mutableStateOf("")
        private set
    var customerPhone by mutableStateOf("")
        private set
    fun setName(text: String) { customerName = text.take(60) }
    fun setPhone(text: String) { customerPhone = text.filter { it.isDigit() || it in " +-()" }.take(24) }

    val phoneNormalized: String? get() = ArgentinePhone.normalize(customerPhone)
    val nameValid: Boolean get() = customerName.trim().length >= 2
    val canSend: Boolean get() = nameValid && phoneNormalized != null && !sending

    var sending by mutableStateOf(false)
        private set
    var sendResult by mutableStateOf<OrderSender.Result?>(null)
        private set

    fun send() {
        val o = origin ?: return
        val d = destination ?: return
        val phone = phoneNormalized ?: return
        if (sending) return
        val summary = OrderSummary(
            businessName = config.businessName,
            customerName = customerName,
            customerPhone = phone,
            origin = o,
            destination = d,
            vehicle = vehicle,
            helpers = helpers,
            originFloor = originFloor,
            destinationFloor = destinationFloor,
            cargo = cargo,
            distanceKm = distanceKm,
            price = price,
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
        originQuery = ""; destinationQuery = ""
        origin = null; destination = null
        activeField = Field.ORIGIN
        suggestions = emptyList(); searching = false; noResults = false
        route = null; routeLoading = false
        vehicleIndex = DEFAULT_VEHICLE.coerceAtMost(config.vehicles.lastIndex)
        helpers = 0
        originFloor = FloorInfo(); destinationFloor = FloorInfo()
        cargo = ""
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

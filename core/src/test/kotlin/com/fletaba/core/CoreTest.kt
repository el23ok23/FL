package com.fletaba.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgentinaTest {
    @Test fun `cobertura nacional por codigo de pais`() {
        assertTrue(Argentina.isInside("AR"))
        assertTrue(Argentina.isInside("ar"))
        assertFalse(Argentina.isInside("UY"))
        assertFalse(Argentina.isInside("CL", LatLng(-34.6, -58.4)))
    }

    @Test fun `sin codigo decide el rectangulo del pais`() {
        assertTrue(Argentina.isInside(null, LatLng(-34.6037, -58.3816))) // Obelisco
        assertTrue(Argentina.isInside(null, LatLng(-31.4201, -64.1888))) // Córdoba
        assertTrue(Argentina.isInside(null, LatLng(-54.8019, -68.3030))) // Ushuaia
        assertFalse(Argentina.isInside(null, LatLng(40.4168, -3.7038))) // Madrid
        assertTrue(Argentina.isInside(null, null))
    }
}

class OriginZoneTest {
    private val CabaZone = OriginZone.CABA

    @Test fun `puntos dentro y fuera de capital federal`() {
        assertTrue(CabaZone.contains(LatLng(-34.6037, -58.3816))) // Obelisco
        assertTrue(CabaZone.contains(LatLng(-34.5875, -58.4300))) // Palermo
        assertTrue(CabaZone.contains(LatLng(-34.6500, -58.4900))) // Mataderos
        assertFalse(CabaZone.contains(LatLng(-34.5260, -58.4800))) // Vicente López
        assertFalse(CabaZone.contains(LatLng(-34.6600, -58.3650))) // Avellaneda
        assertFalse(CabaZone.contains(LatLng(-31.4201, -64.1888))) // Córdoba
    }

    @Test fun `texto administrativo manda sobre el poligono`() {
        val obelisco = LatLng(-34.6037, -58.3816)
        assertFalse(CabaZone.isInside(obelisco, listOf("Provincia de Buenos Aires")))
        assertTrue(CabaZone.isInside(LatLng(-34.6600, -58.3650), listOf("Cdad. Autónoma de Buenos Aires")))
        assertTrue(CabaZone.isInside(obelisco, listOf(null, "")))
    }

    @Test fun `zona de salto hasta 120 km a la redonda`() {
        val salto = OriginZone.SALTO
        assertTrue(salto.contains(LatLng(-34.2925, -60.2550))) // Salto centro
        assertTrue(salto.contains(LatLng(-33.8900, -60.5700))) // Pergamino (~55 km)
        assertTrue(salto.contains(LatLng(-33.3358, -60.2107))) // San Nicolás (~106 km)
        assertTrue(salto.contains(LatLng(-34.5850, -60.9430))) // Junín (~71 km)
        assertFalse(salto.contains(LatLng(-34.6037, -58.3816))) // Obelisco (~176 km)
        assertFalse(salto.contains(LatLng(-32.9468, -60.6393))) // Rosario (~154 km)
        // Borde: 115 km adentro, 125 km afuera (hacia el oeste)
        val c = OriginZone.SALTO_CENTER
        val kmPerLng = 111.32 * Math.cos(Math.toRadians(c.lat))
        assertTrue(salto.contains(LatLng(c.lat, c.lng - 115 / kmPerLng)))
        assertFalse(salto.contains(LatLng(c.lat, c.lng - 125 / kmPerLng)))
        // En Salto, "Provincia de Buenos Aires" no es motivo para quedar afuera.
        assertTrue(salto.isInside(LatLng(-34.2925, -60.2550), listOf("Provincia de Buenos Aires")))
        assertFalse(salto.isInside(LatLng(-34.2925, -60.2550), listOf("Salto, Uruguay")))
    }

    @Test fun `circulo aproximado`() {
        val poly = OriginZone.circle(LatLng(-34.0, -60.0), 10.0, 36)
        assertEquals(36, poly.size)
        poly.forEach { assertEquals(10.0, Geo.haversineKm(LatLng(-34.0, -60.0), it), 0.15) }
    }
}

class GeoTest {
    @Test fun `haversine obelisco a aeroparque`() {
        val km = Geo.haversineKm(LatLng(-34.6037, -58.3816), LatLng(-34.5592, -58.4156))
        assertTrue("fue $km", km > 5.5 && km < 6.3)
    }

    @Test fun `decodifica polilinea de ejemplo de google`() {
        val pts = Geo.decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, pts.size)
        assertEquals(38.5, pts[0].lat, 1e-5)
        assertEquals(-120.2, pts[0].lng, 1e-5)
        assertEquals(43.252, pts[2].lat, 1e-5)
        assertEquals(-126.453, pts[2].lng, 1e-5)
    }
}

class PriceCalculatorTest {
    private val config = PricingConfig(perHelper = 15000, perHelperKm = 500, perStairFloor = 6000, perExtraStop = 8000, roundTo = 500, urbanDistanceFactor = 1.3, minKm = 2.0)
    private val kangoo = VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", baseFare = 25000, perKm = 1800)

    @Test fun `formula completa`() {
        val p = PriceCalculator.calculate(
            vehicle = kangoo,
            distanceKm = 7.8,
            helpers = 1,
            floors = listOf(FloorInfo(Access.STAIRS, 3), FloorInfo(Access.ELEVATOR, 10)),
            config = config,
        )
        // Las líneas visibles se reparten el redondeo del total (diferencias de pocos pesos).
        assertEquals(25000.0, p.base.toDouble(), 60.0)
        assertEquals(0, p.extraStopCount)
        assertEquals(14040.0, p.distance.toDouble(), 20.0)
        assertEquals(18900.0, p.helpers.toDouble(), 20.0) // base + 7,8 km × 500
        assertEquals(3, p.stairFloors)
        assertEquals(18000.0, p.stairs.toDouble(), 20.0)
        assertEquals(p.total, p.base + p.distance + p.helpers + p.stairs + p.extraStops)
        // 25000 + 14040 + 18900 + 18000 = 75940 → redondeado hacia arriba a 500
        assertEquals(75940, p.subtotal)
        assertEquals(76000, p.total)
        assertEquals(0, p.commission)
        assertEquals(76000, p.netForDriver)
    }

    @Test fun `km minimo y sin extras`() {
        val p = PriceCalculator.calculate(kangoo, 0.4, 0, listOf(FloorInfo(), FloorInfo()), config)
        assertEquals(2.0, p.km, 0.0)
        // 25.000 + 3.600 = 28.600 → redondeado hacia arriba a 29.000
        assertEquals(29000, p.total)
    }

    @Test fun `ayudantes se limitan a dos`() {
        val p = PriceCalculator.calculate(kangoo, 2.0, 7, listOf(FloorInfo(), FloorInfo()), config)
        assertEquals(2, p.helperCount)
    }

    @Test fun `comision y recargo operativo invisibles`() {
        val cfg = config.copy(operatingPct = 0.20, commissionPct = 0.15)
        val p = PriceCalculator.calculate(kangoo, 10.0, 1, listOf(FloorInfo(), FloorInfo()), cfg)
        // subtotal: 25000 + 18000 + 20000 = 63000 → × 1,2 / 0,85 = 88.941 → 89.000
        assertEquals(63000, p.subtotal)
        assertEquals(89000, p.total)
        assertEquals(13350, p.commission)             // 15 % del bruto
        assertEquals(75650, p.netForDriver)
        assertEquals(p.netForDriver - p.subtotal, p.operatingCost)
        // Las líneas visibles suman exactamente el total y no hay línea de comisión.
        assertEquals(p.total, p.base + p.distance + p.helpers + p.stairs + p.extraStops)
        assertTrue(p.helpers > 20000) // el ayudante también se muestra ya "engordado"
        assertEquals(PriceCalculator.gross(20000, cfg), 28235)
    }

    @Test fun `el ayudante cuesta mas cuanto mas lejos`() {
        assertEquals(16500, PriceCalculator.helperCost(3.0, config))
        assertEquals(35000, PriceCalculator.helperCost(40.0, config))
        assertEquals(16000, PriceCalculator.helperCost(0.5, config)) // aplica el km mínimo
        val cerca = PriceCalculator.calculate(kangoo, 3.0, 1, listOf(FloorInfo(), FloorInfo()), config)
        val lejos = PriceCalculator.calculate(kangoo, 40.0, 1, listOf(FloorInfo(), FloorInfo()), config)
        assertTrue(lejos.helpers > cerca.helpers)
    }

    @Test fun `paradas adicionales y escaleras en todas`() {
        val floors = listOf(FloorInfo(Access.STAIRS, 2), FloorInfo(Access.STAIRS, 1), FloorInfo(), FloorInfo(Access.STAIRS, 1))
        val p = PriceCalculator.calculate(kangoo, 10.0, 0, floors, config)
        assertEquals(2, p.extraStopCount)
        assertEquals(16000, p.extraStops)
        assertEquals(4, p.stairFloors)
        assertEquals(24000, p.stairs)
        // 25000 + 18000 + 24000 + 16000 = 83000
        assertEquals(83000, p.total)
    }

    @Test fun `distancia de un recorrido con paradas`() {
        val pts = listOf(LatLng(-34.6037, -58.3816), LatLng(-34.5592, -58.4156), LatLng(-34.6037, -58.3816))
        val km = PriceCalculator.estimateRoadKm(pts, config)
        assertTrue("fue $km", km > 14.5 && km < 16.5) // ida y vuelta ~5,9 km × 2 × 1,3
    }

    @Test fun `formato de dinero`() {
        assertEquals("$ 85.000", Money.formatArs(85000))
        assertEquals("$ 1.250.500", Money.formatArs(1250500))
        assertEquals("$ 900", Money.formatArs(900))
        assertEquals("7,8 km", Money.formatKm(7.84))
        assertEquals("12,0 km", Money.formatKm(12.0))
    }

    @Test fun `descripcion de pisos`() {
        assertEquals("Planta baja", FloorInfo().describe())
        assertEquals("Piso 3 por escalera", FloorInfo(Access.STAIRS, 3).describe())
        assertEquals("Piso 5 por ascensor", FloorInfo(Access.ELEVATOR, 5).describe())
    }
}

class ArgentinePhoneTest {
    @Test fun `formatos validos de CABA`() {
        val expected = "5491123456789"
        listOf(
            "11 2345 6789",
            "1123456789",
            "011 2345-6789",
            "011 15 2345 6789",
            "15 2345 6789",
            "+54 9 11 2345-6789",
            "54 9 11 2345 6789",
            "+54 11 2345 6789",
            "0054 9 11 2345 6789",
        ).forEach { assertEquals("entrada: $it", expected, ArgentinePhone.normalize(it)) }
    }

    @Test fun `formatos validos del interior`() {
        assertEquals("5493511234567", ArgentinePhone.normalize("351 15 123 4567"))
        assertEquals("5493511234567", ArgentinePhone.normalize("0351 123 4567"))
        assertEquals("5492920412345", ArgentinePhone.normalize("02920 15 412345"))
        assertEquals("5492920412345", ArgentinePhone.normalize("+54 9 2920 41-2345"))
    }

    @Test fun `invalidos`() {
        assertNull(ArgentinePhone.normalize(""))
        assertNull(ArgentinePhone.normalize("1234"))
        assertNull(ArgentinePhone.normalize("11 2345 678"))      // le falta un dígito
        assertNull(ArgentinePhone.normalize("11 2345 67890"))    // sobra un dígito
        assertNull(ArgentinePhone.normalize("+1 212 555 0100"))  // no es argentino
        assertNull(ArgentinePhone.normalize("11 1111 1111"))     // repetido
        assertNull(ArgentinePhone.normalize("91 2345 6789"))     // área inexistente
    }

    @Test fun `formato para mostrar`() {
        assertEquals("+54 9 11 2345-6789", ArgentinePhone.format("5491123456789"))
        assertEquals("+54 9 351 123-4567", ArgentinePhone.format("5493511234567"))
    }
}

class ScheduleTest {
    @Test fun `dia de la semana y formatos`() {
        val d = DayDate(2026, 10, 28) // miércoles
        assertEquals(2, d.dayOfWeek)
        assertEquals("mié 28/10", d.short())
        assertEquals("miércoles 28 de octubre", d.long())
        assertEquals(DayDate(2026, 11, 1), d.plusDays(4))
        assertEquals("dom", DayDate(2026, 11, 1).dayName)
        assertEquals(0, DayDate(2024, 1, 1).dayOfWeek) // lunes
        assertEquals(6, DayDate(2000, 2, 27).dayOfWeek) // domingo
    }

    @Test fun `descripcion relativa`() {
        val today = DayDate(2026, 10, 28)
        assertEquals("lo antes posible, horario a coordinar", Schedule().describe(today))
        assertEquals("hoy (mié 28/10), por la tarde (13 a 18 h)", Schedule(today, TimeSlot.AFTERNOON).describe(today))
        assertEquals("mañana (jue 29/10), por la mañana (8 a 12 h)", Schedule(today.plusDays(1), TimeSlot.MORNING).describe(today))
        assertEquals("sábado 31 de octubre, horario a coordinar", Schedule(today.plusDays(3)).describe(today))
        assertEquals(14, DayDate.upcoming(14, today).size)
    }
}

class LoadTimeTest {
    private val cfg = TimeConfig(stairMinutesPerFloor = 6, elevatorMinutes = 10, intermediateFactor = 0.5, waitFeePer15Min = 6000)
    private val kangoo = VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", 25000, 1800, loadMinutes = 30)
    private val grande = VehicleSpec(VehicleKind.CAMION_GRANDE_CERRADO, "Camión grande", "", "", "", 130000, 6000, loadMinutes = 140)

    @Test fun `una kangoo se carga mucho antes que un camion grande`() {
        val k = LoadTime.calculate(kangoo, 0, listOf(FloorInfo(), FloorInfo()), cfg)
        val g = LoadTime.calculate(grande, 0, listOf(FloorInfo(), FloorInfo()), cfg)
        assertEquals(60, k.totalMinutes)   // 30 + 30
        assertEquals(280, g.totalMinutes)  // 140 + 140
        assertTrue(g.totalMinutes > k.totalMinutes)
    }

    @Test fun `los ayudantes bajan el tiempo y los pisos lo suben`() {
        val solo = LoadTime.calculate(kangoo, 0, listOf(FloorInfo(Access.STAIRS, 3), FloorInfo()), cfg)
        val con1 = LoadTime.calculate(kangoo, 1, listOf(FloorInfo(Access.STAIRS, 3), FloorInfo()), cfg)
        val con2 = LoadTime.calculate(kangoo, 2, listOf(FloorInfo(Access.STAIRS, 3), FloorInfo()), cfg)
        assertEquals(50, solo.stops[0].minutes) // 30 + 3 × 6 = 48 → 50
        assertEquals(30, solo.stops[1].minutes)
        assertEquals(40, con1.stops[0].minutes) // 18 + 18 = 36 → 40
        assertEquals(20, con1.stops[1].minutes) // 18 → 20
        assertTrue(con2.totalMinutes < con1.totalMinutes && con1.totalMinutes < solo.totalMinutes)
        assertTrue(solo.stops[0].detail.contains("3 pisos por escalera"))
    }

    @Test fun `ascensor y paradas intermedias`() {
        val lt = LoadTime.calculate(kangoo, 0, listOf(FloorInfo(), FloorInfo(Access.ELEVATOR, 5), FloorInfo()), cfg)
        assertEquals(30, lt.stops[0].minutes)
        assertEquals(25, lt.stops[1].minutes) // 15 (mitad) + 10 ascensor
        assertEquals(30, lt.stops[2].minutes)
        assertEquals(85, lt.totalMinutes)
        assertEquals("1 h 25 min", LoadTime.format(85))
        assertEquals("2 h", LoadTime.format(120))
        assertEquals("45 min", LoadTime.format(45))
    }
}

class PriceSheetTest {
    @Test
    fun formAndCsvRoundTrip() {
        val pricing = PricingConfig(perHelper = 15000, perHelperKm = 500, perStairFloor = 6000, perExtraStop = 8000, roundTo = 500, urbanDistanceFactor = 1.3, minKm = 2.5, operatingPct = 0.2, commissionPct = 0.15)
        val vehicles = listOf(VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", baseFare = 25000, perKm = 1800))
        val times = TimeConfig(stairMinutesPerFloor = 6, elevatorMinutes = 10, intermediateFactor = 0.5, waitFeePer15Min = 6000)
        val values = PriceSheet.currentValues(pricing, vehicles, times)
        assertEquals("2.5", values["km_minimo"])
        assertEquals("15", values["comision_porcentaje"])
        assertEquals("25000", values["kangoo.tarifa_base"])
        val groups = PriceSheet.form(vehicles, advanced = true)
        // Todos los campos numéricos del formulario tienen valor actual
        groups.flatMap { it.fields }.filter { !it.text }.forEach { f -> assertTrue(f.key, values.containsKey(f.key)) }
        assertTrue(PriceSheet.form(vehicles, advanced = false).none { it.title == "Avanzado" })
        // Edito, armo el CSV y lo aplico
        val edited = values.toMutableMap().apply { put("kangoo.por_km", "2.000"); put("km_minimo", "3,5"); put("por_ayudante", "abc") }
        val csv = PriceSheet.toCsv(edited)
        assertFalse(csv.contains("por_ayudante,abc"))
        val r = PriceSheet.apply(csv, pricing, vehicles, times)
        assertEquals(2000, r.vehicles[0].perKm)
        assertEquals(3.5, r.pricing.minKm, 1e-9)
        assertEquals(15000, r.pricing.perHelper)
        assertTrue(PriceSheet.isNumber("$ 25.000")); assertFalse(PriceSheet.isNumber("veinte"))
    }

    @Test
    fun severalWhatsApps() {
        assertEquals(listOf("5491122334455", "5492474123456"), PriceSheet.splitPhones("5491122334455, 549 2474 12-3456; 5491122334455"))
        assertTrue(PriceSheet.isValid("whatsapp", "5491122334455, 5492474123456"))
        assertFalse(PriceSheet.isValid("whatsapp", "5491122334455, 12"))
        val b = PriceSheet.branding("whatsapp,5491122334455 / 5492474123456")
        assertEquals(2, b.whatsapps.size)
        assertEquals("5491122334455, 5492474123456", PriceSheet.brandingValues(b)["whatsapp"])
    }

    @Test
    fun weekendSurcharge() {
        val cfg = PricingConfig(perHelper = 0, perHelperKm = 0, perStairFloor = 0, perExtraStop = 0, roundTo = 1, urbanDistanceFactor = 1.0, minKm = 1.0, weekendPct = 0.20)
        val v = VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", baseFare = 10000, perKm = 1000)
        val weekday = PriceCalculator.calculate(v, 10.0, 0, listOf(FloorInfo(), FloorInfo()), cfg, weekend = false)
        val weekend = PriceCalculator.calculate(v, 10.0, 0, listOf(FloorInfo(), FloorInfo()), cfg, weekend = true)
        assertEquals(20000, weekday.total); assertEquals(0, weekday.weekend)
        assertEquals(24000, weekend.total); assertEquals(4000, weekend.weekend)
        assertEquals(weekend.total, weekend.base + weekend.distance + weekend.helpers + weekend.stairs + weekend.extraStops + weekend.weekend)
        // Planilla
        val r = PriceSheet.apply("recargo_fin_de_semana_porcentaje,15", cfg, listOf(v), TimeConfig(6, 10, 0.5, 6000))
        assertEquals(0.15, r.pricing.weekendPct, 1e-9)
        // Sábado 8/11/2025 y domingo; lunes no
        assertTrue(Schedule(DayDate(2025, 11, 8)).isWeekend()); assertTrue(Schedule(DayDate(2025, 11, 9)).isWeekend())
        assertFalse(Schedule(DayDate(2025, 11, 10)).isWeekend())
        assertTrue(Schedule(null).isWeekend(today = DayDate(2025, 11, 9))); assertFalse(Schedule(null).isWeekend(today = DayDate(2025, 11, 5)))
    }

    @Test
    fun minimumFareAndRoundTrip() {
        val cfg = PricingConfig(perHelper = 10000, perHelperKm = 0, perStairFloor = 0, perExtraStop = 0, roundTo = 1, urbanDistanceFactor = 1.0, minKm = 1.0, roundTripDiscountPct = 0.5)
        val v = VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", baseFare = 10000, perKm = 1000, minFare = 25000)
        val floors = listOf(FloorInfo(), FloorInfo())
        val short = PriceCalculator.calculate(v, 2.0, 0, floors, cfg)
        assertTrue(short.minimumApplied); assertEquals(25000, short.total)
        assertEquals(short.total, short.base + short.distance + short.helpers + short.stairs + short.extraStops + short.weekend + short.returnTrip)
        val oneWay = PriceCalculator.calculate(v, 30.0, 1, floors, cfg)
        assertFalse(oneWay.minimumApplied); assertEquals(50000, oneWay.total) // 10000 + 30000 + 10000
        val back = PriceCalculator.calculate(v, 30.0, 1, floors, cfg, roundTrip = true)
        assertEquals(70000, back.total) // + (30000 + 10000) × 0.5
        assertEquals(20000, back.returnTrip)
        // Planilla
        val r = PriceSheet.apply("kangoo.precio_minimo,30000\ndescuento_ida_vuelta_porcentaje,25", cfg, listOf(v), TimeConfig(6, 10, 0.5, 6000))
        assertEquals(30000, r.vehicles[0].minFare); assertEquals(0.25, r.pricing.roundTripDiscountPct, 1e-9)
        assertTrue(PriceSheet.currentValues(cfg, listOf(v), TimeConfig(6, 10, 0.5, 6000)).containsKey("kangoo.precio_minimo"))
    }

    @Test
    fun orderStatus() {
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.parse("confirmado"))
        assertEquals(OrderStatus.ON_THE_WAY, OrderStatus.parse("En camino"))
        assertEquals(OrderStatus.DONE, OrderStatus.parse(" hecho "))
        assertNull(OrderStatus.parse("cualquier cosa")); assertNull(OrderStatus.parse(null))
    }

    @Test
    fun vehiclesEditableAndExtras() {
        val cfg = PricingConfig(perHelper = 0, perHelperKm = 0, perStairFloor = 0, perExtraStop = 0, roundTo = 1, urbanDistanceFactor = 1.0, minKm = 1.0)
        val vs = listOf(
            VehicleSpec(VehicleKind.KANGOO, "Kangoo", "Utilitario", "1,8 m", "650 kg", 25000, 1800),
            VehicleSpec(VehicleKind.PICKUP, "Pickup", "Caja abierta", "1,5 m", "1.000 kg", 30000, 2000),
        )
        val csv = "kangoo.nombre,Kangoo Sr. Pérez\nkangoo.medidas,2 × 1,2 m\npickup.activo,0\nextra1.activo,1\nextra1.nombre,Camión con hidrogrúa\nextra1.icono,camion_playo\nextra1.tarifa_base,80000\nextra1.por_km,4000\n"
        val r = PriceSheet.apply(csv, cfg, vs, TimeConfig(6, 10, 0.5, 6000))
        assertEquals(3, r.vehicles.size)
        assertEquals("Kangoo Sr. Pérez", r.vehicles[0].name); assertEquals("2 × 1,2 m", r.vehicles[0].dimensions); assertTrue(r.vehicles[0].enabled)
        assertFalse(r.vehicles[1].enabled)
        val extra = r.vehicles[2]
        assertEquals("extra1", extra.id); assertEquals(VehicleKind.CAMION_PLAYO, extra.kind); assertEquals("Camión con hidrogrúa", extra.name); assertEquals(80000, extra.baseFare); assertTrue(extra.enabled)
        // Ida y vuelta por la planilla: lo que se arma se vuelve a leer igual
        val values = PriceSheet.currentValues(cfg, r.vehicles, TimeConfig(6, 10, 0.5, 6000))
        assertEquals("0", values["pickup.activo"]); assertEquals("camion_playo", values["extra1.icono"])
        val again = PriceSheet.apply(PriceSheet.toCsv(values), cfg, vs, TimeConfig(6, 10, 0.5, 6000))
        assertEquals(listOf("Kangoo Sr. Pérez", "Pickup", "Camión con hidrogrúa"), again.vehicles.map { it.name })
        assertEquals(listOf(true, false, true), again.vehicles.map { it.enabled })
        // El formulario muestra los extras presentes en los valores
        val groups = PriceSheet.form(vs, advanced = false, values = values)
        assertTrue(groups.any { it.vehicleId == "extra1" && it.title == "Camión con hidrogrúa" })
        assertTrue(groups.first { it.vehicleId == "kangoo" }.fields.any { it.bool })
        assertTrue(PriceSheet.isValid("extra1.nombre", "Camión")); assertFalse(PriceSheet.isValid("extra1.nombre", "")); assertTrue(PriceSheet.isValid("kangoo.activo", "0"))
    }

    @Test
    fun brandingKeys() {
        val csv = "clave,valor\nnombre,Fletes Don Pedro, Salto\nwhatsapp,+54 9 2474 12-3456\ncolor_primario,0e7c4a\ncolor_acento,#ZZZ\nlogo_url,https://x/y.png\nkangoo.por_km,1500\n"
        val b = PriceSheet.branding(csv)
        assertEquals("Fletes Don Pedro, Salto", b.name)
        assertEquals(listOf("5492474123456"), b.whatsapps)
        assertEquals("#0E7C4A", b.primary)
        assertNull(b.accent)
        assertEquals("https://x/y.png", b.logoUrl)
        // Las claves de texto no rompen los números y vuelven a salir en el CSV
        val out = PriceSheet.toCsv(linkedMapOf("nombre" to "Don Pedro", "kangoo.por_km" to "1.500", "color_primario" to "", "por_ayudante" to "x"))
        assertTrue(out.contains("nombre,Don Pedro")); assertTrue(out.contains("kangoo.por_km,1.500"))
        assertFalse(out.contains("color_primario")); assertFalse(out.contains("por_ayudante"))
        assertTrue(PriceSheet.isValid("nombre", "Fletes Pepe")); assertFalse(PriceSheet.isValid("nombre", "F")); assertTrue(PriceSheet.isValid("nombre", ""))
        assertTrue(PriceSheet.isValid("whatsapp", "")); assertFalse(PriceSheet.isValid("whatsapp", "123"))
        assertTrue(PriceSheet.form(emptyList(), advanced = false).first().fields.all { it.text })
    }

    private val pricing = PricingConfig(15000, 500, 6000, 8000, 500, 1.3, 2.0, operatingPct = 0.2, commissionPct = 0.15)
    private val times = TimeConfig(6, 10, 0.5, 6000)
    private val vehicles = listOf(
        VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", 25000, 1800, 30, 3.0),
        VehicleSpec(VehicleKind.FURGON_MEDIANO, "Furgón mediano", "", "", "", 35000, 2300, 45, 10.0),
    )

    @Test fun `aplica solo lo que esta en la planilla`() {
        val csv = """clave,valor
            |por_ayudante,"18.000"
            |kangoo.tarifa_base,${'$'} 27.500
            |furgon_mediano.por_km,2600
            |comision_porcentaje,0
            |recargo_operativo_porcentaje,0
            |espera_por_15_min,7000
            |clave_desconocida,123
            |""".trimMargin()
        val r = PriceSheet.apply(csv, pricing, vehicles, times)
        assertEquals(18000, r.pricing.perHelper)
        assertEquals(500, r.pricing.perHelperKm) // no estaba: queda igual
        assertEquals(27500, r.vehicles[0].baseFare)
        assertEquals(1800, r.vehicles[0].perKm)
        assertEquals(2600, r.vehicles[1].perKm)
        assertEquals(0.0, r.pricing.commissionPct, 1e-9)
        assertEquals(0.0, r.pricing.operatingPct, 1e-9)
        assertEquals(7000, r.times.waitFeePer15Min)
        assertEquals(6, r.applied)
    }

    @Test fun `numeros con coma punto y pesos`() {
        val r = PriceSheet.apply("km_minimo;2,5\nredondeo;1.000\n", pricing, vehicles, times)
        assertEquals(2.5, r.pricing.minKm, 1e-9)
        assertEquals(1000, r.pricing.roundTo)
    }

    @Test fun `la plantilla se puede volver a aplicar sin cambios`() {
        val csv = PriceSheet.template(pricing, vehicles, times)
        val r = PriceSheet.apply(csv, pricing, vehicles, times)
        assertEquals(pricing.perHelper, r.pricing.perHelper)
        assertEquals(vehicles[1].baseFare, r.vehicles[1].baseFare)
        assertTrue(csv.contains("kangoo.tarifa_base,25000"))
    }
}

class OwnerNotesTest {
    @Test fun `la comision solo aparece en el mensaje privado`() {
        val cfg = PricingConfig(15000, 500, 6000, 8000, 500, 1.3, 2.0, operatingPct = 0.2, commissionPct = 0.15)
        val vehicle = VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", 25000, 1800)
        val price = PriceCalculator.calculate(vehicle, 10.0, 0, listOf(FloorInfo(), FloorInfo()), cfg)
        val order = OrderSummary(
            "Fletaba", "Ana", "5491123456789",
            listOf(
                Stop(AddressPoint("Lima 29, Monserrat", LatLng(-34.61, -58.38), true, insideOrigin = true), FloorInfo()),
                Stop(AddressPoint("Av. Cabildo 2000", LatLng(-34.56, -58.45), true), FloorInfo()),
            ),
            vehicle, 0, "", Schedule(), 10.0, price,
        )
        val public = OrderMessage.build(order, DayDate(2026, 10, 20), ownerNotes = false)
        val private = OrderMessage.build(order, DayDate(2026, 10, 20), ownerNotes = true)
        assertFalse(public.contains("Comisión"))
        assertTrue(private.contains("Comisión: ${Money.formatArs(price.commission)}"))
        assertTrue(private.contains("Neto para el fletero"))
    }
}

class OrderMessageTest {
    @Test fun `mensaje contiene todos los datos`() {
        val config = PricingConfig(15000, 500, 6000, 8000, 500, 1.3, 2.0)
        val vehicle = VehicleSpec(VehicleKind.FURGON_MEDIANO, "Furgón mediano (Sprinter)", "", "", "", 35000, 2300)
        val price = PriceCalculator.calculate(vehicle, 7.8, 1, listOf(FloorInfo(Access.STAIRS, 3), FloorInfo(), FloorInfo()), config)
        val msg = OrderMessage.build(
            OrderSummary(
                businessName = "Fletaba",
                customerName = "Juan",
                customerPhone = "5491123456789",
                stops = listOf(
                    Stop(AddressPoint("Av. Corrientes 1234, Buenos Aires", LatLng(-34.6037, -58.3850), true), FloorInfo(Access.STAIRS, 3)),
                    Stop(AddressPoint("Av. Santa Fe 3000, Buenos Aires", LatLng(-34.5880, -58.4100), true), FloorInfo()),
                    Stop(AddressPoint("Av. Cabildo 2000, Buenos Aires", LatLng(-34.5610, -58.4560), true), FloorInfo()),
                ),
                vehicle = vehicle,
                helpers = 1,
                cargo = "heladera y 10 cajas",
                schedule = Schedule(DayDate(2026, 10, 30), TimeSlot.MORNING),
                distanceKm = 7.8,
                price = price,
            ),
            today = DayDate(2026, 10, 20),
        )
        assertFalse("la parte privada no va al cliente", msg.contains("Comisión"))
        listOf(
            "Juan", "+54 9 11 2345-6789", "Origen: Av. Corrientes 1234", "Parada 1: Av. Santa Fe 3000", "Destino: Av. Cabildo 2000",
            "Furgón mediano (Sprinter)", "Ayudantes: 1", "Piso 3 por escalera", "Planta baja",
            "heladera y 10 cajas", "7,8 km", Money.formatArs(price.total),
            "viernes 30 de octubre, por la mañana (8 a 12 h)",
        ).forEach { assertTrue("falta: $it", msg.contains(it)) }
    }
}

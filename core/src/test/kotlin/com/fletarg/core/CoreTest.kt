package com.fletarg.core

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

class CabaZoneTest {
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
    private val config = PricingConfig(perHelper = 25000, perStairFloor = 6000, perExtraStop = 8000, roundTo = 500, urbanDistanceFactor = 1.3, minKm = 2.0)
    private val kangoo = VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", baseFare = 25000, perKm = 1800)

    @Test fun `formula completa`() {
        val p = PriceCalculator.calculate(
            vehicle = kangoo,
            distanceKm = 7.8,
            helpers = 1,
            floors = listOf(FloorInfo(Access.STAIRS, 3), FloorInfo(Access.ELEVATOR, 10)),
            config = config,
        )
        assertEquals(25000, p.base)
        assertEquals(0, p.extraStopCount)
        assertEquals(14040, p.distance)
        assertEquals(25000, p.helpers)
        assertEquals(3, p.stairFloors)
        assertEquals(18000, p.stairs)
        // 82040 redondeado hacia arriba a 500
        assertEquals(82500, p.total)
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

class OrderMessageTest {
    @Test fun `mensaje contiene todos los datos`() {
        val config = PricingConfig(25000, 6000, 8000, 500, 1.3, 2.0)
        val vehicle = VehicleSpec(VehicleKind.FURGON_MEDIANO, "Furgón mediano (Sprinter)", "", "", "", 35000, 2300)
        val price = PriceCalculator.calculate(vehicle, 7.8, 1, listOf(FloorInfo(Access.STAIRS, 3), FloorInfo(), FloorInfo()), config)
        val msg = OrderMessage.build(
            OrderSummary(
                businessName = "Fletarg",
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
        listOf(
            "Juan", "+54 9 11 2345-6789", "Origen: Av. Corrientes 1234", "Parada 1: Av. Santa Fe 3000", "Destino: Av. Cabildo 2000",
            "Furgón mediano (Sprinter)", "Ayudantes: 1", "Piso 3 por escalera", "Planta baja",
            "heladera y 10 cajas", "7,8 km", Money.formatArs(price.total),
            "viernes 30 de octubre, por la mañana (8 a 12 h)",
        ).forEach { assertTrue("falta: $it", msg.contains(it)) }
    }
}

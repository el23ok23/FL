package com.mifleteciudad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceZoneTest {
    private val CabaZone = ServiceZone.CABA

    @Test fun `puntos dentro de la ciudad`() {
        assertTrue(CabaZone.contains(LatLng(-34.6037, -58.3816))) // Obelisco
        assertTrue(CabaZone.contains(LatLng(-34.5875, -58.4300))) // Palermo
        assertTrue(CabaZone.contains(LatLng(-34.6280, -58.4700))) // Flores
        assertTrue(CabaZone.contains(LatLng(-34.5500, -58.4620))) // Núñez
        assertTrue(CabaZone.contains(LatLng(-34.6500, -58.4900))) // Mataderos
        assertTrue(CabaZone.contains(LatLng(-34.6350, -58.3630))) // La Boca
    }

    @Test fun `puntos fuera de la ciudad`() {
        assertFalse(CabaZone.contains(LatLng(-34.5260, -58.4800))) // Vicente López
        assertFalse(CabaZone.contains(LatLng(-34.6600, -58.3650))) // Avellaneda
        assertFalse(CabaZone.contains(LatLng(-34.6300, -58.5500))) // Ciudadela
        assertFalse(CabaZone.contains(LatLng(-34.7200, -58.4700))) // Lomas de Zamora
        assertFalse(CabaZone.contains(LatLng(-34.4700, -58.5100))) // San Isidro
        assertFalse(CabaZone.contains(LatLng(-34.9200, -57.9500))) // La Plata
    }

    @Test fun `texto administrativo manda sobre el poligono`() {
        val obelisco = LatLng(-34.6037, -58.3816)
        assertFalse(CabaZone.isInside(obelisco, listOf("Provincia de Buenos Aires")))
        assertTrue(CabaZone.isInside(LatLng(-34.6600, -58.3650), listOf("Cdad. Autónoma de Buenos Aires")))
        assertTrue(CabaZone.isInside(obelisco, listOf(null, "")))
    }

    @Test fun `zona de salto`() {
        val salto = ServiceZone.SALTO
        assertTrue(salto.contains(LatLng(-34.2925, -60.2550))) // Salto centro
        assertTrue(salto.contains(LatLng(-34.3764, -60.2664))) // Berdier
        assertTrue(salto.contains(LatLng(-34.1100, -60.4000))) // Arroyo Dulce
        assertFalse(salto.contains(LatLng(-33.8900, -60.5700))) // Pergamino
        assertFalse(salto.contains(LatLng(-34.2460, -59.4700))) // San Antonio de Areco
        // En Salto "Provincia de Buenos Aires" no es motivo para quedar afuera.
        assertTrue(salto.isInside(LatLng(-34.2925, -60.2550), listOf("Provincia de Buenos Aires")))
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
    private val config = PricingConfig(perHelper = 25000, perStairFloor = 6000, roundTo = 500, urbanDistanceFactor = 1.3, minKm = 2.0)
    private val kangoo = VehicleSpec(VehicleKind.KANGOO, "Kangoo", "", "", "", baseFare = 25000, perKm = 1800)

    @Test fun `formula completa`() {
        val p = PriceCalculator.calculate(
            vehicle = kangoo,
            distanceKm = 7.8,
            helpers = 1,
            origin = FloorInfo(Access.STAIRS, 3),
            destination = FloorInfo(Access.ELEVATOR, 10),
            config = config,
        )
        assertEquals(25000, p.base)
        assertEquals(14040, p.distance)
        assertEquals(25000, p.helpers)
        assertEquals(3, p.stairFloors)
        assertEquals(18000, p.stairs)
        // 82040 redondeado hacia arriba a 500
        assertEquals(82500, p.total)
    }

    @Test fun `km minimo y sin extras`() {
        val p = PriceCalculator.calculate(kangoo, 0.4, 0, FloorInfo(), FloorInfo(), config)
        assertEquals(2.0, p.km, 0.0)
        // 25.000 + 3.600 = 28.600 → redondeado hacia arriba a 29.000
        assertEquals(29000, p.total)
    }

    @Test fun `ayudantes se limitan a dos`() {
        val p = PriceCalculator.calculate(kangoo, 2.0, 7, FloorInfo(), FloorInfo(), config)
        assertEquals(2, p.helperCount)
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

class OrderMessageTest {
    @Test fun `mensaje contiene todos los datos`() {
        val config = PricingConfig(25000, 6000, 500, 1.3, 2.0)
        val vehicle = VehicleSpec(VehicleKind.FURGON_MEDIANO, "Furgón mediano (Sprinter)", "", "", "", 35000, 2300)
        val price = PriceCalculator.calculate(vehicle, 7.8, 1, FloorInfo(Access.STAIRS, 3), FloorInfo(), config)
        val msg = OrderMessage.build(
            OrderSummary(
                businessName = "Mi Flete Ciudad",
                customerName = "Juan",
                customerPhone = "5491123456789",
                origin = AddressPoint("Av. Corrientes 1234, CABA", LatLng(-34.6037, -58.3850), true),
                destination = AddressPoint("Av. Cabildo 2000, CABA", LatLng(-34.5610, -58.4560), true),
                vehicle = vehicle,
                helpers = 1,
                originFloor = FloorInfo(Access.STAIRS, 3),
                destinationFloor = FloorInfo(),
                cargo = "heladera y 10 cajas",
                distanceKm = 7.8,
                price = price,
            )
        )
        listOf(
            "Juan", "+54 9 11 2345-6789", "Av. Corrientes 1234", "Av. Cabildo 2000",
            "Furgón mediano (Sprinter)", "Ayudantes: 1", "Piso 3 por escalera", "Planta baja",
            "heladera y 10 cajas", "7,8 km", Money.formatArs(price.total),
        ).forEach { assertTrue("falta: $it", msg.contains(it)) }
    }
}

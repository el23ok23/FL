package com.fletaba.app.data

import android.content.Context
import android.util.Log
import com.fletaba.core.ArgentinePhone
import com.fletaba.core.PricingConfig
import com.fletaba.core.TimeConfig
import com.fletaba.core.VehicleKind
import com.fletaba.core.VehicleSpec
import org.json.JSONObject

/**
 * Configuración de la app, leída de assets/config.json.
 * Si el archivo tiene un error, se usan valores por defecto para que la app siga andando.
 */
data class AppConfig(
    val businessName: String,
    val ownerWhatsApp: String?,
    val privacyUrl: String?,
    val webhookUrl: String?,
    val callMeBotKey: String?,
    val googleApiKey: String?,
    /** Clave para Maps Static API (si está vacía se usa googleApiKey). */
    val staticMapsKey: String?,
    val pricing: PricingConfig,
    val times: TimeConfig,
    val vehicles: List<VehicleSpec>,
) {
    companion object {
        private const val TAG = "AppConfig"

        fun load(context: Context): AppConfig = try {
            val text = context.assets.open("config.json").bufferedReader().use { it.readText() }
            parse(text)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer config.json, se usan valores por defecto", e)
            defaults()
        }

        fun parse(json: String): AppConfig {
            val root = JSONObject(json)
            val negocio = root.optJSONObject("negocio") ?: JSONObject()
            val envio = root.optJSONObject("envio_pedido") ?: JSONObject()
            val google = root.optJSONObject("google") ?: JSONObject()
            val precios = root.optJSONObject("precios") ?: JSONObject()
            val tiempos = root.optJSONObject("tiempos") ?: JSONObject()

            val vehicles = ArrayList<VehicleSpec>()
            root.optJSONArray("vehiculos")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val v = arr.optJSONObject(i) ?: continue
                    val kind = VehicleKind.fromId(v.optString("id")) ?: continue
                    vehicles += VehicleSpec(
                        kind = kind,
                        name = v.optString("nombre", kind.name),
                        description = v.optString("descripcion", ""),
                        dimensions = v.optString("medidas", ""),
                        capacity = v.optString("carga", ""),
                        baseFare = v.optInt("tarifa_base", 0),
                        perKm = v.optInt("por_km", 0),
                        loadMinutes = v.optInt("minutos_carga", DEFAULT_LOAD_MINUTES[kind] ?: 45),
                        volumeM3 = v.optDouble("m3", DEFAULT_M3[kind] ?: 10.0),
                    )
                }
            }

            val ownerRaw = clean(negocio.optString("whatsapp_dueno"))
            val owner = ownerRaw?.let { ArgentinePhone.normalize(it) ?: it.filter(Char::isDigit).ifBlank { null } }

            return AppConfig(
                businessName = clean(negocio.optString("nombre")) ?: "Fletaba",
                ownerWhatsApp = owner,
                privacyUrl = clean(negocio.optString("politica_privacidad_url")),
                webhookUrl = clean(envio.optString("webhook_url"))?.takeIf { it.startsWith("https://") },
                callMeBotKey = clean(envio.optString("callmebot_apikey")),
                googleApiKey = clean(google.optString("api_key")),
                staticMapsKey = clean(google.optString("static_maps_key")) ?: clean(google.optString("api_key")),
                pricing = PricingConfig(
                    perHelper = precios.optInt("por_ayudante", 15000),
                    perHelperKm = precios.optInt("por_ayudante_km", 500),
                    perStairFloor = precios.optInt("por_piso_escalera", 6000),
                    perExtraStop = precios.optInt("por_parada_extra", 8000),
                    roundTo = precios.optInt("redondeo", 500),
                    urbanDistanceFactor = precios.optDouble("factor_distancia_urbana", 1.3),
                    minKm = precios.optDouble("km_minimo", 2.0),
                    operatingPct = precios.optDouble("recargo_operativo_porcentaje", 20.0) / 100.0,
                    commissionPct = precios.optDouble("comision_porcentaje", 15.0) / 100.0,
                ),
                times = TimeConfig(
                    stairMinutesPerFloor = tiempos.optInt("minutos_por_piso_escalera", 6),
                    elevatorMinutes = tiempos.optInt("minutos_ascensor", 10),
                    intermediateFactor = tiempos.optDouble("factor_parada_intermedia", 0.5),
                    waitFeePer15Min = tiempos.optInt("espera_por_15_min", 6000),
                ),
                vehicles = vehicles.ifEmpty { defaults().vehicles },
            )
        }

        private val DEFAULT_M3 = mapOf(
            VehicleKind.KANGOO to 3.0, VehicleKind.FURGON_MEDIANO to 10.0, VehicleKind.FURGON_GRANDE to 15.0,
            VehicleKind.PICKUP to 2.5, VehicleKind.CAMION_3_4 to 22.0, VehicleKind.CAMION_MEDIANO_CERRADO to 35.0,
            VehicleKind.CAMION_GRANDE_CERRADO to 52.0, VehicleKind.CAMION_PLAYO to 20.0,
        )
        private val DEFAULT_LOAD_MINUTES = mapOf(
            VehicleKind.KANGOO to 30, VehicleKind.FURGON_MEDIANO to 45, VehicleKind.FURGON_GRANDE to 60,
            VehicleKind.PICKUP to 30, VehicleKind.CAMION_3_4 to 75, VehicleKind.CAMION_MEDIANO_CERRADO to 100,
            VehicleKind.CAMION_GRANDE_CERRADO to 140, VehicleKind.CAMION_PLAYO to 90,
        )

        /** Valores vacíos o marcados como "COMPLETAR_..." cuentan como no configurados. */
        private fun clean(value: String?): String? =
            value?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("COMPLETAR", ignoreCase = true) }

        fun defaults(): AppConfig = AppConfig(
            businessName = "Fletaba",
            ownerWhatsApp = null,
            privacyUrl = null,
            webhookUrl = null,
            callMeBotKey = null,
            googleApiKey = null,
            staticMapsKey = null,
            pricing = PricingConfig(15000, 500, 6000, 8000, 500, 1.3, 2.0, operatingPct = 0.20, commissionPct = 0.15),
            times = TimeConfig(6, 10, 0.5, 6000),
            vehicles = listOf(
                VehicleSpec(VehicleKind.KANGOO, "Kangoo", "Utilitario chico", "1,8 × 1,2 × 1,1 m", "Hasta 650 kg · 3 m³", 25000, 1800, 30, 3.0),
                VehicleSpec(VehicleKind.FURGON_MEDIANO, "Furgón mediano", "Tipo Sprinter", "3,2 × 1,7 × 1,8 m", "Hasta 1.500 kg · 10 m³", 35000, 2300, 45, 10.0),
                VehicleSpec(VehicleKind.FURGON_GRANDE, "Furgón grande", "Techo alto", "4,3 × 1,8 × 2,0 m", "Hasta 1.800 kg · 15 m³", 45000, 2800, 60, 15.0),
                VehicleSpec(VehicleKind.PICKUP, "Camioneta pickup", "Caja abierta", "Caja 1,5 × 1,5 × 0,5 m", "Hasta 1.000 kg", 30000, 2000, 30, 2.5),
                VehicleSpec(VehicleKind.CAMION_3_4, "Camión 3/4", "Caja cerrada chica", "4,5 × 2,2 × 2,2 m", "Hasta 3.500 kg · 22 m³", 65000, 3500, 75, 22.0),
                VehicleSpec(VehicleKind.CAMION_MEDIANO_CERRADO, "Camión mediano", "Caja cerrada", "6,0 × 2,4 × 2,4 m", "Hasta 6.000 kg · 35 m³", 90000, 4500, 100, 35.0),
                VehicleSpec(VehicleKind.CAMION_GRANDE_CERRADO, "Camión grande", "Caja cerrada", "8,0 × 2,5 × 2,6 m", "Hasta 10.000 kg · 52 m³", 130000, 6000, 140, 52.0),
                VehicleSpec(VehicleKind.CAMION_PLAYO, "Camión playo", "Caja abierta", "7,0 × 2,5 m de plataforma", "Hasta 8.000 kg", 100000, 5000, 90, 20.0),
            ),
        )
    }
}

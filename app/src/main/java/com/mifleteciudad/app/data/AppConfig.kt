package com.mifleteciudad.app.data

import android.content.Context
import android.util.Log
import com.mifleteciudad.core.ArgentinePhone
import com.mifleteciudad.core.LatLng
import com.mifleteciudad.core.PricingConfig
import com.mifleteciudad.core.ServiceZone
import com.mifleteciudad.core.VehicleKind
import com.mifleteciudad.core.VehicleSpec
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
    val pricing: PricingConfig,
    val vehicles: List<VehicleSpec>,
    val zone: ServiceZone,
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
            val zona = root.optJSONObject("zona")

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
                    )
                }
            }

            val ownerRaw = clean(negocio.optString("whatsapp_dueno"))
            val owner = ownerRaw?.let { ArgentinePhone.normalize(it) ?: it.filter(Char::isDigit).ifBlank { null } }

            return AppConfig(
                businessName = clean(negocio.optString("nombre")) ?: "Mi Flete Ciudad",
                ownerWhatsApp = owner,
                privacyUrl = clean(negocio.optString("politica_privacidad_url")),
                webhookUrl = clean(envio.optString("webhook_url"))?.takeIf { it.startsWith("https://") },
                callMeBotKey = clean(envio.optString("callmebot_apikey")),
                googleApiKey = clean(google.optString("api_key")),
                pricing = PricingConfig(
                    perHelper = precios.optInt("por_ayudante", 25000),
                    perStairFloor = precios.optInt("por_piso_escalera", 6000),
                    roundTo = precios.optInt("redondeo", 500),
                    urbanDistanceFactor = precios.optDouble("factor_distancia_urbana", 1.3),
                    minKm = precios.optDouble("km_minimo", 2.0),
                ),
                vehicles = vehicles.ifEmpty { defaults().vehicles },
                zone = zona?.let(::parseZone) ?: ServiceZone.CABA,
            )
        }

        private fun parseZone(z: JSONObject): ServiceZone? {
            val polygon = ArrayList<LatLng>()
            z.optJSONArray("poligono")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val pt = arr.optJSONArray(i) ?: continue
                    if (pt.length() >= 2) polygon += LatLng(pt.getDouble(0), pt.getDouble(1))
                }
            }
            if (polygon.size < 3) return null
            fun words(key: String): List<String> {
                val arr = z.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }
            return ServiceZone(
                name = z.optString("nombre").ifBlank { "la zona de cobertura" },
                suffix = z.optString("sufijo"),
                searchHint = z.optString("busqueda_extra"),
                polygon = polygon,
                insideKeywords = words("palabras_dentro"),
                outsideKeywords = words("palabras_fuera"),
            )
        }

        /** Valores vacíos o marcados como "COMPLETAR_..." cuentan como no configurados. */
        private fun clean(value: String?): String? =
            value?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("COMPLETAR", ignoreCase = true) }

        fun defaults(): AppConfig = AppConfig(
            businessName = "Mi Flete Ciudad",
            ownerWhatsApp = null,
            privacyUrl = null,
            webhookUrl = null,
            callMeBotKey = null,
            googleApiKey = null,
            pricing = PricingConfig(25000, 6000, 500, 1.3, 2.0),
            zone = ServiceZone.CABA,
            vehicles = listOf(
                VehicleSpec(VehicleKind.KANGOO, "Kangoo", "Utilitario chico", "1,8 × 1,2 × 1,1 m", "Hasta 650 kg · 3 m³", 25000, 1800),
                VehicleSpec(VehicleKind.FURGON_MEDIANO, "Furgón mediano", "Tipo Sprinter", "3,2 × 1,7 × 1,8 m", "Hasta 1.500 kg · 10 m³", 35000, 2300),
                VehicleSpec(VehicleKind.FURGON_GRANDE, "Furgón grande", "Techo alto", "4,3 × 1,8 × 2,0 m", "Hasta 1.800 kg · 15 m³", 45000, 2800),
                VehicleSpec(VehicleKind.PICKUP, "Camioneta pickup", "Caja abierta", "Caja 1,5 × 1,5 × 0,5 m", "Hasta 1.000 kg", 30000, 2000),
                VehicleSpec(VehicleKind.CAMION_3_4, "Camión 3/4", "Caja cerrada chica", "4,5 × 2,2 × 2,2 m", "Hasta 3.500 kg · 22 m³", 65000, 3500),
                VehicleSpec(VehicleKind.CAMION_MEDIANO_CERRADO, "Camión mediano", "Caja cerrada", "6,0 × 2,4 × 2,4 m", "Hasta 6.000 kg · 35 m³", 90000, 4500),
                VehicleSpec(VehicleKind.CAMION_GRANDE_CERRADO, "Camión grande", "Caja cerrada", "8,0 × 2,5 × 2,6 m", "Hasta 10.000 kg · 52 m³", 130000, 6000),
                VehicleSpec(VehicleKind.CAMION_PLAYO, "Camión playo", "Caja abierta", "7,0 × 2,5 m de plataforma", "Hasta 8.000 kg", 100000, 5000),
            ),
        )
    }
}

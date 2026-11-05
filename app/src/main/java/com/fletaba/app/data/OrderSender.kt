package com.fletaba.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.fletaba.core.OrderMessage
import com.fletaba.core.OrderSummary
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Hace llegar el pedido al dueño. Intenta en orden:
 *  1. webhook_url (POST JSON)            → el pedido llega solo.
 *  2. CallMeBot (GET)                    → el pedido llega solo al WhatsApp del dueño.
 *  3. Abrir WhatsApp con el mensaje armado hacia el número del dueño.
 * Si no hay nada configurado, la app igual muestra la confirmación (modo demo).
 */
class OrderSender(context: Context, private val config: AppConfig) {
    private val appContext = context.applicationContext

    enum class Result { SENT, OPENED_WHATSAPP, NOT_CONFIGURED }

    /** Resultado del envío y, si el backend lo anotó, el id del pedido para seguir su estado. */
    data class Outcome(val result: Result, val orderId: String? = null)

    /**
     * Envía el pedido por el primer canal que funcione:
     *  1. El backend del dueño (Apps Script): lo anota en su planilla, le manda un correo y lo reparte
     *     por WhatsApp a los números que tengan CallMeBot. → SENT
     *  2. Webhook propio (si está). → SENT
     *  3. CallMeBot desde la app a cada número con clave. → SENT si al menos uno llegó
     *  4. Abrir WhatsApp con el mensaje armado hacia el primer número. → OPENED_WHATSAPP
     */
    suspend fun send(order: OrderSummary, numbers: List<String>, backendUrl: String?, photos: List<ByteArray> = emptyList()): Outcome {
        // Con la parte privada (comisión y neto) solo en los canales que no pasan por el teléfono del cliente.
        val message = OrderMessage.build(order)
        val ownerMessage = OrderMessage.build(order, ownerNotes = true)

        backendUrl?.let { url ->
            val body = toJson(order, ownerMessage).put("accion", "pedido").put("numeros", JSONArray(numbers))
            if (photos.isNotEmpty()) body.put("imagenes", JSONArray(photos.map { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }))
            val json = Http.postScript(url, body.toString(), timeoutMs = if (photos.isEmpty()) 20_000 else 60_000)?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (json?.optBoolean("ok") == true) return Outcome(Result.SENT, json.optString("id").takeIf { it.isNotBlank() })
            Log.w(TAG, "Falló el backend del dueño, se prueba el siguiente canal")
        }

        config.webhookUrl?.let { url ->
            val ok = Http.request(url, "POST", toJson(order, ownerMessage).toString()) != null
            if (ok) return Outcome(Result.SENT)
            Log.w(TAG, "Falló el webhook, se prueba el siguiente canal")
        }

        var delivered = 0
        numbers.forEach { n ->
            val key = config.callMeBotKeys[n] ?: return@forEach
            val url = "https://api.callmebot.com/whatsapp.php?phone=%2B$n&text=${encode(ownerMessage)}&apikey=${encode(key)}"
            if (Http.request(url) != null) delivered++ else Log.w(TAG, "Falló CallMeBot para $n")
        }
        if (delivered > 0) return Outcome(Result.SENT)

        numbers.firstOrNull()?.let { n ->
            if (openWhatsApp(n, message)) return Outcome(Result.OPENED_WHATSAPP)
        }

        Log.i(TAG, "Pedido sin canal configurado:\n$message")
        return Outcome(Result.NOT_CONFIGURED)
    }

    /** Abre WhatsApp con el mensaje armado hacia ese número. */
    fun openWhatsApp(number: String, message: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=${encode(message)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    private fun toJson(o: OrderSummary, message: String): JSONObject {
        val paradas = JSONArray()
        o.stops.forEachIndexed { i, s ->
            paradas.put(
                JSONObject()
                    .put("tipo", OrderMessage.stopTitle(i, o.stops.size))
                    .put("direccion", s.point.label)
                    .put("lat", s.point.location.lat)
                    .put("lng", s.point.location.lng)
                    .put("piso", s.floor.describe()),
            )
        }
        return JSONObject()
            .put("negocio", o.businessName)
            .put("nombre", o.customerName.trim())
            .put("whatsapp", o.customerPhone)
            .put("paradas", paradas)
            .put("origen", o.stops.first().point.label)
            .put("destino", o.stops.last().point.label)
            .put("vehiculo", o.vehicle.name)
            .put("ayudantes", o.helpers)
            .put("que_lleva", o.cargo.trim())
            .put("cuando", o.schedule.describe())
            .put("distancia_km", o.distanceKm)
            .put("precio", o.price.total)
            .put("recargo_finde", o.price.weekend)
            .put("ida_y_vuelta", o.roundTrip)
            .put("tiempo_incluido_min", o.loadTime?.totalMinutes ?: 0)
            .put("espera_por_15_min", o.waitFeePer15Min)
            .put("viaje_min", o.travelMinutes ?: -1)
            .put("comision", o.price.commission)
            .put("neto_fletero", o.price.netForDriver)
            .put("costo_base", o.price.subtotal)
            .put("mensaje", message)
    }

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val TAG = "OrderSender"
    }
}

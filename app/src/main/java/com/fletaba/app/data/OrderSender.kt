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

    suspend fun send(order: OrderSummary): Result {
        // Con la parte privada (comisión y neto) solo en los canales que no pasan por el teléfono del cliente.
        val message = OrderMessage.build(order)
        val ownerMessage = OrderMessage.build(order, ownerNotes = true)

        config.webhookUrl?.let { url ->
            val ok = Http.request(url, "POST", toJson(order, ownerMessage).toString()) != null
            if (ok) return Result.SENT
            Log.w(TAG, "Falló el webhook, se prueba el siguiente canal")
        }

        val owner = config.ownerWhatsApp
        val botKey = config.callMeBotKey
        if (owner != null && botKey != null) {
            val url = "https://api.callmebot.com/whatsapp.php?phone=%2B$owner" +
                "&text=${encode(ownerMessage)}&apikey=${encode(botKey)}"
            if (Http.request(url) != null) return Result.SENT
            Log.w(TAG, "Falló CallMeBot, se abre WhatsApp")
        }

        if (owner != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$owner?text=${encode(message)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val opened = runCatching { appContext.startActivity(intent) }.isSuccess
            if (opened) return Result.OPENED_WHATSAPP
        }

        Log.i(TAG, "Pedido sin canal configurado:\n$message")
        return Result.NOT_CONFIGURED
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

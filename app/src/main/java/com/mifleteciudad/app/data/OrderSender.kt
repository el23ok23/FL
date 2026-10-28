package com.mifleteciudad.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.mifleteciudad.core.OrderMessage
import com.mifleteciudad.core.OrderSummary
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
        val message = OrderMessage.build(order)

        config.webhookUrl?.let { url ->
            val ok = Http.request(url, "POST", toJson(order, message).toString()) != null
            if (ok) return Result.SENT
            Log.w(TAG, "Falló el webhook, se prueba el siguiente canal")
        }

        val owner = config.ownerWhatsApp
        val botKey = config.callMeBotKey
        if (owner != null && botKey != null) {
            val url = "https://api.callmebot.com/whatsapp.php?phone=%2B$owner" +
                "&text=${encode(message)}&apikey=${encode(botKey)}"
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

    private fun toJson(o: OrderSummary, message: String): JSONObject = JSONObject()
        .put("negocio", o.businessName)
        .put("nombre", o.customerName.trim())
        .put("whatsapp", o.customerPhone)
        .put("origen", o.origin.label)
        .put("origen_lat", o.origin.location.lat)
        .put("origen_lng", o.origin.location.lng)
        .put("piso_origen", o.originFloor.describe())
        .put("destino", o.destination.label)
        .put("destino_lat", o.destination.location.lat)
        .put("destino_lng", o.destination.location.lng)
        .put("piso_destino", o.destinationFloor.describe())
        .put("vehiculo", o.vehicle.name)
        .put("ayudantes", o.helpers)
        .put("que_lleva", o.cargo.trim())
        .put("distancia_km", o.distanceKm)
        .put("precio", o.price.total)
        .put("mensaje", message)

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val TAG = "OrderSender"
    }
}

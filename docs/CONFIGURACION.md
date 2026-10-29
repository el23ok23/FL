# Configuración de Fletarg

Todo lo que se personaliza está en **un solo archivo**: `app/src/main/assets/config.json`.
Abrilo con cualquier editor de texto, cambiá los valores (no las claves) y volvé a generar la app.
Los montos van en pesos, sin puntos ni decimales (`25000`, no `25.000`).

## 1. Tu WhatsApp (obligatorio)

```json
"whatsapp_dueno": "5491123456789"
```

Formato internacional sin `+`: `549` + código de área sin el 0 + número sin el 15.
Ejemplos: CABA `5491123456789`, Córdoba `5493511234567`.

## 2. Cómo te llega el pedido

La app intenta, en este orden, lo que tengas configurado:

| Canal | Qué hace | Qué completar |
|---|---|---|
| `webhook_url` | Manda el pedido en JSON a una URL `https` tuya (Make, Zapier, Google Apps Script, tu servidor). El cliente no sale de la app. | La URL. |
| `callmebot_apikey` | Servicio gratuito que te escribe por WhatsApp. El cliente no sale de la app. | Tu clave de CallMeBot. |
| Abrir WhatsApp | Si no hay ninguno de los anteriores, se abre WhatsApp en el teléfono del cliente con el mensaje ya armado hacia tu número. El cliente solo toca "enviar". | Nada más que tu número. |

**CallMeBot (recomendado, gratis, 2 minutos):**
1. Agregá a tus contactos el número **+34 644 59 79 78**.
2. Desde tu WhatsApp mandale el mensaje: `I allow callmebot to send me messages`.
3. Te responde con tu *apikey* (un número). Pegalo en `"callmebot_apikey": "123456"`.

Si CallMeBot falla en algún momento, la app pasa sola al canal siguiente (abrir WhatsApp), así que el pedido nunca se pierde.

**JSON que recibe el webhook:** `nombre`, `whatsapp`, `paradas` (lista con `tipo`, `direccion`, `lat`, `lng`, `piso`), `origen`, `destino`, `vehiculo`, `ayudantes`, `que_lleva`, `cuando`, `distancia_km`, `precio`, `mensaje` (el texto completo listo para reenviar).

## 3. Clave de Google (opcional, recomendada)

Sin clave la app **funciona igual**: usa el geocodificador del teléfono para las direcciones y estima la distancia en línea recta por un factor urbano. Con clave, el autocompletado es el de Google Maps en todo el país y la distancia es la del recorrido real por calle pasando por todas las paradas.

1. Entrá a [console.cloud.google.com](https://console.cloud.google.com), creá un proyecto y asociá facturación (Google da crédito mensual gratuito; para el volumen de una app de fletes normalmente no se paga).
2. En *APIs y servicios → Biblioteca* habilitá **Places API (New)** y **Routes API**.
3. En *Credenciales → Crear credenciales → Clave de API*, copiá la clave y pegala en `"api_key": "AIza..."`.
4. Restringí la clave (*Restricciones de aplicación → Apps para Android*): nombre del paquete `com.fletarg.app` o `com.mifletesalto.app` y la huella SHA-1 de tu clave de firma (`keytool -list -v -keystore mi-flete-ciudad.jks`). En *Restricciones de API* dejá solo las dos APIs de arriba.

## 4. Precios

```json
"precios": {
  "por_ayudante": 15000,
  "por_ayudante_km": 500,
  "por_piso_escalera": 6000,
  "por_parada_extra": 8000,
  "redondeo": 500,
  "km_minimo": 2,
  "factor_distancia_urbana": 1.3
}
```

Fórmula: **tarifa_base del vehículo + por_km × km del recorrido completo + ayudantes × (por_ayudante + por_ayudante_km × km) + por_piso_escalera × pisos por escalera (sumando todas las paradas) + por_parada_extra × paradas intermedias**, redondeado hacia arriba al múltiplo de `redondeo`.

Cada ayudante tiene una base fija (`por_ayudante`) más un valor por kilómetro (`por_ayudante_km`): para 3 km cuesta $16.500 y para 40 km, $35.000 con los valores de ejemplo.

Cada vehículo tiene `tarifa_base`, `por_km`, `nombre`, `descripcion`, `medidas` y `carga`. Podés cambiar textos y montos; el `id` no se toca. Los valores que vienen cargados corresponden al tramo alto del mercado del AMBA; para viajes largos entre ciudades conviene revisar el valor por km.

## 5. Cobertura

Todo flete **sale de Capital Federal**: el origen tiene que estar dentro de la Ciudad Autónoma de Buenos Aires (la app lo verifica con el límite de la Ciudad y con lo que informa el servicio de direcciones). Las paradas y el destino pueden ser en cualquier punto de la Argentina. Si una dirección queda fuera del país, o el origen fuera de Capital, la app avisa y no deja seguir. No hay nada que configurar.

## 6. Política de privacidad

Pegá la URL donde publiques la política (ver `docs/GOOGLE_PLAY.md`) en `"politica_privacidad_url"`.

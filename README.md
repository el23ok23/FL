# Fletaba

App Android nativa (Kotlin + Jetpack Compose) para pedir fletes desde Capital Federal a toda la Argentina.
Liviana, abre al instante, animaciones fluidas, modo claro/oscuro automático y precio al toque.
Funciona en Android 5.0 o más nuevo.

## Qué hace (5 pantallas)

1. **Recorrido**: origen en Capital Federal, destino en cualquier punto del país y hasta 4 paradas intermedias, con autocompletado y un mapa que dibuja el recorrido. Avisa si el origen no está en Capital o una dirección queda fuera del país.
2. **Vehículo y ayudantes**: carrusel con 8 vehículos (Kangoo, furgones, pickup, camiones) y 0, 1 o 2 ayudantes.
3. **Detalles**: pisos en cada parada (planta baja, ascensor o escalera), qué lleva y cuándo (día y franja horaria).
4. **Precio y contacto**: precio aproximado con desglose, nombre y WhatsApp argentino válido, enviar. No se paga nada en la app.
5. **Confirmación** animada: "¡Listo! Te contactamos en breve".

El pedido te llega a tu WhatsApp con todas las paradas, vehículo, ayudantes, pisos, qué lleva, cuándo, distancia y precio.

## Estructura

```
app/                           App Android (Compose)
app/src/main/assets/config.json  WhatsApp, canal de envío, clave de Google y precios (editable)
core/                          Lógica pura (precios, cobertura, teléfonos, fechas, mensaje) con tests
docs/                          Versión web (index.html), política de privacidad y guía de Google Play
store/                         Ícono 512 px y gráfico destacado para la ficha de Play
```

## Compilar

- **Android Studio** (Ladybug o más nuevo): abrir la carpeta, esperar la sincronización y ejecutar. Para el bundle de Play: *Build → Generate Signed App Bundle*.
- **Línea de comandos**: `./gradlew :app:assembleDebug` (APK de prueba) o `./gradlew :app:bundleRelease` (AAB).
- **GitHub Actions**: cada push compila y publica el APK con enlace directo en *Releases* (`ultima-version`), además de dejar APK y AAB en *Actions → Artifacts*.
- Solo los tests de lógica, sin SDK de Android: `./gradlew :core:test -PcoreOnly`.

## Versión web

`docs/index.html` es la app completa en versión web (misma fórmula de precio y tiempos, mapa OpenStreetMap, envío por WhatsApp).
Se publica gratis con GitHub Pages: *Settings → Pages → Deploy from a branch → esta rama, carpeta `/docs`*. Queda en `https://el23ok23.github.io/FL/`.
El número de WhatsApp y los precios se editan en el bloque `CONFIG` al principio del archivo.

## Qué completar antes de publicar

Ver **docs/CONFIGURACION.md** (número, claves y precios) y **docs/GOOGLE_PLAY.md** (firma, ficha y formulario de datos).

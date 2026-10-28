# Mi Flete Ciudad / Mi Flete Salto

App Android nativa (Kotlin + Jetpack Compose) para pedir fletes. Un mismo proyecto genera dos apps:
**Mi Flete Ciudad** (Ciudad de Buenos Aires) y **Mi Flete Salto** (Salto, Provincia de Buenos Aires).
Cada una tiene su nombre, su zona de cobertura, sus precios y su propio APK.
Liviana, abre al instante, animaciones fluidas, modo claro/oscuro automático y precio al toque. Funciona en Android 5.0 o más nuevo.

## Qué hace

1. **Origen y destino** con autocompletado y mini mapa del recorrido. Avisa si una dirección queda fuera de la zona.
2. **Vehículo** en un carrusel (Kangoo, furgón mediano, furgón grande, pickup, camión 3/4, camión mediano y grande con caja cerrada, camión playo).
3. **Ayudantes** (0, 1 o 2).
4. **Pisos** en origen y destino (planta baja, ascensor o escalera + cantidad de pisos).
5. **Qué lleva** (campo corto opcional).
6. **Precio aproximado** con desglose. No se paga nada en la app.
7. **Contacto**: nombre y WhatsApp argentino válido. El pedido te llega a tu WhatsApp.
8. **Confirmación** animada: "¡Listo! Te contactamos en breve".

## Estructura

```
app/                 App Android (Compose), común a las dos ciudades
app/src/caba/assets/config.json    Mi Flete Ciudad: WhatsApp, zona, precios
app/src/salto/assets/config.json   Mi Flete Salto: WhatsApp, zona, precios
core/                Lógica pura (precios, zona, teléfonos, mensaje) con tests
docs/                Configuración, política de privacidad y guía de Google Play
store/               Íconos 512 px y gráficos destacados para las fichas de Play
```

Para agregar otra ciudad: copiá un bloque de `productFlavors` en `app/build.gradle.kts`, creá `app/src/<ciudad>/assets/config.json` con su zona y precios, y sumá sus archivos al workflow.

## Compilar

- **Android Studio** (Ladybug o más nuevo): abrir la carpeta, esperar la sincronización y ejecutar. Para el bundle de Play: *Build → Generate Signed App Bundle*.
- **Línea de comandos**: `./gradlew :app:assembleDebug` (APK de prueba de ambas) o `./gradlew :app:bundleSaltoRelease` / `:app:bundleCabaRelease` (AAB).
- **GitHub Actions**: cada push compila las dos apps y publica los APK con enlace directo en *Releases* (`ultima-version`), además de dejar APK y AAB en *Actions → Artifacts*.
- Solo los tests de lógica, sin SDK de Android: `./gradlew :core:test -PcoreOnly`.

## Qué completar antes de publicar

Ver **docs/CONFIGURACION.md** (números, claves y precios) y **docs/GOOGLE_PLAY.md** (firma, ficha y formulario de datos).

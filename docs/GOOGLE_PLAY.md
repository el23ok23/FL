# Publicar en Google Play

Son dos apps separadas en Play Console: **Mi Flete Ciudad** (`com.mifleteciudad.app`) y **Mi Flete Salto** (`com.mifletesalto.app`). Podés usar la misma clave de firma para las dos; cada una tiene su propia ficha, sus capturas y su `.aab`.

## 1. Clave de firma (una sola vez, guardala bien)

```bash
keytool -genkeypair -v -keystore mi-flete-ciudad.jks -alias mifleteciudad \
  -keyalg RSA -keysize 2048 -validity 10000
```

Guardá el archivo `.jks` y las contraseñas en un lugar seguro: si se pierden no se pueden publicar actualizaciones.
Después copiá `keystore.properties.example` como `keystore.properties` (en la raíz del proyecto) y completalo. Ese archivo no se sube al repositorio.

Para que GitHub Actions firme el bundle por vos, cargá en *Settings → Secrets and variables → Actions* estos secrets:
`KEYSTORE_BASE64` (resultado de `base64 -w0 mi-flete-ciudad.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## 2. Generar el archivo para subir (.aab)

- Android Studio: *Build → Generate Signed App Bundle / APK → Android App Bundle*, eligiendo la variante `cabaRelease` o `saltoRelease`.
- Terminal: `./gradlew :app:bundleCabaRelease` o `./gradlew :app:bundleSaltoRelease` → `app/build/outputs/bundle/<variante>/`.
- GitHub Actions: *Actions → Android → la última ejecución → Artifacts → mi-flete-ciudad-release-aab* o *mi-flete-salto-release-aab*.

Antes de cada versión nueva subí `versionCode` (y `versionName`) en `app/build.gradle.kts`.

## 3. Política de privacidad

Play exige una URL pública. Publicá `docs/politica-de-privacidad.html` (completá los datos de contacto marcados) en cualquier hosting, por ejemplo GitHub Pages: *Settings → Pages → Deploy from a branch → carpeta /docs*. La URL queda como `https://TU_USUARIO.github.io/NOMBRE_DEL_REPO/politica-de-privacidad.html`. Pegala también en `config.json` (`politica_privacidad_url`).

## 4. Ficha de la tienda

- **Nombre**: Mi Flete Ciudad / Mi Flete Salto
- **Descripción breve** (80): Pedí tu flete en la Ciudad de Buenos Aires y conocé el precio al toque. / Pedí tu flete en Salto y conocé el precio al toque.
- **Descripción completa**: Elegí origen y destino, el vehículo (de Kangoo a camión grande), ayudantes y pisos, y mirá un precio aproximado al instante. Dejanos tu nombre y WhatsApp y te contactamos para confirmar. Sin pagos en la app. Solo dentro de la Ciudad Autónoma de Buenos Aires (o: solo en Salto, Provincia de Buenos Aires).
- **Ícono**: `store/icono-512.png` (sirve para las dos) .
- **Gráfico destacado**: `store/grafico-destacado-1024x500.png` (Ciudad) y `store/grafico-destacado-salto-1024x500.png` (Salto).
- **Capturas**: al menos 2 del teléfono (sacalas desde el emulador o un celular: direcciones, carrusel de vehículos, precio, confirmación).
- **Categoría**: Mapas y navegación (o Empresa). **Clasificación de contenido**: cuestionario estándar, sin contenido sensible.

## 5. Formulario "Seguridad de los datos" (Data safety)

| Pregunta | Respuesta |
|---|---|
| ¿Recopila o comparte datos del usuario? | Sí |
| Datos recopilados | Información personal → **Nombre**, **Número de teléfono**. Ubicación aproximada → **No** (solo direcciones escritas a mano, no se usa el GPS). |
| ¿Se encripta en tránsito? | Sí (HTTPS). |
| ¿El usuario puede pedir que se eliminen? | Sí (contacto en la política). |
| Finalidad de nombre y teléfono | Funcionalidad de la app / Comunicación con el usuario. |
| ¿Es obligatorio? | Sí, para pedir contacto (el presupuesto se ve sin cargar datos). |
| ¿Se comparten con terceros? | Si usás CallMeBot o un webhook, marcá "Sí" (proveedor de servicios). Si solo se abre WhatsApp, "No". |

## 6. Checklist final

- [ ] `config.json` con tu WhatsApp, canal de envío y (opcional) clave de Google.
- [ ] Política de privacidad publicada y con datos de contacto completos.
- [ ] `.aab` firmado y subido a *Producción* (o *Prueba interna* para probar primero).
- [ ] Capturas, ícono y gráfico destacado cargados.
- [ ] Formulario de seguridad de datos y clasificación de contenido completados.

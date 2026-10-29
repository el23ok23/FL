# Publicar en Google Play

La app en Play Console es **Fletarg** (`com.fletarg.app`).

## 1. Clave de firma (una sola vez, guardala bien)

```bash
keytool -genkeypair -v -keystore fletarg.jks -alias fletarg \
  -keyalg RSA -keysize 2048 -validity 10000
```

Guardá el archivo `.jks` y las contraseñas en un lugar seguro: si se pierden no se pueden publicar actualizaciones.
Después copiá `keystore.properties.example` como `keystore.properties` (en la raíz del proyecto) y completalo. Ese archivo no se sube al repositorio.

Para que GitHub Actions firme el bundle por vos, cargá en *Settings → Secrets and variables → Actions* estos secrets:
`KEYSTORE_BASE64` (resultado de `base64 -w0 fletarg.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## 2. Generar el archivo para subir (.aab)

- Android Studio: *Build → Generate Signed App Bundle / APK → Android App Bundle*.
- Terminal: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
- GitHub Actions: *Actions → Android → la última ejecución → Artifacts → fletarg-release-aab*.

Antes de cada versión nueva subí `versionCode` (y `versionName`) en `app/build.gradle.kts`.

## 3. Política de privacidad

Play exige una URL pública. Publicá `docs/politica-de-privacidad.html` (completá los datos de contacto marcados) en cualquier hosting, por ejemplo GitHub Pages: *Settings → Pages → Deploy from a branch → carpeta /docs*. La URL queda como `https://TU_USUARIO.github.io/NOMBRE_DEL_REPO/politica-de-privacidad.html`. Pegala también en `config.json` (`politica_privacidad_url`).

## 4. Ficha de la tienda

- **Nombre**: Fletarg
- **Descripción breve** (80): Fletes desde Capital Federal a todo el país. Precio al toque.
- **Descripción completa**: Elegí origen, destino y hasta 4 paradas, el vehículo (de Kangoo a camión grande), ayudantes, pisos y cuándo lo necesitás, y mirá un precio aproximado al instante. Dejanos tu nombre y WhatsApp y te contactamos para confirmar. Sin pagos en la app. Salimos desde Capital Federal a cualquier punto de la Argentina.
- **Ícono**: `store/icono-512.png`.
- **Gráfico destacado**: `store/grafico-destacado-1024x500.png`.
- **Capturas**: al menos 2 del teléfono (sacalas desde el emulador o un celular: recorrido con paradas, carrusel de vehículos, precio, confirmación).
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

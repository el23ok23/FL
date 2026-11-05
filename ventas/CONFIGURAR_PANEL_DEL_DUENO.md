# Configurar el panel del dueño (una vez por empresa, lo hacemos nosotros)

El panel de la app guarda precios, nombre, colores y logo en una **planilla de Google del dueño**, a través de un **Apps Script** gratuito (sin servidor, sin abono). Lleva unos 10 minutos.

## 1. La planilla

1. Entrar a Google con la cuenta del dueño (o crearle una Gmail para el negocio).
2. Crear una planilla nueva en Google Sheets. Nombre sugerido: `Mi app · Fletes Don Pedro`.
3. Renombrar la primera hoja como `precios`.
4. Archivo → Importar → Subir → `docs/plantilla-precios-salto.csv` (o `plantilla-precios.csv`) → "Reemplazar la hoja actual". Queda con dos columnas: `clave` y `valor`.

## 2. El Apps Script

1. En la planilla: Extensiones → **Apps Script**.
2. Borrar lo que haya y pegar el contenido de `scripts/apps_script/Code.gs`.
3. Cambiar la línea `const PIN = 'salto2026fletes';` por el PIN inicial del dueño (4 a 32 caracteres). Después él lo cambia desde la app y el nuevo queda guardado en las propiedades del script. En `CORREO_RECUPERACION` se puede dejar su correo desde el inicio (también se carga desde la app).
4. Para que los pedidos le lleguen solos por WhatsApp: registrar cada número en CallMeBot (gratis: el dueño manda desde ese WhatsApp el mensaje "I allow callmebot to send me messages" al número que indica https://www.callmebot.com/blog/free-api-whatsapp-messages/ y recibe una clave) y cargar número y clave en `CALLMEBOT`. Los números sin clave igual reciben los pedidos que el cliente mande con el botón de la app.
5. Si el correo de aviso tiene que ir a otra casilla, ponerla en `CORREO`. Guardar (Ctrl+S).
6. **Implementar → Nueva implementación** → tipo **Aplicación web**:
   - Ejecutar como: **Yo** (la cuenta del dueño).
   - Quién tiene acceso: **Cualquier persona**.
   - Implementar. La primera vez pide autorizar permisos (Sheets, Drive, correo y conexiones externas): aceptar.
7. Copiar la **URL de la aplicación web** (termina en `/exec`).

Si más adelante hay que cambiar el PIN o actualizar el script: editar, guardar y en Implementar → Administrar implementaciones → lápiz → Versión: **Nueva versión** → Implementar. La URL no cambia.

## 3. Conectarlo a la app y la web

- Nueva empresa: `python3 scripts/nueva_empresa.py ... --precios-url "https://script.google.com/macros/s/.../exec"`.
- Empresa ya creada: pegar la URL en `precios_remotos.url` del `app/src/<slug>/assets/config.json` y en `precios_remotos_url` del bloque CONFIG de `docs/<slug>/index.html`. Dejar `pin_local` vacío. Commit y push: GitHub genera el APK y publica la web.

## 4. Probar

1. Abrir la app, mantener apretado el título de la primera pantalla, entrar con el PIN.
2. Cambiar un precio y Guardar → "Guardado. Ya rige en todos los teléfonos."
3. En la planilla aparece el valor nuevo. Abrir la web del cliente: lo toma al recargar.
4. Elegir un logo de la galería → se guarda en una carpeta "Logos …" del Drive del dueño.
5. Hacer un pedido de prueba desde la app: aparece en la hoja "pedidos", llega el correo y, si hay claves de CallMeBot, el WhatsApp.

## Qué pasa por detrás

- `GET /exec` devuelve la hoja como CSV `clave,valor`. La app lo baja al abrir y guarda una copia para usarla sin internet.
- `POST /exec` con `{pin, accion:"guardar", csv}` reemplaza la hoja. `accion:"verificar"` solo comprueba el PIN. `accion:"logo"` sube la imagen a Drive, la hace pública y devuelve su enlace.
- `POST /exec` con `{pin, accion:"cambiar_pin", nuevo_pin}` y `{pin, accion:"correo", correo}` guardan PIN y correo en las propiedades del script (nunca en la hoja, que es pública). `{accion:"recuperar"}` manda el PIN actual al correo de recuperación, máximo 3 veces por hora.
- `POST /exec` con `{accion:"pedido", ...}` (sin PIN, lo mandan los clientes desde la app y la web) agrega una fila en la hoja "pedidos", manda el correo y llama a CallMeBot por cada número de `CALLMEBOT`.
- Cualquiera con la URL puede *leer* los precios (los mismos que ve en la app). Escribir requiere el PIN, que vive solo en el script del dueño.

## Sin Apps Script (modo local)

Si en `config.json` queda `pin_local` con un PIN y `url` vacía, el panel funciona igual pero **los cambios valen solo en ese teléfono**. Sirve para probar y para mostrar la app en una reunión. Las apps del repositorio vienen así: Fletaba Salto con PIN `salto2026fletes` y Fletaba (Capital) con PIN `1234`. En modo local, "Olvidé mi PIN" abre el correo del teléfono con el PIN para mandárselo al correo de recuperación.

# Entrega al cliente · cómo cambiar los precios vos mismo

Tu app toma los precios de una **planilla de Google Sheets que es tuya**. Cambiás un número en la planilla y la app y la web lo usan en el próximo pedido. No hay que actualizar nada ni avisarle a nadie.

## Cómo está armada la planilla

Dos columnas: **clave** y **valor**. Las filas que importan:

| Clave | Qué es | Ejemplo |
|---|---|---|
| `kangoo.tarifa_base` | Tarifa base de la Kangoo (igual para cada vehículo: `furgon_mediano`, `furgon_grande`, `pickup`, `camion_3_4`, `camion_mediano_cerrado`, `camion_grande_cerrado`, `camion_playo`) | 25000 |
| `kangoo.por_km` | Valor por kilómetro de ese vehículo | 1800 |
| `por_ayudante` | Base por ayudante | 15000 |
| `por_ayudante_km` | Lo que suma cada ayudante por km | 500 |
| `por_piso_escalera` | Recargo por piso por escalera | 6000 |
| `por_parada_extra` | Recargo por parada intermedia | 8000 |
| `km_minimo` | Kilómetros mínimos que se cobran | 2 |
| `redondeo` | El total se redondea hacia arriba a este múltiplo | 500 |
| `espera_por_15_min` | Lo que se cobra por cada 15 minutos de espera de más | 6000 |

Los montos van sin signo pesos y sin puntos (`25000`), aunque la app entiende `25.000` y `$ 25.000`.
Si borrás una fila, para ese valor vuelve a usarse el que se cargó al entregar la app.
No cambies el texto de la columna clave.

## Cómo cambiar un precio (desde el celular o la compu)

1. Abrí la planilla "Precios · Tu empresa" (te la compartimos a tu Gmail; también está en tu Google Drive).
2. Tocá la celda del valor y escribí el número nuevo. Se guarda solo.
3. Listo. La app lo toma la próxima vez que un cliente la abre (y la web, al recargar). Si un cliente ya tiene la app abierta, lo ve al volver a entrar.

## Cómo la configuramos (lo hacemos nosotros en la entrega)

1. Google Sheets → Archivo → Importar → subir `plantilla-precios.csv` (o copiar y pegar).
2. Archivo → Compartir → **Publicar en la web** → elegir la hoja, formato **Valores separados por comas (.csv)** → Publicar → copiar el enlace.
3. Ese enlace va en `precios_remotos.url` del `config.json` de la app y en `precios_remotos_url` de la web.
4. Compartir la planilla con el Gmail del dueño como **Editor**.

"Publicar en la web" hace que cualquiera con el enlace pueda *leer* los precios (lo mismo que ya ve en la app). Nadie puede editarlos sin acceso a tu Google.

## Qué más recibís

- La app en Google Play, a tu nombre o transferida a tu cuenta de desarrollador (Play permite transferir apps entre cuentas).
- La versión web, con su enlace, para mandar por WhatsApp y redes.
- El material para redes (10 posts y 5 videos) con tu marca.
- Este instructivo y la planilla de precios.

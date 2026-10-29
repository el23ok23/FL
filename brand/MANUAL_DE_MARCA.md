# Manual de marca · FLETABA

**Fletaba** es la app para pedir fletes desde Capital Federal a todo el país. La marca es simple, confiable y argentina:
un camión blanco en movimiento bajo el sol, sobre celeste.

## 1. Logo

Archivos en `brand/logo/` (SVG vectorial, con la tipografía embebida) y `brand/png/` (PNG 2048 px y PDF).

| Versión | Archivo | Cuándo usarla |
|---|---|---|
| Horizontal color | `fletaba-logo-horizontal-color.svg` | Versión principal: web, documentos, cartelería, fondos claros. |
| Vertical color | `fletaba-logo-vertical-color.svg` | Espacios cuadrados: remeras (pecho), stickers, perfiles. |
| Isotipo | `fletaba-isotipo-color.svg` / `-circular-` | Ícono de la app, redes (foto de perfil), favicon, bordados chicos. |
| Wordmark | `fletaba-wordmark-color.svg` | Cuando el isotipo ya está presente en otro lado. |
| Blanco | `*-blanco.svg` | Sobre Celeste Fletaba, Azul noche, fotos oscuras, textil de color. |
| Negro | `*-negro.svg` | Una sola tinta: grabado, sello, serigrafía a un color, fax, documentos B/N. |

**Área de resguardo**: alrededor del logo dejá libre, como mínimo, la altura de la letra "F" del wordmark.
**Tamaño mínimo**: horizontal 28 mm de ancho en impresión (120 px en pantalla); isotipo 10 mm (32 px).

**No hacer**: no estirar ni deformar, no rotar, no cambiar los colores, no agregar sombras ni contornos, no poner el logo color sobre fondos de color fuerte (usar la versión blanca), no separar el sol del camión.

## 2. Colores

| Nombre | HEX | RGB | CMYK aprox. | Pantone aprox. | Uso |
|---|---|---|---|---|---|
| Celeste Fletaba | `#1B7BE0` | (27, 123, 224) | (88, 45, 0, 12) | 2727 C | Color principal: botones, origen, barra de precio, fondo del isotipo. |
| Sol | `#F6B40E` | (246, 180, 14) | (0, 27, 94, 4) | 7408 C | Acento: destino, detalles, llamados a la acción secundarios. |
| Azul noche | `#0B1F3A` | (11, 31, 58) | (81, 47, 0, 77) | 2767 C | Textos y wordmark sobre fondos claros; fondos oscuros. |
| Celeste claro | `#D6E9FF` | (214, 233, 255) | (16, 9, 0, 0) | 2707 C | Fondos suaves, chips y tarjetas seleccionadas. |
| Gris pizarra | `#64748B` | (100, 116, 139) | (28, 17, 0, 45) | 7544 C | Textos secundarios. |
| Blanco | `#FFFFFF` | (255, 255, 255) | (0, 0, 0, 0) | — | Fondos y camión del isotipo. |

Proporción recomendada: 60 % blanco/fondos claros, 25 % Celeste Fletaba, 10 % Azul noche, 5 % Sol.

## 3. Tipografía

- **Inter** (gratuita, Google Fonts): ExtraBold para títulos y wordmark, SemiBold para botones, Regular/Medium para textos.
- Alternativa cuando no se pueda instalar Inter: **Roboto** (Android) o **Arial**.
- Títulos con tracking ligeramente negativo (−2 % a −3 %); textos normales sin tracking.

## 4. Tono de voz

Cercano y rioplatense ("pedí", "elegí", "te contactamos"), frases cortas, sin tecnicismos, sin promesas de precio exacto: siempre "precio aproximado" o "estimado". Palabras clave: rápido, claro, de confianza, todo el país.

## 5. Textil e impresión

- **Remeras y buzos**: isotipo blanco en pecho izquierdo (8 a 10 cm de ancho) y logo horizontal blanco en espalda (28 a 30 cm). Prenda en Azul noche `#0B1F3A` o Celeste `#1B7BE0`. Usar `fletaba-logo-horizontal-blanco.pdf` (vectorial).
- **Serigrafía / vinilo a un color**: versión negra o blanca según la prenda. El sol se imprime del mismo color que el camión en una tinta; en dos tintas, el sol va en Sol `#F6B40E`.
- **Bordado**: isotipo o wordmark; mínimo 60 mm de ancho para el horizontal, 25 mm para el isotipo. Hilos: celeste (Madeira 1029 aprox.), amarillo (Madeira 1024 aprox.), blanco, azul marino.
- **Imanes y ploteo de vehículos**: logo horizontal blanco sobre Celeste, o color sobre blanco. Tamaño en puerta: 40 a 50 cm de ancho.
- **Gorras**: isotipo bordado al frente (55 a 60 mm).
- Los PDF de `brand/png/` son vectoriales: se pueden escalar a cualquier tamaño sin perder calidad. Entregar a la imprenta el PDF o el SVG, nunca el PNG salvo que lo pidan.

## 6. Regenerar los archivos

```
python3 brand/make_logos.py brand      # vuelve a generar los SVG
node brand/render_png.js               # exporta PNG 2048 px y PDF
```

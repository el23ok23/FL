# Marketing Fletaba

## Videos (`videos/mp4/`)
Cinco videos verticales 1080×1920 (reels, historias, TikTok, YouTube Shorts) con música original sintetizada, sin material de terceros.

| Archivo | Duración | Mensaje |
|---|---|---|
| `fletaba-01-lanzamiento.mp4` | 12 s | Presentación: logo, "Pedí tu flete al toque", beneficios, descarga. |
| `fletaba-02-como-funciona.mp4` | 16 s | Los 4 pasos de la app: recorrido, tamaño, detalles, precio. |
| `fletaba-03-todo-el-pais.mp4` | 12 s | Cobertura: desde Capital Federal a todo el país. |
| `fletaba-04-precio-claro.mp4` | 12 s | Precio desglosado, sin pagos en la app, confirmación por WhatsApp. |
| `fletaba-05-fleteros.mp4` | 12 s | Para conductores: pedidos claros, sumate. |

Para volver a generarlos (cambiar textos, colores o música): editá `videos/make_videos.js` (escenas) o `videos/make_audio.py` (música) y corré `node videos/make_videos.js`. Hace falta Node con Playwright, Python con numpy y ffmpeg.

## Posts (`posts/`)
Diez posts en dos formatos: `feed/` (1080×1080) e `historias/` (1080×1920). Los textos para publicar están en `posts/CAPTIONS.md`. Se regeneran con `node posts/make_posts.js`.

## Identidad
Logos, colores, tipografía y reglas para textil en `../brand/` (ver `MANUAL_DE_MARCA.md` o `manual.html`).

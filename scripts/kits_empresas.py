#!/usr/bin/env python3
"""Kit completo por empresa (después de demos_empresas.py y make_empresas.js):
   docs/empresas/<slug>/contrato.html  contrato a nombre de la empresa, imprimible (los $ y la fecha quedan para completar)
   docs/empresas/<slug>/mensaje.txt    mensaje de WhatsApp con sus enlaces
   docs/empresas/<slug>/kit.html       una página con todo: demo, panel, video, propuesta, contrato, mensaje y datos
   y, si se pasa --zip <carpeta>, un ZIP por empresa con propuesta, contrato, video, mensaje y LEEME.
   Uso: python3 scripts/kits_empresas.py [--zip /ruta/salida]"""
import os, re, json, html, sys, zipfile, datetime
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "https://el23ok23.github.io/FL/empresas"
emp = json.load(open(os.path.join(ROOT, "ventas", "empresas_salto.json"), encoding="utf-8"))
contrato_md = open(os.path.join(ROOT, "ventas", "CONTRATO_MODELO.md"), encoding="utf-8").read()
zip_dir = sys.argv[sys.argv.index("--zip") + 1] if "--zip" in sys.argv else None
hoy = datetime.date.today().strftime("%d/%m/%Y")

CSS = "body{font-family:Inter,system-ui,sans-serif;color:#0B1F3A;background:#fff;max-width:860px;margin:0 auto;padding:32px 24px;line-height:1.55}h1{font-size:26px;letter-spacing:-.02em}h2{font-size:18px;margin-top:28px}p{margin:10px 0}blockquote{color:#64748B;border-left:3px solid #D9E2F0;margin:0;padding:4px 14px;font-size:14px}.campo{background:#FEF3C7;border-radius:4px;padding:0 4px}.firmas{margin-top:48px;display:flex;gap:40px;justify-content:space-between;font-size:14px}.firmas div{flex:1;border-top:1px solid #0B1F3A;padding-top:8px}a{color:#1B7BE0}.card{background:#F4F8FE;border:1px solid #D9E2F0;border-radius:14px;padding:16px 18px;margin:12px 0}code{background:#E9F0FA;padding:2px 6px;border-radius:6px}@media print{a{color:inherit;text-decoration:none}.noprint{display:none}}"

def md_to_html(md):
    out = []
    for line in md.split("\n"):
        t = line.strip()
        if not t: continue
        if t.startswith("# "): out.append(f"<h1>{html.escape(t[2:])}</h1>"); continue
        if t.startswith("> "): out.append(f"<blockquote>{html.escape(t[2:])}</blockquote>"); continue
        esc = html.escape(t)
        esc = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", esc)
        esc = esc.replace("COMPLETAR", '<span class="campo">COMPLETAR</span>')
        if t.startswith("Firmas:"):
            out.append('<div class="firmas"><div>Firma y aclaración · el Proveedor</div><div>Firma y aclaración · la Empresa</div><div>Fecha</div></div>')
        else:
            out.append(f"<p>{esc}</p>")
    return "\n".join(out)

rows = []
for e in emp:
    slug, nombre = e["slug"], e["nombre"]
    d = os.path.join(ROOT, "docs", "empresas", slug)
    pin = slug.replace("-", "") + "2026"
    demo, prop, video, contrato, kit = (f"{BASE}/{slug}/", f"{BASE}/{slug}/propuesta.html", f"{BASE}/{slug}/video.mp4", f"{BASE}/{slug}/contrato.html", f"{BASE}/{slug}/kit.html")
    tel = ", ".join(e["telefonos"]) + (f" · WhatsApp {e['whatsapp']}" if e.get("whatsapp") else "")

    # Contrato a su nombre
    md = contrato_md.replace("# Contrato de servicio · modelo", f"# Contrato de servicio · {nombre}")
    md = md.replace("**y** COMPLETAR (razón social o nombre, CUIT COMPLETAR; en adelante, \"la Empresa\")",
                    f"**y** {nombre} (razón social COMPLETAR, CUIT COMPLETAR, con domicilio en {e.get('direccion', 'Salto')}; en adelante, \"la Empresa\")")
    md = md.replace("en el WhatsApp indicado por la Empresa (COMPLETAR número)", f"en el WhatsApp indicado por la Empresa ({e['whatsapp'] if e.get('whatsapp') else 'COMPLETAR número'})")
    md = md.replace("los tribunales ordinarios de COMPLETAR, Provincia de Buenos Aires", "los tribunales ordinarios de Salto, Provincia de Buenos Aires")
    body = md_to_html(md)
    open(os.path.join(d, "contrato.html"), "w", encoding="utf-8").write(f'<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>Contrato · {html.escape(nombre)}</title><style>{CSS}</style></head><body><p class="noprint"><a href="kit.html">← Volver al kit</a> · <a href="javascript:print()">Imprimir / guardar PDF</a></p>{body}<p style="color:#64748B;font-size:12px;margin-top:30px">Borrador preparado el {hoy} para {html.escape(nombre)}. Revisar con contador o abogado antes de firmar. Los campos en amarillo se completan a mano.</p></body></html>')
    open(os.path.join(d, "contrato.md"), "w", encoding="utf-8").write(md)

    # Mensaje de WhatsApp
    # Secuencia completa de WhatsApp: qué mandar y qué escribir antes de cada cosa
    m1 = (f"Hola, ¿qué tal? Soy [tu nombre], de Salto. Les armé una web app de prueba a {nombre}: sus clientes piden el flete desde el celular y a ustedes les llega el pedido al WhatsApp con el precio ya calculado con sus tarifas. Es una web app: se abre desde un enlace y se instala en el celular sin pasar por ninguna tienda. Un solo pago, sin comisiones ni abonos.\n\n"
          f"Son dos enlaces:\n\n"
          f"1) La página que ven sus clientes, ya con el nombre {nombre}. Ábranla desde el celular y pidan un flete de prueba:\n{demo}\n\n"
          f"2) El panel del dueño, donde ustedes mismos cambian los precios, el nombre, los colores y el logo:\n{demo}?panel\nCódigo para entrar: {pin}\n\n"
          f"¿Con quién puedo hablar del tema?")
    m2 = "Les dejo un video de 30 segundos de cómo funciona 👇"
    m3 = (f"Buenísimo. Les paso la propuesta completa en PDF: qué incluye, el precio único y los plazos. Cualquier duda me escriben, y si quieren en 20 minutos configuramos la web app con sus tarifas y su WhatsApp para que la prueben con un pedido real. ¿Qué día les queda cómodo?")
    m4 = (f"Hola, ¿pudieron ver la web app de {nombre}? Les recuerdo los enlaces: la página de sus clientes {demo} y el panel del dueño {demo}?panel (código {pin}). Si les sirve, coordinamos 20 minutos y la dejamos andando con sus precios. ¿Qué día les queda bien?")
    m5 = (f"Les paso el contrato modelo en PDF para que lo revisen con tranquilidad. Es un solo pago, 50 % al empezar y 50 % contra entrega, sin comisiones ni abonos; en 15 días hábiles tienen la web app definitiva con su nombre, su WhatsApp y su panel. Si están de acuerdo, lo firmamos y arrancamos.")
    secuencia = (f"SECUENCIA DE ENVÍO · {nombre}\nTeléfonos: {tel}\n{'=' * 60}\n\n"
                 f"PASO 1 · Primer contacto (solo texto). Copiá y pegá:\n{'-' * 60}\n{m1}\n{'-' * 60}\n\n"
                 f"PASO 2 · Enseguida, en otro mensaje: escribí esto y ADJUNTÁ EL VIDEO ({slug}-video.mp4) desde la galería:\n{'-' * 60}\n{m2}\n{'-' * 60}\n\n"
                 f"PASO 3 · Cuando respondan con interés: escribí esto y ADJUNTÁ LA PROPUESTA ({slug}-propuesta.pdf):\n{'-' * 60}\n{m3}\n{'-' * 60}\n\n"
                 f"PASO 4 · Si en 2 o 3 días no respondieron (una sola vez):\n{'-' * 60}\n{m4}\n{'-' * 60}\n\n"
                 f"PASO 5 · Cuando digan que sí: escribí esto y ADJUNTÁ EL CONTRATO ({slug}-contrato.pdf). Antes completá a mano el precio y la fecha:\n{'-' * 60}\n{m5}\n{'-' * 60}\n\n"
                 f"Enlaces de {nombre}:\n· Página de sus clientes: {demo}\n· Panel del dueño (cambian precios, nombre, colores y logo): {demo}?panel · código {pin}\n· Video: {video}\n· Propuesta: {prop}\n· Contrato: {contrato}\n")
    msg, msg2, msg3 = m1, m2, m4
    open(os.path.join(d, "mensaje.txt"), "w", encoding="utf-8").write(secuencia)
    open(os.path.join(d, "secuencia.txt"), "w", encoding="utf-8").write(secuencia)

    # Página del kit
    kit_html = f'''<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>Kit · {html.escape(nombre)}</title><style>{CSS}</style></head><body>
<p class="noprint"><a href="../">← Todas las empresas</a></p>
<h1>Kit de venta · {html.escape(nombre)}</h1>
<div class="card"><b>Datos</b><br>Teléfonos: {html.escape(tel)}<br>Dirección: {html.escape(e.get("direccion", ""))}<br>Rubro: {html.escape(e.get("rubro", ""))}<br><small>Fuente: {html.escape(e.get("fuente", ""))}. Confirmar el número antes de llamar.</small></div>
<div class="card"><b>1. Demo con su nombre</b><br><a href="{demo}">{demo}</a><br>Panel del dueño: <a href="{demo}?panel">{demo}?panel</a> · PIN <code>{pin}</code><br><small>Desde el celular, el botón "Instalar la app" deja el ícono con su nombre en la pantalla de inicio.</small></div>
<div class="card"><b>2. Video (30 s)</b><br><a href="{video}">{video}</a><br><video src="video.mp4" controls playsinline style="width:220px;border-radius:12px;margin-top:8px"></video></div>
<div class="card"><b>3. Propuesta a su nombre</b><br><a href="{prop}">{prop}</a></div>
<div class="card"><b>4. Contrato a su nombre</b><br><a href="{contrato}">{contrato}</a> (imprimible; los montos y la fecha se completan a mano)</div>
<div class="card"><b>5. Secuencia de WhatsApp (qué escribir antes de cada cosa)</b><pre style="white-space:pre-wrap;font:inherit;margin:8px 0 0">{html.escape(secuencia)}</pre></div>
<div class="card"><b>Orden sugerido</b><ol style="margin:6px 0 0;padding-left:20px"><li>Llamar al fijo y pedir el WhatsApp del dueño.</li><li>Mandar el mensaje con el video.</li><li>Si responde: reunión de 20 minutos con la demo en su teléfono.</li><li>Propuesta y contrato. Seña del 50 %.</li></ol></div>
</body></html>'''
    open(os.path.join(d, "kit.html"), "w", encoding="utf-8").write(kit_html)
    rows.append((e, pin))

    if zip_dir:
        os.makedirs(zip_dir, exist_ok=True)
        zp = os.path.join(zip_dir, f"{slug}.zip")
        with zipfile.ZipFile(zp, "w", zipfile.ZIP_DEFLATED) as z:
            for fn in ["propuesta.html", "propuesta.pdf", "contrato.html", "contrato.pdf", "contrato.md", "secuencia.txt", "video.mp4", "kit.html"]:
                fp = os.path.join(d, fn)
                if os.path.exists(fp): z.write(fp, f"{slug}/{fn}")
            z.writestr(f"{slug}/LEEME.txt", f"{nombre}\nTeléfonos: {tel}\nDirección: {e.get('direccion','')}\n\nDemo con su nombre: {demo}\nPanel del dueño: {demo}?panel (PIN {pin})\nVideo: {video}\nPropuesta: {prop}\nContrato: {contrato}\nKit completo: {kit}\n\nEl mensaje de WhatsApp está en mensaje.txt. La propuesta y el contrato se abren con cualquier navegador y se imprimen o guardan en PDF desde ahí.\n")

# Índice general
idx_rows = "".join(f'<tr><td><b style="color:{e.get("color","#1B7BE0")}">{html.escape(e["nombre"])}</b><br><small>{html.escape(e.get("rubro",""))}</small></td><td>{html.escape(", ".join(e["telefonos"]))}{(" · WhatsApp " + e["whatsapp"]) if e.get("whatsapp") else ""}<br><small>{html.escape(e.get("direccion",""))}</small></td><td><a href="{e["slug"]}/kit.html"><b>kit completo</b></a><br><a href="{e["slug"]}/">demo</a> · <a href="{e["slug"]}/?panel">panel</a> (PIN <code>{pin}</code>)<br><a href="{e["slug"]}/propuesta.html">propuesta</a> · <a href="{e["slug"]}/contrato.html">contrato</a> · <a href="{e["slug"]}/video.mp4">video</a></td></tr>' for e, pin in rows)
open(os.path.join(ROOT, "docs", "empresas", "index.html"), "w", encoding="utf-8").write(f'''<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>Kits por empresa · Salto</title>
<style>body{{font-family:Inter,system-ui,sans-serif;margin:0;padding:24px;background:#F4F8FE;color:#0B1F3A}}h1{{font-size:26px}}table{{width:100%;border-collapse:collapse;background:#fff;border-radius:14px;overflow:hidden}}td{{padding:12px;border-bottom:1px solid #E2E8F0;vertical-align:top;font-size:14px}}a{{color:#1B7BE0}}code{{background:#E9F0FA;padding:2px 6px;border-radius:6px}}</style></head>
<body><h1>Kits por empresa · fletes de Salto</h1><p>Uso interno para la venta. Cada empresa tiene su app web con su nombre, su panel, su video, su propuesta y su contrato.</p><table>{idx_rows}</table></body></html>''')

# ventas/EMPRESAS_SALTO.md: sección de kits
md_path = os.path.join(ROOT, "ventas", "EMPRESAS_SALTO.md"); md = open(md_path, encoding="utf-8").read()
links = ["## Kit por empresa (demo web, panel, video, propuesta, contrato y mensaje)", ""]
for e, pin in rows:
    links.append(f'- **{e["nombre"]}**: kit completo {BASE}/{e["slug"]}/kit.html · demo {BASE}/{e["slug"]}/ · panel {BASE}/{e["slug"]}/?panel (PIN `{pin}`) · video {BASE}/{e["slug"]}/video.mp4 · propuesta {BASE}/{e["slug"]}/propuesta.html · contrato {BASE}/{e["slug"]}/contrato.html')
links += ["", "Índice de todo: " + BASE + "/", ""]
md = re.sub(r"## Kit por empresa.*?(?=## Mensaje de primer contacto)", "\n".join(links) + "\n", md, flags=re.S)
open(md_path, "w", encoding="utf-8").write(md)
print("kits ok", len(rows), "| zips:", zip_dir or "no")

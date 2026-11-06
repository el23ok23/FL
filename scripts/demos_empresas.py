#!/usr/bin/env python3
"""Arma, para cada empresa de ventas/empresas_salto.json, su carpeta en docs/empresas/<slug>/:
   - index.html: la app web de prueba con su nombre, su color y su teléfono (PIN del panel: <slug sin guiones>2026)
   - propuesta.html: la propuesta comercial en una página, a su nombre, con los enlaces a la demo y al video
   - video.mp4: su video personalizado (si ya se generó con marketing/videos/make_empresas.js)
   y docs/empresas/index.html con el listado. Uso: python3 scripts/demos_empresas.py"""
import json, os, re, shutil, html
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "https://el23ok23.github.io/FL/empresas"
emp = json.load(open(os.path.join(ROOT, "ventas", "empresas_salto.json"), encoding="utf-8"))
tpl = open(os.path.join(ROOT, "docs", "donpedro", "index.html"), encoding="utf-8").read()
fol = open(os.path.join(ROOT, "ventas", "folleto.html"), encoding="utf-8").read()

def dark(hexc):
    r, g, b = (int(hexc[i:i+2], 16) for i in (1, 3, 5))
    return "#%02X%02X%02X" % (int(r*0.8), int(g*0.8), int(b*0.8))

def wa_digits(e):
    if e.get("whatsapp"): return e["whatsapp"]
    return ""

rows = []
for e in emp:
    slug, nombre, color = e["slug"], e["nombre"], e.get("color", "#1B7BE0")
    pin = slug.replace("-", "") + "2026"
    d = os.path.join(ROOT, "docs", "empresas", slug); os.makedirs(d, exist_ok=True)
    w = tpl
    w = w.replace("<title>Fletes Don Pedro</title>", f"<title>{html.escape(nombre)}</title>")
    w = w.replace('id="brandname">FLETES DON PEDRO<', f'id="brandname">{html.escape(nombre.upper())}<')
    w = re.sub(r'negocio: \{ nombre: "[^"]*", whatsapp_dueno: "[^"]*"', f'negocio: {{ nombre: {json.dumps(nombre, ensure_ascii=False)}, whatsapp_dueno: "{wa_digits(e)}"', w, count=1)
    w = w.replace("--celeste:#0E7C4A; --celeste-oscuro:#0E7C4A;", f"--celeste:{color}; --celeste-oscuro:{dark(color)};")
    w = w.replace('<rect width="108" height="108" fill="#1B7BE0"/>', f'<rect width="108" height="108" fill="{color}"/>')
    w = re.sub(r'pin_local: "[^"]*"', f'pin_local: "{pin}"', w, count=1)
    assert nombre in w and color in w and pin in w
    open(os.path.join(d, "index.html"), "w", encoding="utf-8").write(w)

    # Propuesta a su nombre
    p = fol
    p = p.replace("<title>Fletaba para empresas de fletes", f"<title>Propuesta para {html.escape(nombre)}")
    p = p.replace("FLETABA · PARA EMPRESAS DE FLETES DE SALTO", f"FLETABA · PROPUESTA PARA {html.escape(nombre.upper())}")
    p = re.sub(r"<h1>.*?</h1>", f"<h1>La app de fletes de {html.escape(nombre)}, lista para sus clientes.</h1>", p, count=1, flags=re.S)
    video = f"{BASE}/{slug}/video.mp4"
    demo = f"{BASE}/{slug}/"
    extra = (f'<h2>Pruébenla ahora, con su nombre</h2><div class="grid">'
             f'<div class="card"><b>Demo web de {html.escape(nombre)}</b>La app completa, con su nombre y su color: <a href="{demo}">{demo}</a><br><small>Panel del dueño: mantener apretado el título o abrir <a href="{demo}?panel">{demo}?panel</a> · PIN <code>{pin}</code> (en la demo los cambios quedan solo en su navegador).</small></div>'
             f'<div class="card"><b>Video de 30 segundos</b><a href="{video}">{video}</a></div></div>')
    p = p.replace('<h2>Qué recibís</h2>', extra + '<h2>Qué recibís</h2>', 1)
    p = p.replace("Tu nombre, tus colores, tu ícono.", f"Con el nombre {html.escape(nombre)}, sus colores y su ícono.")
    p = p.replace("<footer class=\"in\">COMPLETAR nombre · WhatsApp COMPLETAR · Salto, Provincia de Buenos Aires</footer>", f"<footer class=\"in\">Propuesta preparada para {html.escape(nombre)} · {html.escape(e.get('direccion',''))} · Fletaba, Salto, Provincia de Buenos Aires</footer>")
    open(os.path.join(d, "propuesta.html"), "w", encoding="utf-8").write(p)

    # Video
    src = os.path.join(ROOT, "marketing", "videos", "mp4", f"empresa-{slug}.mp4")
    if os.path.exists(src): shutil.copyfile(src, os.path.join(d, "video.mp4"))
    tel = html.escape(", ".join(e["telefonos"])) + (f' · WhatsApp {e["whatsapp"]}' if e.get("whatsapp") else "")
    rows.append(f'<tr><td><b style="color:{color}">{html.escape(nombre)}</b><br><small>{html.escape(e.get("rubro",""))}</small></td><td>{tel}<br><small>{html.escape(e.get("direccion",""))}</small></td><td><a href="{slug}/">demo</a> · <a href="{slug}/?panel">panel</a> (PIN <code>{pin}</code>)<br><a href="{slug}/propuesta.html">propuesta</a> · <a href="{slug}/video.mp4">video</a></td></tr>')
    e["_pin"] = pin

idx = f'''<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>Demos por empresa · Salto</title>
<style>body{{font-family:Inter,system-ui,sans-serif;margin:0;padding:24px;background:#F4F8FE;color:#0B1F3A}}h1{{font-size:26px}}table{{width:100%;border-collapse:collapse;background:#fff;border-radius:14px;overflow:hidden}}td{{padding:12px;border-bottom:1px solid #E2E8F0;vertical-align:top;font-size:14px}}a{{color:#1B7BE0}}code{{background:#E9F0FA;padding:2px 6px;border-radius:6px}}</style></head>
<body><h1>Demos por empresa · fletes de Salto</h1><p>Uso interno para la venta. Cada empresa tiene su app web con su nombre, su panel, su propuesta y su video.</p><table>{"".join(rows)}</table></body></html>'''
open(os.path.join(ROOT, "docs", "empresas", "index.html"), "w", encoding="utf-8").write(idx)

# Lista en ventas/EMPRESAS_SALTO.md: enlaces por empresa
md_path = os.path.join(ROOT, "ventas", "EMPRESAS_SALTO.md"); md = open(md_path, encoding="utf-8").read()
start = md.find("## Mensaje de primer contacto (personalizado)")
links = ["## Kit por empresa (demo web, panel, propuesta y video)", ""]
for e in emp:
    links.append(f'- **{e["nombre"]}**: demo {BASE}/{e["slug"]}/ · panel {BASE}/{e["slug"]}/?panel (PIN `{e["_pin"]}`) · propuesta {BASE}/{e["slug"]}/propuesta.html · video {BASE}/{e["slug"]}/video.mp4')
links += ["", "Índice de todo: " + BASE + "/", ""]
if "## Kit por empresa" in md:
    md = re.sub(r"## Kit por empresa.*?(?=## Mensaje de primer contacto)", "\n".join(links) + "\n", md, flags=re.S)
else:
    md = md[:start] + "\n".join(links) + "\n" + md[start:]
md = md.replace("Les hice un video de 30 segundos a", "Les armé una app de prueba y un video de 30 segundos a")
for e in emp:
    md = md.replace(f"se las muestro funcionando con su nombre y sus precios.\n\n", f"se las muestro funcionando con su nombre y sus precios.\n>\n> Pruébenla acá, ya con su nombre: {BASE}/{e['slug']}/ · Video: {BASE}/{e['slug']}/video.mp4\n\n", 1) if f"**{e['nombre']}**" in md else md
open(md_path, "w", encoding="utf-8").write(md)
print("ok", len(emp), "empresas")

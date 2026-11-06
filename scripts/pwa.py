#!/usr/bin/env python3
"""Convierte cada web (docs/, docs/salto, docs/donpedro, docs/empresas/*) en una web app instalable (PWA):
   manifest con nombre y color, íconos PNG, service worker para abrir sin conexión y un botón "Instalar".
   Es idempotente: se puede correr después de cada cambio. Uso: python3 scripts/pwa.py"""
import os, re, json, html, subprocess, glob
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")
ISO = open(os.path.join(ROOT, "brand", "logo", "fletaba-isotipo-color.svg"), encoding="utf-8").read()
FOLDERS = [DOCS, os.path.join(DOCS, "salto"), os.path.join(DOCS, "donpedro")] + sorted(glob.glob(os.path.join(DOCS, "empresas", "*", "")))

SW = """// Service worker: deja la app abierta sin conexión (la página y lo que ya se vio). Las búsquedas de
// direcciones, el mapa y el envío del pedido siguen necesitando internet.
const CACHE = 'fletaba-__VERSION__';
const CORE = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png'];
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(CORE)).then(() => self.skipWaiting())); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET') return;
  // Datos vivos (precios, pedidos, direcciones, mapas): siempre red; si falla, lo que haya en caché.
  const live = /script\\.google|googleusercontent|nominatim|openstreetmap|googleapis\\.com\\/maps|tile\\./.test(u.host + u.pathname);
  if (live) { e.respondWith(fetch(e.request).catch(() => caches.match(e.request))); return; }
  // Página y recursos: caché primero, y se actualiza en segundo plano.
  e.respondWith(caches.match(e.request).then(hit => {
    const net = fetch(e.request).then(r => { if (r && r.ok && (u.origin === location.origin || /cdnjs|fonts\\.g/.test(u.host))) caches.open(CACHE).then(c => c.put(e.request, r.clone())); return r; }).catch(() => hit);
    return hit || net;
  }));
});
"""

PWA_JS = """
/* ---------- WebApp instalable ---------- */
(function(){
  if('serviceWorker' in navigator && location.protocol.startsWith('http')){navigator.serviceWorker.register('./sw.js').catch(()=>{});}
  let deferred=null;const ios=/iphone|ipad|ipod/i.test(navigator.userAgent)&&!window.MSStream;const standalone=window.matchMedia('(display-mode: standalone)').matches||navigator.standalone;
  function button(){if(standalone||document.getElementById('installbtn'))return;const b=document.createElement('button');b.id='installbtn';b.type='button';b.textContent='📲 Instalar la WebApp';b.style.cssText='position:fixed;left:50%;transform:translateX(-50%);bottom:calc(96px + env(safe-area-inset-bottom,0px));z-index:60;border:0;border-radius:999px;padding:10px 16px;font:600 14px/1 Inter,system-ui,sans-serif;background:var(--noche,#0B1F3A);color:#fff;box-shadow:0 10px 30px rgba(11,31,58,.25);cursor:pointer';
    b.addEventListener('click',async()=>{if(deferred){deferred.prompt();const r=await deferred.userChoice;deferred=null;if(r&&r.outcome==='accepted')b.remove();return;}
      alert(ios?'Para instalarla en tu iPhone: tocá el botón Compartir (el cuadrado con la flecha) y después "Agregar a inicio".':'Para instalarla: abrí el menú del navegador (⋮) y tocá "Instalar aplicación" o "Agregar a pantalla de inicio".');});
    document.body.appendChild(b);setTimeout(()=>{if(b.parentNode&&!deferred)b.style.opacity='.9';},0);}
  window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();deferred=e;button();});
  window.addEventListener('appinstalled',()=>{const b=document.getElementById('installbtn');if(b)b.remove();});
  // Sin el evento (iPhone, o ya visitada): igual ofrecemos el botón con las instrucciones, pero discreto y solo tras 20 s de uso.
  setTimeout(()=>{if(!deferred)button();},20000);
})();
"""

def color_of(page):
    m = re.search(r"--celeste:(#[0-9A-Fa-f]{6})", page)
    return m.group(1).upper() if m else "#1B7BE0"

def name_of(page):
    m = re.search(r'negocio: \{ nombre: "([^"]*)"', page)
    return m.group(1) if m else "Fletaba"

def render_icons(folder, color):
    svg = ISO.replace("#1B7BE0", color).replace("#1b7be0", color)
    svg_path = os.path.join(folder, "icon.svg")
    open(svg_path, "w", encoding="utf-8").write(svg)
    big = os.path.join(folder, "icon-512.png"); small = os.path.join(folder, "icon-192.png")
    js = f"""const {{ chromium }} = require('playwright');(async()=>{{const b=await chromium.launch();const p=await b.newPage({{viewport:{{width:512,height:512}},deviceScaleFactor:1}});
await p.goto('file://{svg_path}');await p.waitForTimeout(300);await p.screenshot({{path:'{big}',omitBackground:true,clip:{{x:0,y:0,width:512,height:512}}}});await b.close();}})();"""
    subprocess.run(["node", "-e", js], check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", big, "-vf", "scale=192:192:flags=lanczos", small], check=True)
    os.remove(svg_path)

version = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=ROOT, capture_output=True, text=True).stdout.strip() or "1"
for folder in FOLDERS:
    folder = folder.rstrip("/")
    idx = os.path.join(folder, "index.html")
    if not os.path.exists(idx): continue
    page = open(idx, encoding="utf-8").read()
    name, color = name_of(page), color_of(page)
    # Nombre corto para debajo del ícono: la palabra más distintiva (no "Transportes", "Fletes", "Mudanzas"…).
    generic = {"transporte", "transportes", "fletes", "flete", "mudanzas", "mudanza", "logística", "logistica", "de", "y", "cargas", "carga", "srl", "sa", "el", "la", "los", "las"}
    words = [w for w in name.split() if w.lower().strip(".,") not in generic]
    short = name if len(name) <= 12 else (" ".join(words)[:12].strip() if words else name[:12])
    manifest = {
        "name": name, "short_name": short, "description": f"{name}: pedí tu flete desde el celular y conocé el precio al toque.",
        "start_url": "./", "scope": "./", "display": "standalone", "orientation": "portrait",
        "background_color": "#F4F8FE", "theme_color": color, "lang": "es-AR",
        "icons": [
            {"src": "icon-192.png", "sizes": "192x192", "type": "image/png", "purpose": "any"},
            {"src": "icon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "any"},
            {"src": "icon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable"},
        ],
    }
    json.dump(manifest, open(os.path.join(folder, "manifest.webmanifest"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    open(os.path.join(folder, "sw.js"), "w", encoding="utf-8").write(SW.replace("__VERSION__", version))
    if not (os.path.exists(os.path.join(folder, "icon-512.png")) and os.path.exists(os.path.join(folder, "icon-192.png"))) or os.environ.get("PWA_ICONS") == "1":
        render_icons(folder, color)
    # Etiquetas en el <head> (se reemplazan si ya estaban)
    page = re.sub(r'\n<!-- pwa -->.*?<!-- /pwa -->', "", page, flags=re.S)
    head = (f'\n<!-- pwa --><link rel="manifest" href="manifest.webmanifest"><meta name="theme-color" content="{color}">'
            f'<meta name="apple-mobile-web-app-capable" content="yes"><meta name="mobile-web-app-capable" content="yes">'
            f'<meta name="apple-mobile-web-app-status-bar-style" content="default"><meta name="apple-mobile-web-app-title" content="{html.escape(short)}">'
            f'<link rel="apple-touch-icon" href="icon-192.png"><link rel="icon" type="image/png" sizes="192x192" href="icon-192.png"><!-- /pwa -->')
    page = page.replace("<title>", head + "\n<title>", 1) if "<!-- pwa -->" not in page else page
    # Script (al final del último <script>)
    page = re.sub(r"\n/\* ---------- (Web app|WebApp) instalable ---------- \*/.*?\n\}\)\(\);\n", "\n", page, flags=re.S)
    i = page.rfind("</script>")
    page = page[:i] + PWA_JS + page[i:]
    open(idx, "w", encoding="utf-8").write(page)
    print("pwa:", os.path.relpath(folder, ROOT), name, color)

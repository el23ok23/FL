#!/usr/bin/env python3
"""
Crea la app y la web de una nueva empresa de fletes (marca blanca de Fletaba) en un solo paso.

Uso:
  python3 scripts/nueva_empresa.py --slug donpedro --nombre "Fletes Don Pedro" --whatsapp 5492474123456 \
      --origen salto --color "#0E7C4A" --acento "#F6B40E" [--precios-de salto]

Qué hace:
  1. app/build.gradle.kts: agrega la variante <slug> (id com.fletaba.<slug>, nombre de la app).
  2. app/src/<slug>/assets/config.json: copia la configuración base (caba o salto) con el nombre,
     WhatsApp, colores y zona de origen de la empresa.
  3. app/src/<slug>/res/values/colors.xml: color de fondo del ícono.
  4. docs/<slug>/index.html: la versión web con la misma configuración.
  5. .github/workflows/android.yml: compila y publica <Nombre>.apk en Releases.
Después: commit + push, y en unos minutos el APK queda en
  https://github.com/el23ok23/FL/releases/download/ultima-version/<NombreSinEspacios>.apk
"""
import argparse, json, os, re, sys, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def die(msg):
    print("ERROR:", msg); sys.exit(1)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--slug", required=True, help="identificador corto, solo minúsculas y números (ej. donpedro)")
    ap.add_argument("--nombre", required=True, help='nombre de la app (ej. "Fletes Don Pedro")')
    ap.add_argument("--whatsapp", required=True, help="549 + área + número (ej. 5492474123456)")
    ap.add_argument("--origen", default="salto", choices=["caba", "salto"], help="zona de origen")
    ap.add_argument("--color", default="#1B7BE0", help="color principal #RRGGBB")
    ap.add_argument("--acento", default="#F6B40E", help="color de acento #RRGGBB")
    ap.add_argument("--precios-de", default=None, choices=["caba", "salto"], help="de qué variante copiar los precios (por defecto, la del origen)")
    ap.add_argument("--comision", type=float, default=None, help="comisión %% sobre el bruto (por defecto la de la base)")
    a = ap.parse_args()

    slug = a.slug.lower()
    if not re.fullmatch(r"[a-z][a-z0-9]{1,20}", slug): die("el slug debe ser minúsculas/números, empezar con letra (ej. donpedro)")
    if not re.fullmatch(r"#[0-9A-Fa-f]{6}", a.color) or not re.fullmatch(r"#[0-9A-Fa-f]{6}", a.acento): die("colores en formato #RRGGBB")
    wa = re.sub(r"\D", "", a.whatsapp)
    if not wa.startswith("549") or len(wa) != 13: die("WhatsApp en formato 549 + área + número, 13 dígitos (ej. 5492474123456)")
    base = a.precios_de or a.origen
    app_id = f"com.fletaba.{slug}"
    release_name = re.sub(r"[^A-Za-z0-9]", "", a.nombre) or slug.capitalize()

    # 1. Gradle
    gradle = os.path.join(ROOT, "app", "build.gradle.kts"); g = open(gradle).read()
    if f'create("{slug}")' in g: die(f"la variante {slug} ya existe")
    block = f'''        create("{slug}") {{
            dimension = "origen"
            applicationId = "{app_id}"
            resValue("string", "app_name", {json.dumps(a.nombre, ensure_ascii=False)})
        }}
'''
    marker = '        create("salto") {'
    i = g.index(marker); j = g.index("        }\n", i) + len("        }\n")
    g = g[:j] + block + g[j:]
    open(gradle, "w").write(g)

    # 2. config.json
    src = os.path.join(ROOT, "app", "src", base, "assets", "config.json")
    cfg = json.load(open(src), object_pairs_hook=collections.OrderedDict)
    cfg["_ayuda"] = re.sub(r"Archivo de configuración de [^.]*\.", f"Archivo de configuración de {a.nombre}.", cfg["_ayuda"])
    cfg["negocio"]["nombre"] = a.nombre
    cfg["negocio"]["whatsapp_dueno"] = wa
    cfg["marca"]["color_principal"] = a.color.upper()
    cfg["marca"]["color_acento"] = a.acento.upper()
    if a.origen != base:
        ocfg = json.load(open(os.path.join(ROOT, "app", "src", a.origen, "assets", "config.json")), object_pairs_hook=collections.OrderedDict)
        cfg["origen"] = ocfg["origen"]
    if a.comision is not None: cfg["precios"]["comision_porcentaje"] = a.comision
    dst_dir = os.path.join(ROOT, "app", "src", slug, "assets"); os.makedirs(dst_dir, exist_ok=True)
    s = json.dumps(cfg, ensure_ascii=False, indent=2)
    s = re.sub(r'\{\s+"id": "([^"]+)",\s+"nombre": "([^"]+)",\s+"descripcion": "([^"]+)",\s+"medidas": "([^"]+)",\s+"carga": "([^"]+)",\s+"tarifa_base": (\d+),\s+"por_km": (\d+),\s+"minutos_carga": (\d+),\s+"m3": ([\d.]+)\s+\}',
               r'{ "id": "\1", "nombre": "\2", "descripcion": "\3", "medidas": "\4", "carga": "\5", "tarifa_base": \6, "por_km": \7, "minutos_carga": \8, "m3": \9 }', s)
    s = re.sub(r'"(palabras_fuera|palabras_dentro|poligono|calles)": \[\s*\]', r'"\1": []', s)
    s = re.sub(r'"palabras_(fuera|dentro)": \[\s+((?:"[^"]*",?\s+)+)\]', lambda m: '"palabras_%s": [%s]' % (m.group(1), ", ".join(x.strip() for x in m.group(2).strip().rstrip(',').split(','))), s)
    open(os.path.join(dst_dir, "config.json"), "w").write(s + "\n")

    # 3. Color del ícono
    res_dir = os.path.join(ROOT, "app", "src", slug, "res", "values"); os.makedirs(res_dir, exist_ok=True)
    open(os.path.join(res_dir, "colors.xml"), "w").write(f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="brand">{a.color.upper()}</color>
    <color name="ic_launcher_background">{a.color.upper()}</color>
</resources>
''')

    # 4. Web
    web_src = os.path.join(ROOT, "docs", "index.html" if a.origen == "caba" else os.path.join("salto", "index.html"))
    w = open(web_src).read()
    w = re.sub(r'negocio: \{ nombre: "[^"]*", whatsapp_dueno: "[^"]*"', f'negocio: {{ nombre: {json.dumps(a.nombre, ensure_ascii=False)}, whatsapp_dueno: "{wa}"', w, count=1)
    w = re.sub(r"<title>[^<]*</title>", f"<title>{a.nombre}</title>", w, count=1)
    w = w.replace("--celeste:#1B7BE0; --celeste-oscuro:#0F5FB8; --sol:#F6B40E;", f"--celeste:{a.color.upper()}; --celeste-oscuro:{a.color.upper()}; --sol:{a.acento.upper()};")
    w = re.sub(r'<div id="brandname">FLETABA.*?<small', f'<div id="brandname">{a.nombre.upper()}<small', w, count=1, flags=re.S)
    if a.comision is not None: w = re.sub(r"comision_porcentaje: [\d.]+", f"comision_porcentaje: {a.comision:g}", w)
    web_dir = os.path.join(ROOT, "docs", slug); os.makedirs(web_dir, exist_ok=True)
    open(os.path.join(web_dir, "index.html"), "w").write(w)

    # 5. CI
    wf = os.path.join(ROOT, ".github", "workflows", "android.yml"); y = open(wf).read()
    y = y.replace("          cp app/build/outputs/bundle/saltoRelease/app-salto-release.aab release/FletabaSalto-release.aab\n",
                  "          cp app/build/outputs/bundle/saltoRelease/app-salto-release.aab release/FletabaSalto-release.aab\n"
                  f"          cp app/build/outputs/apk/{slug}/debug/app-{slug}-debug.apk release/{release_name}.apk\n"
                  f"          cp app/build/outputs/bundle/{slug}Release/app-{slug}-release.aab release/{release_name}-release.aab\n")
    y = y.replace("            release/FletabaSalto-release.aab\n", f"            release/FletabaSalto-release.aab\n            release/{release_name}.apk\n            release/{release_name}-release.aab\n")
    open(wf, "w").write(y)

    print(f"""Listo: {a.nombre}
  App:   variante '{slug}' · id {app_id} · config en app/src/{slug}/assets/config.json
  Web:   docs/{slug}/index.html  →  https://el23ok23.github.io/FL/{slug}/
  APK:   tras el push, https://github.com/el23ok23/FL/releases/download/ultima-version/{release_name}.apk
Siguiente paso: git add -A && git commit -m "Nueva empresa: {a.nombre}" && git push""")

if __name__ == "__main__":
    main()

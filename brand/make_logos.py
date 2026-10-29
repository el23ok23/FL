#!/usr/bin/env python3
"""Genera los SVG del logo de Fletaba con la tipografía embebida (Inter, subconjunto)."""
import base64, os, sys
here = os.path.dirname(os.path.abspath(__file__))
subset_dir = sys.argv[1] if len(sys.argv) > 1 else here
EB = base64.b64encode(open(os.path.join(subset_dir, 'inter-eb-sub.otf'), 'rb').read()).decode()
MD = base64.b64encode(open(os.path.join(subset_dir, 'inter-md-sub.otf'), 'rb').read()).decode()

CELESTE, SOL, NOCHE, BLANCO, NEGRO = '#1B7BE0', '#F6B40E', '#0B1F3A', '#FFFFFF', '#111111'

STYLE = f"""<style>
@font-face{{font-family:'FletabaEB';src:url(data:font/otf;base64,{EB}) format('opentype');}}
@font-face{{font-family:'FletabaMD';src:url(data:font/otf;base64,{MD}) format('opentype');}}
.w{{font-family:'FletabaEB',Inter,Arial,sans-serif;font-weight:800;letter-spacing:-0.035em}}
.t{{font-family:'FletabaMD',Inter,Arial,sans-serif;font-weight:500}}
</style>"""

def mark(x, y, size, bg, truck, sun, window, bg_shape='square'):
    """Isotipo: cuadrado redondeado (o círculo) con sol y camión. Coordenadas base 108×108."""
    s = size / 108.0
    shape = (f'<rect x="{x}" y="{y}" width="{size}" height="{size}" rx="{size*0.225}" fill="{bg}"/>' if bg_shape == 'square'
             else f'<circle cx="{x+size/2}" cy="{y+size/2}" r="{size/2}" fill="{bg}"/>')
    if bg == 'none': shape = ''
    g = f'<g transform="translate({x},{y}) scale({s})">'
    g += f'<circle cx="72" cy="33" r="8" fill="{sun}"/>'
    g += f'<path fill="{sun}" d="M72,20.5 l1.6,3.4 h-3.2 z M72,45.5 l1.6,-3.4 h-3.2 z M59.5,33 l3.4,1.6 v-3.2 z M84.5,33 l-3.4,1.6 v-3.2 z M63.2,24.2 l3.5,1.3 l-2.2,2.2 z M80.8,24.2 l-3.5,1.3 l2.2,2.2 z M63.2,41.8 l3.5,-1.3 l-2.2,-2.2 z M80.8,41.8 l-3.5,-1.3 l2.2,-2.2 z"/>'
    g += f'<path fill="{truck}" d="M27,45 H58 A3,3 0 0 1 61,48 V70 H24 V48 A3,3 0 0 1 27,45 Z"/>'
    g += f'<path fill="{truck}" d="M61,52 H72 A3.5,3.5 0 0 1 74.8,53.4 L81.4,62.2 A3.5,3.5 0 0 1 82.1,64.3 V67 A3,3 0 0 1 79.1,70 H61 Z"/>'
    g += f'<path fill="{window}" d="M64.5,55.2 H71.6 L77.4,63 H64.5 Z"/>'
    g += f'<circle cx="32.5" cy="72" r="5.6" fill="{truck}"/><circle cx="32.5" cy="72" r="2.2" fill="{window}"/>'
    g += f'<circle cx="73" cy="72" r="5.6" fill="{truck}"/><circle cx="73" cy="72" r="2.2" fill="{window}"/>'
    g += '</g>'
    return shape + g

def svg(w, h, body, bg=None):
    rect = f'<rect width="{w}" height="{h}" fill="{bg}"/>' if bg else ''
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}">{STYLE}{rect}{body}</svg>'

def horizontal(variant):
    if variant == 'color':
        m = mark(40, 40, 320, CELESTE, BLANCO, SOL, CELESTE); word, tag = NOCHE, CELESTE
    elif variant == 'blanco':
        m = mark(40, 40, 320, 'none', BLANCO, SOL, '#00000000'); word, tag = BLANCO, SOL
    else:  # negro
        m = mark(40, 40, 320, 'none', NEGRO, NEGRO, BLANCO); word, tag = NEGRO, NEGRO
    body = m + f'<text x="412" y="262" class="w" font-size="196" fill="{word}">FLETABA</text>'
    body += f'<text x="418" y="332" class="t" font-size="46" fill="{tag}">Fletes desde Capital Federal a todo el país</text>'
    return svg(1400, 400, body)

def vertical(variant):
    if variant == 'color':
        m = mark(240, 40, 320, CELESTE, BLANCO, SOL, CELESTE); word, tag = NOCHE, CELESTE
    elif variant == 'blanco':
        m = mark(240, 40, 320, 'none', BLANCO, SOL, '#00000000'); word, tag = BLANCO, SOL
    else:
        m = mark(240, 40, 320, 'none', NEGRO, NEGRO, BLANCO); word, tag = NEGRO, NEGRO
    body = m + f'<text x="400" y="560" text-anchor="middle" class="w" font-size="170" fill="{word}">FLETABA</text>'
    body += f'<text x="400" y="622" text-anchor="middle" class="t" font-size="38" fill="{tag}">Fletes desde Capital Federal a todo el país</text>'
    return svg(800, 680, body)

def isotipo(variant, shape):
    if variant == 'color': m = mark(0, 0, 512, CELESTE, BLANCO, SOL, CELESTE, shape)
    elif variant == 'blanco': m = mark(0, 0, 512, 'none', BLANCO, SOL, '#00000000', shape)
    else: m = mark(0, 0, 512, 'none', NEGRO, NEGRO, BLANCO, shape)
    return svg(512, 512, m)

def wordmark(variant):
    color = {'color': NOCHE, 'blanco': BLANCO, 'negro': NEGRO}[variant]
    return svg(1000, 220, f'<text x="20" y="178" class="w" font-size="196" fill="{color}">FLETABA</text>')

out = os.path.join(here, 'logo')
os.makedirs(out, exist_ok=True)
files = {}
for v in ['color', 'blanco', 'negro']:
    files[f'fletaba-logo-horizontal-{v}.svg'] = horizontal(v)
    files[f'fletaba-logo-vertical-{v}.svg'] = vertical(v)
    files[f'fletaba-isotipo-{v}.svg'] = isotipo(v, 'square')
    files[f'fletaba-isotipo-circular-{v}.svg'] = isotipo(v, 'circle')
    files[f'fletaba-wordmark-{v}.svg'] = wordmark(v)
for name, content in files.items():
    open(os.path.join(out, name), 'w').write(content)
print(len(files), 'SVG generados en', out)

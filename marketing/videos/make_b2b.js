// Videos para vender la app a empresas de fletes (Salto): tarjetas de texto + demo real grabada de la web
// de la empresa de ejemplo "Fletes Don Pedro". 1080×1920, música sintetizada propia.
// Uso: node marketing/videos/make_b2b.js   (necesita Playwright/Chromium y ffmpeg)
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path'), { execSync } = require('child_process');
const ROOT = path.join(__dirname, '..', '..');
const OUT = path.join(__dirname, 'mp4'); fs.mkdirSync(OUT, { recursive: true });
const TMP = process.env.B2B_TMP || path.join(require('os').tmpdir(), 'fletaba-b2b'); fs.rmSync(TMP, { recursive: true, force: true }); fs.mkdirSync(TMP, { recursive: true });
const DEMO = 'file://' + path.join(ROOT, 'docs', 'donpedro', 'index.html');
const ISO = fs.readFileSync(path.join(ROOT, 'brand', 'logo', 'fletaba-isotipo-blanco.svg'), 'utf8').replace(/width="\d+" height="\d+"/, 'width="150" height="150"');
const CEL = '#1B7BE0', SOL = '#F6B40E', NOCHE = '#0B1F3A';
const sleep = ms => new Promise(r => setTimeout(r, ms));

// Tarjeta de texto a pantalla completa, con entrada animada por CSS.
const card = (lines, { bg = NOCHE, accent = SOL, eyebrow = '', small = '', logo = false } = {}) => `<!doctype html><html><head><meta charset="utf-8"><style>
body{margin:0;width:1080px;height:1920px;background:${bg};color:#fff;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,sans-serif;overflow:hidden;display:flex;flex-direction:column;justify-content:center;padding:0 90px;box-sizing:border-box}
.eye{font-size:30px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:${accent};margin-bottom:34px;opacity:0;animation:up .6s .1s forwards}
.h{font-size:108px;font-weight:800;letter-spacing:-.035em;line-height:1.02;opacity:0;animation:up .7s forwards}
.h b{color:${accent}}
.s{font-size:42px;font-weight:500;line-height:1.3;opacity:.92;margin-top:40px;opacity:0;animation:up .7s forwards}
.logo{position:absolute;left:90px;top:120px;opacity:0;animation:pop .7s .05s forwards}
.dot{position:absolute;right:-180px;bottom:-180px;width:620px;height:620px;border-radius:50%;background:${accent};opacity:.18}
@keyframes up{from{opacity:0;transform:translateY(60px)}to{opacity:1;transform:none}}
@keyframes pop{from{opacity:0;transform:scale(.6)}to{opacity:1;transform:none}}
</style></head><body><div class="dot"></div>${logo ? `<div class="logo">${ISO}</div>` : ''}${eyebrow ? `<div class="eye">${eyebrow}</div>` : ''}${lines.map((l, i) => `<div class="h" style="animation-delay:${0.25 + i * 0.35}s">${l}</div>`).join('')}${small ? `<div class="s" style="animation-delay:${0.4 + lines.length * 0.35}s">${small}</div>` : ''}</body></html>`;

const CAPTION_CSS = 'position:fixed;left:16px;right:16px;bottom:120px;z-index:99999;background:rgba(11,31,58,.93);color:#fff;font:600 19px/1.3 Inter,system-ui,sans-serif;padding:14px 18px;border-radius:16px;text-align:center;box-shadow:0 10px 30px rgba(0,0,0,.25);transition:opacity .3s';

async function make(name, dur, bpm, steps) {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1, recordVideo: { dir: TMP, size: { width: 1080, height: 1920 } }, colorScheme: 'light' });
  const p = await ctx.newPage();
  let n = 0;
  const api = {
    card: async (lines, opts, ms) => { const f = path.join(TMP, `${name}-c${n++}.html`); fs.writeFileSync(f, card(lines, opts)); await p.goto('file://' + f); await sleep(ms); },
    demo: async () => { await p.goto(DEMO); await p.addStyleTag({ content: 'html{zoom:2}' }); await sleep(900); },
    cap: async (t) => p.evaluate(([t, css]) => { let c = document.getElementById('__cap'); if (!c) { c = document.createElement('div'); c.id = '__cap'; c.style.cssText = css; document.body.appendChild(c); } c.textContent = t; c.style.opacity = t ? '1' : '0'; }, [t, CAPTION_CSS]),
    click: async (sel) => p.click(sel),
    type: async (sel, text) => { await p.fill(sel, ''); for (const ch of text) await p.type(sel, ch, { delay: 55 }); },
    ev: async (fn) => p.evaluate(fn),
    scrollTo: async (sel) => p.evaluate(s => { const el = document.querySelector(s); if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' }); }, sel),
    sleep,
  };
  await steps(api);
  await ctx.close(); await b.close();
  const webm = fs.readdirSync(TMP).filter(x => x.endsWith('.webm')).map(x => path.join(TMP, x)).sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs)[0];
  const real = parseFloat(execSync(`ffprobe -v error -show_entries format=duration -of csv=p=0 "${webm}"`).toString());
  const wav = path.join(TMP, `${name}.wav`);
  execSync(`python3 "${path.join(__dirname, 'make_audio.py')}" "${wav}" ${real.toFixed(2)} ${bpm} 2 whoosh:0.2 ding:2.0`, { stdio: 'inherit' });
  const out = path.join(OUT, `${name}.mp4`);
  execSync(`ffmpeg -y -loglevel error -i "${webm}" -i "${wav}" -c:v libx264 -preset medium -crf 20 -pix_fmt yuv420p -r 30 -movflags +faststart -c:a aac -b:a 160k -shortest "${out}"`);
  fs.unlinkSync(webm);
  console.log('ok', path.relative(ROOT, out), real.toFixed(1) + 's');
}

(async () => {
  // 1. Tu propia app
  await make('b2b-01-tu-propia-app', 40, 112, async (a) => {
    await a.card(['¿Tenés una empresa', 'de fletes en <b>Salto</b>?'], { eyebrow: 'Para empresas de fletes', logo: true }, 3200);
    await a.card(['Tené tu <b>propia app</b>', 'con tu nombre y tu logo.'], { bg: CEL, accent: SOL }, 3200);
    await a.demo(); await a.cap('Así la ve tu cliente: tu marca, tus colores'); await a.sleep(2400);
    await a.click('#demo'); await a.cap('Escribe de dónde a dónde…'); await a.sleep(2200);
    await a.click('#cta'); await a.cap('…elige el vehículo…'); await a.sleep(1200); await a.click('#next'); await a.sleep(1500);
    await a.click('#cta'); await a.cap('…pisos, qué lleva y cuándo…'); await a.sleep(2200);
    await a.click('#cta'); await a.cap('…y ve el precio al instante, con tus tarifas'); await a.sleep(2800);
    await a.card(['Tus clientes piden', 'en <b>un minuto</b>.', 'Vos recibís el pedido', 'en tu <b>WhatsApp</b>.'], {}, 3600);
    await a.card(['Un solo pago.', '<b>Sin comisiones.</b>', '<b>Sin abonos.</b>'], { bg: CEL, accent: SOL, small: 'Respondé este mensaje y te la muestro con tu nombre.' }, 4000);
  });

  // 2. Cómo te llegan los pedidos
  await make('b2b-02-como-llegan-los-pedidos', 45, 116, async (a) => {
    await a.card(['¿Cómo te llegan', 'los <b>pedidos</b>?'], { eyebrow: 'Fletaba para empresas', logo: true }, 2800);
    await a.demo(); await a.click('#demo'); await a.cap('El cliente arma el pedido'); await a.sleep(1500);
    await a.click('#cta'); await a.sleep(900); await a.click('#cta'); await a.sleep(600);
    await a.scrollTo('#roundtrip'); await a.cap('Puede pedir ida y vuelta y adjuntar fotos'); await a.sleep(700); await a.click('#roundtrip'); await a.sleep(1600);
    await a.click('#cta'); await a.cap('Precio claro, con el detalle'); await a.sleep(1800);
    await a.scrollTo('#name'); await a.type('#name', 'Ana'); await a.type('#phone', '11 2345 6789'); await a.sleep(400);
    await a.click('#cta'); await a.cap('Lo envía. Vos lo recibís por WhatsApp y por correo'); await a.sleep(2600);
    await a.cap('El cliente ve el estado de su pedido'); await a.scrollTo('#orderstatus'); await a.sleep(2200);
    await a.ev(() => panelOpen()); await a.sleep(500); await a.type('#panelpin', 'donpedro2026'); await a.click('#panelenter'); await a.sleep(700);
    await a.click('button[data-ptab="1"]'); await a.cap('En tu panel: todos los pedidos'); await a.sleep(2000);
    await a.ev(() => { const b = document.querySelector('button[data-estado$="|confirmado"]'); if (b) b.click(); }); await a.cap('Lo marcás "Confirmado" y el cliente lo ve al instante'); await a.sleep(2600);
    await a.card(['Historial en <b>tu planilla</b>.', 'Aviso por <b>correo</b>.', 'WhatsApp a <b>tus números</b>.'], {}, 3600);
    await a.card(['Un solo pago.', '<b>Sin comisiones.</b>', '<b>Sin abonos.</b>'], { bg: CEL, accent: SOL, small: 'Respondé este mensaje y te la muestro con tu nombre.' }, 3600);
  });

  // 3. Vos manejás todo
  await make('b2b-03-vos-manejas-todo', 40, 108, async (a) => {
    await a.card(['Precios, nombre,', 'colores y logo:', '<b>los manejás vos.</b>'], { eyebrow: 'Fletaba para empresas', logo: true }, 3200);
    await a.demo(); await a.ev(() => panelOpen()); await a.cap('Entrás al panel con tu PIN'); await a.sleep(600);
    await a.type('#panelpin', 'donpedro2026'); await a.click('#panelenter'); await a.sleep(900);
    await a.cap('Elegís tus colores y los ves al instante'); await a.scrollTo('button[data-sw^="color_primario"]'); await a.sleep(600);
    await a.click('button[data-sw="color_primario|#7C3AED"]'); await a.sleep(1000); await a.click('button[data-sw="color_primario|#DC2626"]'); await a.sleep(1000); await a.click('button[data-sw="color_primario|"]'); await a.sleep(800);
    await a.scrollTo('input[data-pk="kangoo.tarifa_base"]'); await a.cap('Cambiás un precio…'); await a.sleep(700); await a.type('input[data-pk="kangoo.tarifa_base"]', '21000'); await a.sleep(900);
    await a.scrollTo('input[data-pk="recargo_fin_de_semana_porcentaje"]'); await a.cap('…un recargo de fin de semana, un descuento de ida y vuelta…'); await a.sleep(700); await a.type('input[data-pk="recargo_fin_de_semana_porcentaje"]', '15'); await a.sleep(1200);
    await a.scrollTo('#panelsave'); await a.cap('Guardar. Rige en la app de todos tus clientes y en la web'); await a.sleep(800); await a.click('#panelsave'); await a.sleep(2000);
    await a.card(['Sin llamar a nadie.', 'Sin esperar.', '<b>Desde tu celular.</b>'], {}, 3200);
    await a.card(['Un solo pago.', '<b>Sin comisiones.</b>', '<b>Sin abonos.</b>'], { bg: CEL, accent: SOL, small: 'Respondé este mensaje y te la muestro con tu nombre.' }, 3600);
  });
})();

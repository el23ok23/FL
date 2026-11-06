// Un video por empresa de fletes de Salto, con su nombre: tarjetas + demo real de la web con el nombre
// de la empresa aplicado en vivo. Lee ventas/empresas_salto.json. Uso: node marketing/videos/make_empresas.js [slug]
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path'), { execSync } = require('child_process');
const ROOT = path.join(__dirname, '..', '..');
const OUT = path.join(__dirname, 'mp4'); fs.mkdirSync(OUT, { recursive: true });
const TMP = process.env.B2B_TMP || path.join(require('os').tmpdir(), 'fletaba-empresas'); fs.rmSync(TMP, { recursive: true, force: true }); fs.mkdirSync(TMP, { recursive: true });
const DEMO = 'file://' + path.join(ROOT, 'docs', 'donpedro', 'index.html');
const ISO = fs.readFileSync(path.join(ROOT, 'brand', 'logo', 'fletaba-isotipo-blanco.svg'), 'utf8').replace(/width="\d+" height="\d+"/, 'width="150" height="150"');
const EMPRESAS = JSON.parse(fs.readFileSync(path.join(ROOT, 'ventas', 'empresas_salto.json'), 'utf8'));
const only = process.argv[2];
const SOL = '#F6B40E', NOCHE = '#0B1F3A';
const sleep = ms => new Promise(r => setTimeout(r, ms));
const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;');

const card = (lines, { bg = NOCHE, accent = SOL, eyebrow = '', small = '', logo = false } = {}) => `<!doctype html><html><head><meta charset="utf-8"><style>
body{margin:0;width:1080px;height:1920px;background:${bg};color:#fff;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,sans-serif;overflow:hidden;display:flex;flex-direction:column;justify-content:center;padding:0 90px;box-sizing:border-box}
.eye{font-size:30px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:${accent};margin-bottom:34px;opacity:0;animation:up .6s .1s forwards}
.h{font-size:100px;font-weight:800;letter-spacing:-.035em;line-height:1.02;opacity:0;animation:up .7s forwards;text-wrap:balance}
.h b{color:${accent}}
.s{font-size:42px;font-weight:500;line-height:1.3;margin-top:40px;opacity:0;animation:up .7s forwards}
.logo{position:absolute;left:90px;top:120px;opacity:0;animation:pop .7s .05s forwards}
.dot{position:absolute;right:-180px;bottom:-180px;width:620px;height:620px;border-radius:50%;background:${accent};opacity:.18}
@keyframes up{from{opacity:0;transform:translateY(60px)}to{opacity:1;transform:none}}
@keyframes pop{from{opacity:0;transform:scale(.6)}to{opacity:1;transform:none}}
</style></head><body><div class="dot"></div>${logo ? `<div class="logo">${ISO}</div>` : ''}${eyebrow ? `<div class="eye">${eyebrow}</div>` : ''}${lines.map((l, i) => `<div class="h" style="animation-delay:${0.25 + i * 0.35}s">${l}</div>`).join('')}${small ? `<div class="s" style="animation-delay:${0.4 + lines.length * 0.35}s">${small}</div>` : ''}</body></html>`;

const CAPTION_CSS = 'position:fixed;left:16px;right:16px;bottom:120px;z-index:99999;background:rgba(11,31,58,.93);color:#fff;font:600 19px/1.3 Inter,system-ui,sans-serif;padding:14px 18px;border-radius:16px;text-align:center;box-shadow:0 10px 30px rgba(0,0,0,.25);transition:opacity .3s';

async function make(e) {
  const name = `empresa-${e.slug}`;
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1, recordVideo: { dir: TMP, size: { width: 1080, height: 1920 } }, colorScheme: 'light' });
  const p = await ctx.newPage();
  let n = 0;
  const showCard = async (lines, opts, ms) => { const f = path.join(TMP, `${name}-c${n++}.html`); fs.writeFileSync(f, card(lines, opts)); await p.goto('file://' + f); await sleep(ms); };
  const cap = (t) => p.evaluate(([t, css]) => { let c = document.getElementById('__cap'); if (!c) { c = document.createElement('div'); c.id = '__cap'; c.style.cssText = css; document.body.appendChild(c); } c.textContent = t; c.style.opacity = t ? '1' : '0'; }, [t, CAPTION_CSS]);
  const short = e.nombre.length > 26 ? e.nombre.split(' ').slice(0, 3).join(' ') : e.nombre;

  await showCard([`Para <b>${esc(short)}</b>`, 'de Salto.'], { eyebrow: 'Un video de 30 segundos', logo: true }, 3000);
  await showCard(['Así se vería', '<b>su propia WebApp</b>', 'de fletes.'], { bg: e.color || '#1B7BE0', accent: SOL, small: 'Se abre desde un enlace y se instala en el celular, sin tienda.' }, 3200);
  // Demo con el nombre y el color de la empresa aplicados en vivo
  await p.goto(DEMO); await p.addStyleTag({ content: 'html{zoom:2}' });
  await p.evaluate(([nombre, color]) => { applyBrand('nombre', nombre); applyBrand('color_primario', color); render(); }, [e.nombre, e.color || '#1B7BE0']);
  await sleep(500);
  await cap(`La WebApp con el nombre de ${short} y sus colores`); await sleep(2300);
  await p.click('#demo'); await cap('El cliente escribe de dónde a dónde…'); await sleep(2000);
  await p.click('#cta'); await cap('…elige el vehículo…'); await sleep(900); await p.click('#next'); await sleep(1400);
  await p.click('#cta'); await cap('…pisos, qué lleva y cuándo…'); await sleep(2000);
  await p.click('#cta'); await cap(`…y ve el precio al instante, con las tarifas de ${short}`); await sleep(2800);
  await p.evaluate(() => document.querySelector('#name').scrollIntoView({ behavior: 'smooth', block: 'center' })); await sleep(300);
  await p.fill('#name', 'Ana'); await p.fill('#phone', '11 2345 6789'); await sleep(300); await p.click('#cta');
  await cap('Lo envía, y el pedido les llega por WhatsApp'); await sleep(2600);
  await showCard(['Sus clientes piden', 'en <b>un minuto</b>.', 'Ustedes reciben el pedido', 'en el <b>WhatsApp</b>.'], {}, 3400);
  await showCard(['Precios, nombre y logo', '<b>los manejan ustedes</b>,', 'desde el panel del dueño.'], { bg: e.color || '#1B7BE0', accent: SOL }, 3000);
  await showCard(['Un solo pago.', '<b>Sin comisiones.</b>', '<b>Sin abonos.</b>'], { small: `Respondan este mensaje y les dejo la WebApp funcionando con sus precios y su WhatsApp.` }, 3800);

  await ctx.close(); await b.close();
  const webm = fs.readdirSync(TMP).filter(x => x.endsWith('.webm')).map(x => path.join(TMP, x)).sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs)[0];
  const real = parseFloat(execSync(`ffprobe -v error -show_entries format=duration -of csv=p=0 "${webm}"`).toString());
  const wav = path.join(TMP, `${name}.wav`);
  execSync(`python3 "${path.join(__dirname, 'make_audio.py')}" "${wav}" ${real.toFixed(2)} 112 2 whoosh:0.2 ding:2.0`, { stdio: 'ignore' });
  const out = path.join(OUT, `${name}.mp4`);
  execSync(`ffmpeg -y -loglevel error -i "${webm}" -i "${wav}" -c:v libx264 -preset medium -crf 20 -pix_fmt yuv420p -r 30 -movflags +faststart -c:a aac -b:a 160k -shortest "${out}"`);
  fs.unlinkSync(webm);
  console.log('ok', path.relative(ROOT, out), real.toFixed(1) + 's');
}

(async () => {
  for (const e of EMPRESAS) { if (only && e.slug !== only) continue; await make(e); }
})();

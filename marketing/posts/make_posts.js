// Genera 10 posts 1080×1080 (feed) y 1080×1920 (historias) para Fletaba con Playwright.
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
const VEH = fs.readFileSync(path.join(__dirname, '..', 'vehicles.js'), 'utf8');
const LOGO_W = fs.readFileSync(path.join(__dirname, '..', '..', 'brand', 'logo', 'fletaba-logo-horizontal-blanco.svg'), 'utf8');
const LOGO_C = fs.readFileSync(path.join(__dirname, '..', '..', 'brand', 'logo', 'fletaba-logo-horizontal-color.svg'), 'utf8');
const ISO_W = fs.readFileSync(path.join(__dirname, '..', '..', 'brand', 'logo', 'fletaba-isotipo-blanco.svg'), 'utf8');
const CEL = '#1B7BE0', SOL = '#F6B40E', NOCHE = '#0B1F3A', CLARO = '#F4F8FE';
const logo = (svg, w) => svg.replace(/width="\d+" height="\d+"/, `width="${w}" height="${Math.round(w * 400 / 1400)}"`);
const iso = (w) => ISO_W.replace(/width="\d+" height="\d+"/, `width="${w}" height="${w}"`);

const base = (bg, fg, inner, story) => `<html><head><meta charset="utf-8"><style>
@font-face{font-family:Inter;src:local('Inter');} body{margin:0;width:1080px;height:${story ? 1920 : 1080}px;background:${bg};color:${fg};font-family:Inter,system-ui,sans-serif;overflow:hidden;position:relative}
.pad{position:absolute;inset:0;padding:${story ? '140px 90px' : '80px'};display:flex;flex-direction:column}
h1{font-weight:800;letter-spacing:-.035em;line-height:.98;margin:0}
p{font-weight:500;line-height:1.3;margin:0}
.eyebrow{font-size:28px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;opacity:.85}
.foot{margin-top:auto;display:flex;align-items:center;justify-content:space-between;gap:20px}
.pill{display:inline-block;border-radius:999px;padding:14px 26px;font-weight:700;font-size:30px}
.sun{position:absolute;border-radius:50%;background:${SOL};filter:blur(0)}
.card{background:#fff;color:${NOCHE};border-radius:36px;padding:34px 40px;box-shadow:0 30px 80px rgba(11,31,58,.25)}
.badge{display:inline-grid;place-items:center;width:64px;height:64px;border-radius:50%;color:#fff;font-weight:800;font-size:30px}
</style></head><body>${inner}</body>
<script>${VEH}
document.querySelectorAll('[data-veh]').forEach(el=>{const [k,w,h,b,d]=el.dataset.veh.split('|');el.appendChild(vehicleCanvas(k,+w,+h,b,d,2));});
</script></html>`;

function posts(story) {
  const S = story; const H1 = S ? 118 : 90; const H2 = S ? 92 : 84; const P = S ? 42 : 34;
  return [
    { name: '01-lanzamiento', html: base(CEL, '#fff', `<div class="sun" style="width:${S?520:420}px;height:${S?520:420}px;right:-120px;top:${S?120:-80}px;opacity:.95"></div>
      <div class="pad"><div class="eyebrow">Nueva app</div><h1 style="font-size:${S?150:128}px;margin-top:30px">Pedí tu flete<br>al toque.</h1>
      <p style="font-size:${P+6}px;margin-top:40px;max-width:820px">Desde Capital Federal a todo el país. Precio aproximado en segundos, sin llamar a nadie.</p>
      <div data-veh="furgon_mediano|${S?820:560}|${S?460:314}|#FFFFFF|#0B1F3A" style="margin:${S?60:20}px auto 0"></div>
      <div class="foot">${logo(LOGO_W, 440)}<span class="pill" style="background:${SOL};color:#3b2a00">Descargala gratis</span></div></div>`, S) },
    { name: '02-precio-al-toque', html: base(CLARO, NOCHE, `<div class="pad"><div class="eyebrow" style="color:${CEL}">Sin vueltas</div><h1 style="font-size:${H1}px;margin-top:30px">Sabé cuánto sale<br>antes de pedirlo.</h1>
      <div class="card" style="margin-top:${S?80:50}px;background:linear-gradient(135deg,${CEL},#0F5FB8);color:#fff"><div style="opacity:.85;font-size:30px">Total estimado</div><div style="font-size:${S?150:120}px;font-weight:800;letter-spacing:-.04em;line-height:1">$ 89.000</div><div style="font-size:32px;margin-top:8px">Kangoo · 10,0 km · 1 ayudante</div></div>
      <p style="font-size:${P}px;margin-top:40px;color:#475569">Tarifa, recorrido, ayudantes y pisos: todo desglosado. Después lo confirmás por WhatsApp.</p>
      <div class="foot">${logo(LOGO_C, 440)}</div></div>`, S) },
    { name: '03-todo-el-pais', html: base(NOCHE, '#fff', `<div class="sun" style="width:240px;height:240px;right:-70px;top:${S?1380:700}px"></div>
      <div class="pad"><div class="eyebrow" style="color:${SOL}">Cobertura</div><h1 style="font-size:${H1}px;margin-top:30px">Salís de Capital.<br>Llegás a donde sea.</h1>
      <p style="font-size:${P}px;margin-top:40px;max-width:860px;opacity:.9">Mar del Plata, Córdoba, Rosario, Bariloche. El origen siempre en Capital Federal; el destino, en cualquier punto de la Argentina.</p>
      <div style="margin-top:${S?90:60}px;display:flex;gap:18px;flex-wrap:wrap">${['Buenos Aires','Rosario','Córdoba','Mendoza','Mar del Plata','Salta','Neuquén'].map(c=>`<span class="pill" style="background:rgba(255,255,255,.12);color:#fff;font-size:28px">${c}</span>`).join('')}</div>
      <div class="foot">${logo(LOGO_W, 440)}</div></div>`, S) },
    { name: '04-paradas', html: base('#fff', NOCHE, `<div class="pad"><div class="eyebrow" style="color:${CEL}">Nuevo</div><h1 style="font-size:${H1}px;margin-top:30px">Hasta 5 paradas<br>en un solo viaje.</h1>
      <div style="margin-top:${S?90:60}px;display:flex;flex-direction:column;gap:${S?26:14}px">${[['A','Origen · Palermo',CEL],['B','Parada · Caballito','#475569'],['C','Parada · Flores','#475569'],['D','Destino · La Plata',SOL]].map(([l,t,c])=>`<div style="display:flex;align-items:center;gap:24px"><span class="badge" style="background:${c};${c===SOL?'color:#3b2a00':''}">${l}</span><span style="font-size:${S?40:34}px;font-weight:600">${t}</span></div>`).join(`<div style="width:4px;height:${S?26:14}px;background:#D9E2F0;margin-left:30px"></div>`)}</div>
      <p style="font-size:${P}px;margin-top:${S?50:30}px;color:#475569">Levantás en un lado, dejás en otro y seguís. El precio ya lo contempla.</p>
      <div class="foot">${logo(LOGO_C, 440)}</div></div>`, S) },
    { name: '05-vehiculos', html: base(CEL, '#fff', `<div class="pad"><div class="eyebrow">8 tamaños</div><h1 style="font-size:${H1}px;margin-top:30px">De la Kangoo<br>al camión grande.</h1>
      <div style="margin-top:${S?70:40}px;display:grid;grid-template-columns:${S?'1fr 1fr':'repeat(4,1fr)'};gap:${S?30:14}px">${['kangoo','furgon_mediano','furgon_grande','pickup','camion_3_4','camion_mediano_cerrado','camion_grande_cerrado','camion_playo'].map(k=>`<div style="background:rgba(255,255,255,.12);border-radius:28px;padding:10px;display:grid;place-items:center"><div data-veh="${k}|${S?380:200}|${S?200:112}|#FFFFFF|#0B1F3A"></div></div>`).join('')}</div>
      <div class="foot">${logo(LOGO_W, 440)}<span class="pill" style="background:${SOL};color:#3b2a00">Elegís deslizando</span></div></div>`, S) },
    { name: '06-ayudantes', html: base(CLARO, NOCHE, `<div class="pad"><div class="eyebrow" style="color:${CEL}">Manos extra</div><h1 style="font-size:${H1}px;margin-top:30px">Ayudantes y tiempo<br>de carga, claros.</h1>
      <div style="margin-top:${S?80:50}px;display:grid;grid-template-columns:repeat(3,1fr);gap:20px">${[['🚚','Sin','Sin cargo'],['👷','1','+ $ 23.000'],['👷👷','2','+ $ 46.000']].map(([e,n,p],i)=>`<div class="card" style="text-align:center;padding:${S?34:20}px 20px;${i===1?`border:5px solid ${CEL}`:''}"><div style="font-size:${S?52:40}px">${e}</div><div style="font-size:${S?64:52}px;font-weight:800">${n}</div><div style="font-size:26px;color:#475569">${p}</div></div>`).join('')}</div>
      <div class="card" style="margin-top:20px;padding:${S?34:22}px 36px;display:flex;justify-content:space-between;align-items:center"><span style="font-size:${S?34:30}px;font-weight:600">Carga y descarga incluidas</span><span style="font-size:44px;font-weight:800;color:${CEL}">hasta 1 h</span></div>
      <p style="font-size:${P}px;margin-top:${S?40:24}px;color:#475569">Cada presupuesto dice cuánto tiempo está incluido. Sin sorpresas para vos ni para el conductor.</p>
      <div class="foot">${logo(LOGO_C, 440)}</div></div>`, S) },
    { name: '07-cuando', html: base('#fff', NOCHE, `<div class="pad"><div class="eyebrow" style="color:${CEL}">Vos elegís</div><h1 style="font-size:${H1}px;margin-top:30px">El día y la franja,<br>a tu medida.</h1>
      <div style="margin-top:${S?90:60}px;display:flex;gap:16px">${[['hoy','29'],['mañ.','30'],['sáb','31'],['dom','1'],['lun','2']].map(([d,n],i)=>`<div style="flex:1;border:4px solid ${i===1?CEL:'#D9E2F0'};border-radius:30px;padding:26px 0;text-align:center;${i===1?'background:#E8F1FF':''}"><div style="font-size:26px;color:#64748B">${d}</div><div style="font-size:70px;font-weight:800">${n}</div></div>`).join('')}</div>
      <div style="margin-top:20px;display:flex;gap:16px">${[['Mañana','8 a 12 h',true],['Tarde','13 a 18 h',false],['A coordinar','',false]].map(([t,s,on])=>`<div style="flex:1;border:4px solid ${on?CEL:'#D9E2F0'};border-radius:30px;padding:22px;text-align:center;${on?'background:#E8F1FF':''}"><div style="font-size:36px;font-weight:700">${t}</div><div style="font-size:24px;color:#64748B">${s}</div></div>`).join('')}</div>
      <div class="foot">${logo(LOGO_C, 440)}</div></div>`, S) },
    { name: '08-sin-pagos', html: base(NOCHE, '#fff', `<div class="pad"><div class="eyebrow" style="color:${SOL}">Tranqui</div><h1 style="font-size:${H1}px;margin-top:30px">No pagás nada<br>en la app.</h1>
      <p style="font-size:${P+4}px;margin-top:40px;max-width:860px;opacity:.9">Ves el precio aproximado, dejás tu nombre y tu WhatsApp, y te contactamos para confirmar. Después arreglás directo con el fletero.</p>
      <div style="margin-top:${S?90:60}px;display:flex;flex-direction:column;gap:22px">${['Precio aproximado al instante','Confirmación por WhatsApp','Sin tarjeta, sin registro'].map(t=>`<div style="display:flex;align-items:center;gap:22px;font-size:38px;font-weight:600"><span class="badge" style="background:#22C55E;width:56px;height:56px;font-size:30px">✓</span>${t}</div>`).join('')}</div>
      <div class="foot">${logo(LOGO_W, 440)}</div></div>`, S) },
    { name: '09-fleteros', html: base(CEL, '#fff', `<div class="pad"><div class="eyebrow">Para fleteros</div><h1 style="font-size:${H1}px;margin-top:30px">¿Tenés camioneta<br>o camión?</h1>
      <p style="font-size:${P+4}px;margin-top:40px;max-width:880px">Recibí pedidos con todo claro: paradas, pisos, qué se lleva, cuánto tiempo de carga está incluido y el precio ya cerrado.</p>
      <div data-veh="camion_mediano_cerrado|${S?860:560}|${S?480:314}|#FFFFFF|#0B1F3A" style="margin:${S?70:16}px auto 0"></div>
      <div class="foot">${logo(LOGO_W, 440)}<span class="pill" style="background:${SOL};color:#3b2a00">Escribinos</span></div></div>`, S) },
    { name: '10-mudanza', html: base(CLARO, NOCHE, `<div class="pad"><div class="eyebrow" style="color:${CEL}">Mudanza chica</div><h1 style="font-size:${H1}px;margin-top:30px">Cargá lo que llevás.<br>Te decimos dónde entra.</h1>
      <div style="margin-top:${S?80:50}px;display:grid;grid-template-columns:1fr 1fr;gap:16px">${[['🧊 Heladera','1'],['📦 Cajas','10'],['🛏️ Sommier','1'],['📺 TV','1']].map(([t,n])=>`<div class="card" style="display:flex;justify-content:space-between;align-items:center;padding:${S?24:16}px 28px;border:4px solid ${CEL}"><span style="font-size:${S?34:30}px;font-weight:600">${t}</span><span style="font-size:${S?40:36}px;font-weight:800;color:${CEL}">×${n}</span></div>`).join('')}</div>
      <div class="card" style="margin-top:16px;padding:${S?34:20}px 30px;background:#E8F1FF;box-shadow:none"><span style="font-size:${S?34:28}px;font-weight:600">Volumen estimado: 2,8 m³ · entra en Kangoo (3,0 m³)</span></div>
      <div class="foot">${logo(LOGO_C, 440)}</div></div>`, S) },
  ];
}

(async () => {
  const browser = await chromium.launch();
  for (const story of [false, true]) {
    const page = await browser.newPage({ viewport: { width: 1080, height: story ? 1920 : 1080 }, deviceScaleFactor: 1 });
    const dir = path.join(__dirname, story ? 'historias' : 'feed'); fs.mkdirSync(dir, { recursive: true });
    for (const p of posts(story)) {
      await page.setContent(p.html); await page.evaluate(() => document.fonts.ready); await page.waitForTimeout(150);
      await page.screenshot({ path: path.join(dir, `${p.name}.png`) });
    }
    await page.close();
  }
  await browser.close(); console.log('posts listos');
})();

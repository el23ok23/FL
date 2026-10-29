// Renderiza 5 videos 1080×1920 a 30 fps cuadro por cuadro (determinista) y los une con la música.
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path'); const { execSync } = require('child_process');
const VEH = fs.readFileSync(path.join(__dirname, '..', 'vehicles.js'), 'utf8');
const B = path.join(__dirname, '..', '..', 'brand', 'logo');
const LOGO_W = fs.readFileSync(path.join(B, 'fletaba-logo-horizontal-blanco.svg'), 'utf8');
const LOGO_C = fs.readFileSync(path.join(B, 'fletaba-logo-horizontal-color.svg'), 'utf8');
const ISO_W = fs.readFileSync(path.join(B, 'fletaba-isotipo-blanco.svg'), 'utf8');
const ISO_C = fs.readFileSync(path.join(B, 'fletaba-isotipo-color.svg'), 'utf8');
const sz = (svg, w, h) => svg.replace(/width="\d+" height="\d+"/, `width="${w}" height="${h}"`);
const CEL = '#1B7BE0', SOL = '#F6B40E', NOCHE = '#0B1F3A', CLARO = '#F4F8FE';
const FPS = 30, OUT = path.join(__dirname, 'mp4'); fs.mkdirSync(OUT, { recursive: true });

// Motor: cada elemento con data-in="inicio,duración,dirección" aparece animado; data-out="inicio,duración" desaparece.
const ENGINE = `
const ease=x=>1-Math.pow(1-x,3), back=x=>{const c=1.70158;return 1+(c+1)*Math.pow(x-1,3)+c*Math.pow(x-1,2)};
function prog(t,s,d){return Math.max(0,Math.min(1,(t-s)/d));}
window.__seek=function(t){
  document.querySelectorAll('[data-in]').forEach(el=>{const [s,d,dir]=el.dataset.in.split(',');let p=prog(t,+s,+d);let o=1;
    if(el.dataset.out){const [os,od]=el.dataset.out.split(',');o=1-prog(t,+os,+od);}
    const e=dir==='pop'?back(p):ease(p);const dist=90;
    let tr='';if(dir==='up')tr=\`translateY(\${(1-e)*dist}px)\`;if(dir==='down')tr=\`translateY(\${-(1-e)*dist}px)\`;if(dir==='left')tr=\`translateX(\${(1-e)*dist*1.6}px)\`;if(dir==='right')tr=\`translateX(\${-(1-e)*dist*1.6}px)\`;if(dir==='pop')tr=\`scale(\${0.2+0.8*e})\`;if(dir==='zoom')tr=\`scale(\${1.08-0.08*e})\`;
    el.style.opacity=Math.min(p*1.6,1)*o;el.style.transform=tr+(o<1?\` translateY(\${-(1-o)*40}px)\`:'');});
  if(window.__custom)window.__custom(t);
};`;
const base = (bg, fg, inner, custom) => `<html><head><meta charset="utf-8"><style>
body{margin:0;width:1080px;height:1920px;background:${bg};color:${fg};font-family:Inter,system-ui,sans-serif;overflow:hidden;position:relative}
*{box-sizing:border-box}[data-in]{opacity:0;will-change:transform,opacity}
.abs{position:absolute}.h1{font-weight:800;letter-spacing:-.035em;line-height:.98}.p{font-weight:500;line-height:1.3}
.eyebrow{font-size:30px;font-weight:700;letter-spacing:.14em;text-transform:uppercase}
.pill{display:inline-block;border-radius:999px;padding:18px 34px;font-weight:800;font-size:38px}
.card{background:#fff;color:${NOCHE};border-radius:40px;box-shadow:0 30px 80px rgba(11,31,58,.25)}
.badge{display:inline-grid;place-items:center;width:64px;height:64px;border-radius:50%;color:#fff;font-weight:800;font-size:30px}
.row{display:flex;align-items:center;gap:22px;font-size:40px;font-weight:600}
.check{display:inline-grid;place-items:center;width:60px;height:60px;border-radius:50%;background:#22C55E;color:#fff;font-size:34px;font-weight:800}
</style></head><body>${inner}
<script>${VEH}${ENGINE}${custom}</script></body></html>`;

// Camión que avanza: canvas redibujado por cuadro (ruedas giran) + ruta con guiones que corren.
const truckScript = (kind, w, h, body, detail) => `
const _tc=document.getElementById('truck');_tc.width=${w}*2;_tc.height=${h}*2;_tc.style.width='${w}px';_tc.style.height='${h}px';
const _rc=document.getElementById('road');_rc.width=1080*2;_rc.height=40*2;_rc.style.width='1080px';_rc.style.height='40px';
window.__drawTruck=function(rot){const c=_tc.getContext('2d');c.setTransform(1,0,0,1,0,0);c.clearRect(0,0,_tc.width,_tc.height);const s=Math.min(${w}/100,${h}/56);c.setTransform(2*s,0,0,2*s,2*(${w}-100*s)/2,2*(${h}-56*s)/2);window.__rot=rot;drawVehicle(c,'${kind}','${body}','${detail}');};
window.__drawRoad=function(shift,alpha){const c=_rc.getContext('2d');c.setTransform(2,0,0,2,0,0);c.clearRect(0,0,1080,40);c.strokeStyle='rgba(255,255,255,'+alpha+')';c.lineWidth=8;c.lineCap='round';const dw=70,gap=48;for(let x=-((shift)%(dw+gap));x<1080;x+=dw+gap){c.beginPath();c.moveTo(x,20);c.lineTo(x+dw,20);c.stroke();}};`;

const videos = [
  { name: '01-lanzamiento', dur: 12, bpm: 124, variant: 1, events: ['whoosh:0.2', 'ding:1.7', 'whoosh:8.6'],
    html: base(CEL, '#fff', `
      <div class="abs" style="left:-40px;top:-60px;width:460px;height:460px;border-radius:50%;background:${SOL}" data-in="0.1,1.2,pop"></div>
      <div class="abs" style="left:0;right:0;top:200px;text-align:center" data-in="1.5,0.7,pop">${sz(ISO_W, 220, 220)}</div>
      <div class="abs h1" style="left:0;right:0;top:450px;text-align:center;font-size:150px" data-in="1.9,0.6,up">FLETABA</div>
      <div class="abs p" style="left:0;right:0;top:620px;text-align:center;font-size:40px;opacity:.9" data-in="2.3,0.6,up">Fletes desde Capital Federal a todo el país</div>
      <div class="abs h1" style="left:90px;right:90px;top:790px;font-size:118px" data-in="3.2,0.7,up" data-out="8.4,0.4">Pedí tu flete<br>al toque.</div>
      <div class="abs" style="left:90px;right:90px;top:790px;display:flex;flex-direction:column;gap:30px">
        <div class="row" data-in="8.8,0.5,left"><span class="check">✓</span>Precio aproximado en segundos</div>
        <div class="row" data-in="9.3,0.5,left"><span class="check">✓</span>Hasta 5 paradas por viaje</div>
        <div class="row" data-in="9.8,0.5,left"><span class="check">✓</span>Sin pagos en la app</div>
      </div>
      <canvas id="truck" class="abs" style="left:160px;top:1120px"></canvas>
      <canvas id="road" class="abs" style="left:0;top:1545px"></canvas>
      <div class="abs" style="left:0;right:0;top:1660px;text-align:center" data-in="10.4,0.6,pop"><span class="pill" style="background:${SOL};color:#3b2a00">Descargala gratis</span></div>`,
      truckScript('furgon_mediano', 760, 420, '#FFFFFF', '#0B1F3A') + `
      window.__custom=function(t){const p=Math.max(0,Math.min(1,(t-0.2)/1.4));const e=1-Math.pow(1-p,3);const x=-900+(160+900)*e;_tc.style.left=x+'px';const bob=Math.sin(t*14)*(1-p)*6;_tc.style.top=(1120+bob)+'px';__drawTruck(t*720*(p<1?1:0.15)+(p<1?0:p*800));__drawRoad(t*520*(p<1?1:0.18)+ (p<1?0:t*60),0.35);};`) },

  { name: '02-como-funciona', dur: 16, bpm: 118, variant: 2, events: ['whoosh:0.3', 'whoosh:4.0', 'whoosh:7.7', 'whoosh:11.4', 'ding:14.6'],
    html: base(CLARO, NOCHE, `
      <div class="abs" style="left:90px;top:150px" data-in="0.1,0.5,down">${sz(LOGO_C, 500, 143)}</div>
      <div class="abs eyebrow" style="left:90px;top:360px;color:${CEL}" data-in="0.3,0.5,up">Cómo funciona</div>
      <div class="abs h1" style="left:90px;right:90px;top:410px;font-size:96px" data-in="0.5,0.6,up">4 pasos y listo.</div>
      <!-- Paso 1 -->
      <div class="abs card" style="left:90px;right:90px;top:620px;padding:40px" data-in="0.9,0.6,left" data-out="4.0,0.4">
        <div class="eyebrow" style="color:${CEL}">Paso 1 · Recorrido</div>
        <div style="margin-top:20px;display:flex;align-items:center;gap:18px;border:3px solid #D9E2F0;border-radius:26px;padding:22px"><span class="badge" style="background:${CEL}">A</span><span style="font-size:36px" id="t1"></span></div>
        <div style="margin:14px 0 0 56px;width:4px;height:22px;background:#D9E2F0"></div>
        <div style="margin-top:0;display:flex;align-items:center;gap:18px;border:3px solid #D9E2F0;border-radius:26px;padding:22px"><span class="badge" style="background:${SOL};color:#3b2a00">B</span><span style="font-size:36px" id="t2"></span></div>
        <canvas id="map" style="width:900px;height:420px;margin-top:24px;border-radius:28px;display:block"></canvas>
      </div>
      <!-- Paso 2 -->
      <div class="abs card" style="left:90px;right:90px;top:620px;padding:40px;overflow:hidden" data-in="4.5,0.6,left" data-out="7.7,0.4">
        <div class="eyebrow" style="color:${CEL}">Paso 2 · Tamaño</div>
        <div id="carousel" style="display:flex;gap:24px;margin-top:30px;width:3000px"></div>
        <div style="margin-top:26px;font-size:34px;color:#475569;text-align:center">Deslizás y elegís. El precio se actualiza solo.</div>
      </div>
      <!-- Paso 3 -->
      <div class="abs card" style="left:90px;right:90px;top:620px;padding:40px" data-in="8.2,0.6,left" data-out="11.4,0.4">
        <div class="eyebrow" style="color:${CEL}">Paso 3 · Detalles</div>
        <div style="margin-top:24px;display:grid;grid-template-columns:1fr 1fr;gap:16px">
          ${[['🧊 Heladera','1',8.7],['📦 Cajas','10',8.95],['🛏️ Sommier','1',9.2],['📺 TV','1',9.45]].map(([t,n,s])=>`<div data-in="${s},0.4,pop" style="display:flex;justify-content:space-between;align-items:center;border:4px solid ${CEL};border-radius:26px;padding:18px 26px"><span style="font-size:32px;font-weight:600">${t}</span><span style="font-size:36px;font-weight:800;color:${CEL}">×${n}</span></div>`).join('')}
        </div>
        <div data-in="9.9,0.5,up" style="margin-top:20px;background:#E8F1FF;border-radius:26px;padding:22px 26px;font-size:30px;font-weight:600">Volumen 2,8 m³ · entra en Kangoo ✓</div>
        <div data-in="10.4,0.5,up" style="margin-top:16px;display:flex;justify-content:space-between;align-items:center;border:3px solid #D9E2F0;border-radius:26px;padding:22px 26px"><span style="font-size:32px;font-weight:600">A · 3º piso por escalera</span><span style="background:#E8F1FF;color:${CEL};border-radius:14px;padding:8px 16px;font-weight:700;font-size:28px">⏱ 55 min</span></div>
      </div>
      <!-- Paso 4 -->
      <div class="abs card" style="left:90px;right:90px;top:620px;padding:40px;background:linear-gradient(135deg,${CEL},#0F5FB8);color:#fff" data-in="11.9,0.6,left">
        <div class="eyebrow" style="opacity:.85">Paso 4 · Precio</div>
        <div style="font-size:34px;margin-top:30px;opacity:.85">Total estimado</div>
        <div id="price" style="font-size:150px;font-weight:800;letter-spacing:-.04em;line-height:1">$ 0</div>
        <div style="font-size:34px;margin-top:10px">Kangoo · 10,0 km · 1 ayudante</div>
        <div style="margin-top:30px;display:flex;flex-direction:column;gap:14px;font-size:32px">
          ${[['Tarifa base','$ 35.300',13.0],['Recorrido · 10 km','$ 25.400',13.25],['1 ayudante','$ 28.300',13.5]].map(([a,b,s])=>`<div data-in="${s},0.4,left" style="display:flex;justify-content:space-between;border-top:2px solid rgba(255,255,255,.25);padding-top:12px"><span>${a}</span><b>${b}</b></div>`).join('')}
        </div>
        <div data-in="14.4,0.5,up" style="margin-top:30px;font-size:34px;font-weight:700">Confirmás por WhatsApp. No pagás nada en la app.</div>
      </div>
      <div class="abs h1" style="left:0;right:0;top:1700px;text-align:center;font-size:72px" data-in="14.8,0.6,pop">Así de simple.</div>`,
      `
      const T1="Av. Corrientes 1000, San Nicolás", T2="Calle 7 800, La Plata";
      const mc=document.getElementById('map');mc.width=1800;mc.height=840;
      const car=document.getElementById('carousel');['kangoo','furgon_mediano','furgon_grande','pickup','camion_3_4'].forEach(k=>{const d=document.createElement('div');d.style.cssText='flex:0 0 420px;border:4px solid #D9E2F0;border-radius:30px;padding:16px;text-align:center;background:#fff';d.appendChild(vehicleCanvas(k,380,200,'${CEL}','#0F172A',2));const n=document.createElement('div');n.style.cssText='font-size:30px;font-weight:700;margin-top:8px';n.textContent={kangoo:'Kangoo',furgon_mediano:'Furgón mediano',furgon_grande:'Furgón grande',pickup:'Pickup',camion_3_4:'Camión 3/4'}[k];d.appendChild(n);car.appendChild(d);});
      window.__custom=function(t){
        document.getElementById('t1').textContent=T1.slice(0,Math.floor(prog(t,1.3,1.0)*T1.length));
        document.getElementById('t2').textContent=T2.slice(0,Math.floor(prog(t,2.2,0.9)*T2.length));
        const c=mc.getContext('2d');c.setTransform(2,0,0,2,0,0);c.fillStyle='#E9F0FA';c.fillRect(0,0,900,420);c.fillStyle='rgba(15,23,42,.05)';for(let y=-10,r=0;y<420;y+=36,r++)for(let x=r%2?-20:-8;x<900;x+=36){c.beginPath();c.roundRect(x,y,30,30,3);c.fill();}
        const A=[160,110],Bp=[740,330];const lp=prog(t,3.0,0.9);c.lineCap='round';c.lineWidth=14;c.strokeStyle='rgba(27,123,224,.2)';c.beginPath();c.moveTo(A[0],A[1]);c.lineTo(A[0]+(Bp[0]-A[0])*lp,A[1]+(Bp[1]-A[1])*lp);c.stroke();c.lineWidth=6;c.strokeStyle='${CEL}';c.stroke();
        [[A,'A','${CEL}',1.3],[Bp,'B','${SOL}',2.2]].forEach(([p,l,col,s])=>{const sc=back(prog(t,s,0.5));if(sc<=0)return;c.save();c.translate(p[0],p[1]);c.scale(sc,sc);c.fillStyle='#fff';c.beginPath();c.arc(0,0,24,0,7);c.fill();c.fillStyle=col;c.beginPath();c.arc(0,0,20,0,7);c.fill();c.fillStyle=l==='B'?'#3b2a00':'#fff';c.font='bold 20px Inter,sans-serif';c.textAlign='center';c.textBaseline='middle';c.fillText(l,0,1);c.restore();});
        const sp=ease(prog(t,5.2,1.6));car.style.transform='translateX('+(-444*2*sp+444*0.55)+'px)';[...car.children].forEach((d,i)=>{const sel=i===Math.round(sp*2);d.style.transform=sel?'scale(1.05)':'scale(.94)';d.style.borderColor=sel?'${CEL}':'#D9E2F0';});
        const pp=ease(prog(t,12.2,1.2));document.getElementById('price').textContent='$ '+Math.round(89000*pp).toString().replace(/\\B(?=(\\d{3})+(?!\\d))/g,'.');
      };`) },

  { name: '03-todo-el-pais', dur: 12, bpm: 112, variant: 3, events: ['whoosh:0.2', 'ding:9.4'],
    html: base(NOCHE, '#fff', `
      <div class="abs eyebrow" style="left:90px;top:170px;color:${SOL}" data-in="0.2,0.5,up">Cobertura nacional</div>
      <div class="abs h1" style="left:90px;right:90px;top:230px;font-size:112px" data-in="0.4,0.7,up">Salís de Capital.<br>Llegás a donde sea.</div>
      <canvas id="net" class="abs" style="left:0;top:560px;width:1080px;height:900px"></canvas>
      ${[['Capital Federal',540,1000,1.2,CEL],['Rosario',300,620,2.4,SOL],['Córdoba',230,440,3.1,SOL],['Mendoza',110,760,3.8,SOL],['Mar del Plata',760,1280,4.5,SOL],['Bariloche',150,1300,5.2,SOL],['Salta',330,520,5.9,SOL],['Posadas',900,600,6.6,SOL]].map(([n,x,y,s,c])=>`<div class="abs" data-in="${s},0.5,pop" style="left:${x}px;top:${y}px;transform-origin:center"><span class="pill" style="background:${c};color:${c===SOL?'#3b2a00':'#fff'};font-size:${n==='Capital Federal'?36:30}px;padding:14px 26px;position:relative;left:-50%">${n}</span></div>`).join('')}
      <canvas id="truck" class="abs" style="left:-700px;top:1500px"></canvas>
      <canvas id="road" class="abs" style="left:0;top:1735px"></canvas>
      <div class="abs" style="left:0;right:0;top:1790px;text-align:center" data-in="9.3,0.6,pop">${sz(LOGO_W, 520, 149)}</div>`,
      truckScript('camion_3_4', 620, 345, '#FFFFFF', '#0B1F3A') + `
      const net=document.getElementById('net');net.width=2160;net.height=1800;
      const C=[540,1000-560];const P=[[300,620,2.4],[230,440,3.1],[110,760,3.8],[760,1280,4.5],[150,1300,5.2],[330,520,5.9],[900,600,6.6]];
      window.__custom=function(t){const c=net.getContext('2d');c.setTransform(2,0,0,2,0,0);c.clearRect(0,0,1080,900);c.lineCap='round';
        P.forEach(([x,y,s])=>{const p=ease(prog(t,s-0.5,0.6));if(p<=0)return;c.strokeStyle='rgba(246,180,14,.9)';c.lineWidth=6;c.setLineDash([18,14]);c.beginPath();c.moveTo(C[0],C[1]);c.lineTo(C[0]+(x-C[0])*p,C[1]+(y-560-C[1])*p);c.stroke();c.setLineDash([]);});
        const pulse=1+0.08*Math.sin(t*5);c.fillStyle='rgba(27,123,224,.25)';c.beginPath();c.arc(C[0],C[1],70*pulse,0,7);c.fill();
        const p=Math.min(1,Math.max(0,(t-7.2)/3.2));_tc.style.left=(-700+(1080+700+300)*p)+'px';__drawTruck(t*600);__drawRoad(t*420,0.25);};`) },

  { name: '04-precio-claro', dur: 12, bpm: 120, variant: 4, events: ['whoosh:0.2', 'ding:2.6', 'whoosh:7.2'],
    html: base(CLARO, NOCHE, `
      <div class="abs" style="left:90px;top:150px" data-in="0.1,0.5,down">${sz(LOGO_C, 500, 143)}</div>
      <div class="abs eyebrow" style="left:90px;top:360px;color:${CEL}" data-in="0.3,0.5,up">Sin vueltas</div>
      <div class="abs h1" style="left:90px;right:90px;top:410px;font-size:100px" data-in="0.5,0.6,up">Sabé cuánto sale<br>antes de pedirlo.</div>
      <div class="abs card" style="left:90px;right:90px;top:720px;padding:44px;background:linear-gradient(135deg,${CEL},#0F5FB8);color:#fff" data-in="1.2,0.6,pop">
        <canvas id="hero" style="width:300px;height:168px;display:block;margin:0 auto"></canvas>
        <div style="font-size:32px;opacity:.85;text-align:center">Total estimado</div>
        <div id="price" style="font-size:160px;font-weight:800;letter-spacing:-.04em;line-height:1;text-align:center">$ 0</div>
        <div style="font-size:34px;text-align:center">Furgón mediano · 12,4 km · 1 ayudante</div>
      </div>
      <div class="abs card" style="left:90px;right:90px;top:1270px;padding:10px 40px">
        ${[['Tarifa base · Furgón mediano','$ 49.500',4.2],['Recorrido · 12,4 km','$ 40.300',4.5],['1 ayudante · 12,4 km','$ 29.900',4.8],['Carga y descarga incluidas','hasta 1 h 10',5.1]].map(([a,b,s])=>`<div data-in="${s},0.45,left" style="display:flex;justify-content:space-between;align-items:center;padding:22px 0;border-top:2px solid #E2E8F0;font-size:33px"><span>${a}</span><b style="font-size:36px">${b}</b></div>`).join('')}
      </div>
      <div class="abs" style="left:90px;right:90px;top:1680px;display:flex;gap:18px;align-items:center" data-in="7.4,0.6,up"><span class="check">✓</span><span style="font-size:42px;font-weight:700">No pagás nada en la app</span></div>
      <div class="abs" style="left:90px;right:90px;top:1770px;display:flex;gap:18px;align-items:center" data-in="8.2,0.6,up"><span class="check" style="background:#25D366">💬</span><span style="font-size:42px;font-weight:700">Confirmás por WhatsApp</span></div>`,
      `const h=document.getElementById('hero');h.width=600;h.height=336;
      window.__custom=function(t){const c=h.getContext('2d');c.setTransform(1,0,0,1,0,0);c.clearRect(0,0,600,336);const s=Math.min(300/100,168/56);c.setTransform(2*s,0,0,2*s,2*(300-100*s)/2,2*(168-56*s)/2);window.__rot=t*300;drawVehicle(c,'furgon_mediano','#FFFFFF','rgba(7,25,74,.85)');
        const pp=ease(prog(t,1.8,1.4));document.getElementById('price').textContent='$ '+Math.round(119700*pp).toString().replace(/\\B(?=(\\d{3})+(?!\\d))/g,'.');};`) },

  { name: '05-fleteros', dur: 12, bpm: 126, variant: 5, events: ['whoosh:0.2', 'whoosh:5.0', 'ding:9.8'],
    html: base(CEL, '#fff', `
      <div class="abs" style="right:-120px;top:120px;width:420px;height:420px;border-radius:50%;background:${SOL}" data-in="0.1,1,pop"></div>
      <div class="abs eyebrow" style="left:90px;top:200px" data-in="0.2,0.5,up">Para fleteros</div>
      <div class="abs h1" style="left:90px;right:90px;top:260px;font-size:112px" data-in="0.4,0.7,up">¿Tenés camioneta<br>o camión?</div>
      <canvas id="truck" class="abs" style="left:-900px;top:620px"></canvas>
      <canvas id="road" class="abs" style="left:0;top:1070px"></canvas>
      <div class="abs" style="left:90px;right:90px;top:1160px;display:flex;flex-direction:column;gap:28px">
        ${[['Paradas, pisos y carga, por escrito',5.2],['Tiempo de carga incluido y espera tarifada',5.8],['Precio cerrado antes de salir',6.4],['El cliente ya te espera por WhatsApp',7.0]].map(([t,s])=>`<div class="row" data-in="${s},0.5,left" style="font-size:38px"><span class="check">✓</span>${t}</div>`).join('')}
      </div>
      <div class="abs" style="left:0;right:0;top:1600px;text-align:center" data-in="9.6,0.6,pop"><span class="pill" style="background:${SOL};color:#3b2a00">Sumate a Fletaba</span></div>
      <div class="abs" style="left:0;right:0;top:1740px;text-align:center" data-in="10.0,0.6,up">${sz(LOGO_W, 480, 137)}</div>`,
      truckScript('camion_mediano_cerrado', 820, 455, '#FFFFFF', '#0B1F3A') + `
      window.__custom=function(t){const p=ease(prog(t,0.6,1.6));_tc.style.left=(-900+(130+900)*p)+'px';__drawTruck(t*700*(p<1?1:0.1));__drawRoad(t*500*(p<1?1:0.15),0.35);};`) },
];

(async () => {
  const browser = await chromium.launch();
  for (const v of videos) {
    // Una pestaña nueva por video: así el script de un video no queda cargado en el siguiente.
    const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
    page.on('pageerror', e => console.error('error en', v.name, e.message));
    const frames = path.join(__dirname, 'frames', v.name); fs.rmSync(frames, { recursive: true, force: true }); fs.mkdirSync(frames, { recursive: true });
    await page.setContent(v.html); await page.evaluate(() => document.fonts.ready);
    const n = Math.round(v.dur * FPS);
    for (let i = 0; i < n; i++) {
      await page.evaluate(t => window.__seek(t), i / FPS);
      await page.screenshot({ path: path.join(frames, `f${String(i).padStart(4, '0')}.jpg`), type: 'jpeg', quality: 92 });
    }
    const wav = path.join(frames, 'audio.wav');
    execSync(`python3 "${path.join(__dirname, 'make_audio.py')}" "${wav}" ${v.dur} ${v.bpm} ${v.variant} ${v.events.join(' ')}`);
    const out = path.join(OUT, `fletaba-${v.name}.mp4`);
    execSync(`ffmpeg -y -loglevel error -framerate ${FPS} -i "${path.join(frames, 'f%04d.jpg')}" -i "${wav}" -c:v libx264 -preset medium -crf 19 -pix_fmt yuv420p -movflags +faststart -c:a aac -b:a 160k -shortest "${out}"`);
    fs.rmSync(frames, { recursive: true, force: true });
    await page.close();
    console.log('video listo:', out);
  }
  await browser.close();
})();

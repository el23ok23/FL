// Exporta cada SVG del logo a PNG (2048 px de ancho) y a PDF vectorial, para imprenta y textil.
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
(async () => {
  const dir = path.join(__dirname, 'logo'); const out = path.join(__dirname, 'png'); fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch(); const page = await browser.newPage({ deviceScaleFactor: 1 });
  for (const f of fs.readdirSync(dir).filter(n => n.endsWith('.svg'))) {
    const svg = fs.readFileSync(path.join(dir, f), 'utf8');
    const m = svg.match(/viewBox="0 0 (\d+) (\d+)"/); const w = +m[1], h = +m[2];
    const scale = 2048 / w; const W = Math.round(w * scale), H = Math.round(h * scale);
    const dark = f.includes('blanco');
    await page.setViewportSize({ width: W, height: H });
    await page.setContent(`<html><body style="margin:0;background:${dark ? '#0B1F3A' : 'transparent'}">${svg.replace(/width="\d+" height="\d+"/, `width="${W}" height="${H}"`)}</body></html>`);
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(out, f.replace('.svg', dark ? '-sobre-azul.png' : '.png')), omitBackground: !dark });
    if (f.includes('horizontal') || f.includes('isotipo-color') || f.includes('vertical')) {
      await page.pdf({ path: path.join(out, f.replace('.svg', '.pdf')), width: `${W}px`, height: `${H}px`, printBackground: true, pageRanges: '1' });
    }
  }
  await browser.close(); console.log('listo');
})();

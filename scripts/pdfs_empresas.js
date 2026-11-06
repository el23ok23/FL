// Genera, por empresa, la propuesta y el contrato en PDF a partir de sus páginas HTML.
// Uso: node scripts/pdfs_empresas.js [carpeta de salida]   (por defecto docs/empresas/<slug>/)
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
const emp = JSON.parse(fs.readFileSync(path.join(ROOT, 'ventas', 'empresas_salto.json'), 'utf8'));
const outDir = process.argv[2] || null;
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage();
  for (const e of emp) {
    const d = path.join(ROOT, 'docs', 'empresas', e.slug);
    const target = outDir ? path.join(outDir, e.slug) : d;
    fs.mkdirSync(target, { recursive: true });
    for (const [file, name] of [['propuesta.html', 'propuesta'], ['contrato.html', 'contrato']]) {
      await p.goto('file://' + path.join(d, file), { waitUntil: 'networkidle' }).catch(() => {});
      await p.addStyleTag({ content: '.noprint{display:none!important}a{color:inherit;text-decoration:none}' });
      await p.pdf({ path: path.join(target, `${name}.pdf`), format: 'A4', printBackground: true, margin: { top: '14mm', bottom: '16mm', left: '14mm', right: '14mm' } });
    }
    console.log('pdf ok', e.slug);
  }
  await b.close();
})();

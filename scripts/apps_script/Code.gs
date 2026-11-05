/**
 * Fletaba · panel del dueño (backend gratuito en Google Apps Script).
 *
 * Qué hace:
 *  - GET  → devuelve la planilla como CSV "clave,valor" (lo leen la app y la web al abrir).
 *  - POST {pin, accion:"verificar"}           → comprueba el PIN.
 *  - POST {pin, accion:"guardar", csv}        → reemplaza la planilla con los valores nuevos.
 *  - POST {pin, accion:"logo", imagen, tipo}  → guarda el logo en Drive y devuelve su enlace público.
 *
 * Cómo se instala (una sola vez por empresa, lo hacemos nosotros en la entrega): ver
 * ventas/CONFIGURAR_PANEL_DEL_DUENO.md
 */

// Cambiá el PIN acá antes de implementar. Solo números, de 4 a 8 cifras.
const PIN = '1234';

const HOJA = 'precios';

function doGet() {
  const hoja = hojaPrecios_();
  const filas = hoja.getDataRange().getValues();
  const csv = filas
    .filter(f => String(f[0]).trim() !== '')
    .map(f => String(f[0]).trim() + ',' + String(f[1]).trim())
    .join('\n');
  return ContentService.createTextOutput(csv || 'clave,valor').setMimeType(ContentService.MimeType.TEXT);
}

function doPost(e) {
  let body = {};
  try { body = JSON.parse(e.postData.contents || '{}'); } catch (err) { return json_({ ok: false, error: 'json' }); }
  if (String(body.pin || '') !== PIN) return json_({ ok: false, error: 'pin' });

  const accion = body.accion || 'guardar';
  if (accion === 'verificar') return json_({ ok: true });

  if (accion === 'guardar') {
    const filas = String(body.csv || '')
      .split(/\r?\n/)
      .map(l => l.trim())
      .filter(l => l && !/^clave\s*,/i.test(l))
      .map(l => { const i = l.indexOf(','); return [l.slice(0, i).trim(), l.slice(i + 1).trim()]; })
      .filter(f => f[0]);
    const lock = LockService.getScriptLock();
    lock.waitLock(10000);
    try {
      const hoja = hojaPrecios_();
      hoja.clearContents();
      hoja.getRange(1, 1, 1, 2).setValues([['clave', 'valor']]);
      if (filas.length) hoja.getRange(2, 1, filas.length, 2).setValues(filas);
      hoja.getRange(2, 2, Math.max(filas.length, 1), 1).setNumberFormat('@'); // valores como texto, sin redondeos raros
    } finally { lock.releaseLock(); }
    return json_({ ok: true, filas: filas.length });
  }

  if (accion === 'logo') {
    const bytes = Utilities.base64Decode(String(body.imagen || ''));
    if (!bytes.length || bytes.length > 3 * 1024 * 1024) return json_({ ok: false, error: 'imagen' });
    const blob = Utilities.newBlob(bytes, String(body.tipo || 'image/png'), 'logo-' + Date.now() + '.png');
    const carpeta = carpetaLogos_();
    const archivo = carpeta.createFile(blob);
    archivo.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    // Enlace directo a la imagen (sirve en la app y en la web).
    const url = 'https://drive.google.com/uc?export=view&id=' + archivo.getId();
    return json_({ ok: true, url: url });
  }

  return json_({ ok: false, error: 'accion' });
}

function hojaPrecios_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let hoja = ss.getSheetByName(HOJA);
  if (!hoja) {
    hoja = ss.getSheets()[0];
    hoja.setName(HOJA);
  }
  return hoja;
}

function carpetaLogos_() {
  const nombre = 'Logos ' + SpreadsheetApp.getActiveSpreadsheet().getName();
  const it = DriveApp.getFoldersByName(nombre);
  return it.hasNext() ? it.next() : DriveApp.createFolder(nombre);
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

/**
 * Fletaba · panel del dueño (backend gratuito en Google Apps Script).
 *
 * Qué hace:
 *  - GET  → devuelve la planilla como CSV "clave,valor" (lo leen la app y la web al abrir).
 *  - POST {pin, accion:"verificar"}           → comprueba el PIN.
 *  - POST {pin, accion:"guardar", csv}        → reemplaza la planilla con los valores nuevos.
 *  - POST {pin, accion:"logo", imagen, tipo}  → guarda el logo en Drive y devuelve su enlace público.
 *  - POST {accion:"pedido", ...}              → (sin PIN: lo mandan los clientes) anota el pedido en la hoja
 *                                               "pedidos", manda un correo al dueño y lo reparte por WhatsApp
 *                                               a cada número que tenga clave de CallMeBot.
 *
 * Cómo se instala (una sola vez por empresa, lo hacemos nosotros en la entrega): ver
 * ventas/CONFIGURAR_PANEL_DEL_DUENO.md
 */

// Cambiá el PIN acá antes de implementar. Solo números, de 4 a 8 cifras.
const PIN = '1234';

// WhatsApp a los que llega cada pedido solo, sin que el cliente abra nada (gratis con CallMeBot).
// Cada número se registra una vez en https://www.callmebot.com/blog/free-api-whatsapp-messages/ y
// recibe su clave. Formato: '549 + área + número': 'clave'. Los números que no estén acá igual
// aparecen como botón en la app para que el cliente les escriba.
const CALLMEBOT = {
  // '5492474123456': '123456',
};

// Correo al que llega cada pedido (vacío = el dueño de la planilla).
const CORREO = '';

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

  const accion = body.accion || 'guardar';
  if (accion === 'pedido') return pedido_(body);

  if (String(body.pin || '') !== PIN) return json_({ ok: false, error: 'pin' });

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

// ---- Pedidos ----------------------------------------------------------------------------------

function pedido_(b) {
  const mensaje = String(b.mensaje || '').slice(0, 4000);
  if (!mensaje) return json_({ ok: false, error: 'mensaje' });
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const hoja = hojaPedidos_();
    hoja.appendRow([
      new Date(), String(b.nombre || ''), "'" + String(b.whatsapp || ''), String(b.origen || ''), String(b.destino || ''),
      String(b.vehiculo || ''), Number(b.ayudantes || 0), String(b.que_lleva || ''), String(b.cuando || ''),
      Number(b.distancia_km || 0), Number(b.precio || 0), Number(b.neto_fletero || b.precio || 0), 'nuevo', mensaje,
    ]);
  } finally { lock.releaseLock(); }

  // Correo al dueño (gratis, hasta 100 por día).
  try {
    const para = CORREO || Session.getEffectiveUser().getEmail();
    if (para) MailApp.sendEmail({ to: para, subject: '🚚 Nuevo pedido · ' + (b.nombre || 'cliente') + ' · $ ' + Number(b.precio || 0).toLocaleString('es-AR'), body: mensaje + '\n\nResponder por WhatsApp: https://wa.me/' + String(b.whatsapp || '').replace(/\D/g, '') });
  } catch (err) { /* sin correo no se cae el pedido */ }

  // WhatsApp a cada número con clave de CallMeBot.
  const numeros = Object.keys(CALLMEBOT);
  const pedidos = numeros.map(n => ({ url: 'https://api.callmebot.com/whatsapp.php?phone=%2B' + n + '&text=' + encodeURIComponent(mensaje) + '&apikey=' + encodeURIComponent(CALLMEBOT[n]), muteHttpExceptions: true }));
  let enviados = 0;
  if (pedidos.length) {
    try { UrlFetchApp.fetchAll(pedidos).forEach(r => { if (r.getResponseCode() < 300) enviados++; }); } catch (err) { /* idem */ }
  }
  return json_({ ok: true, whatsapps: enviados });
}

function hojaPedidos_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let hoja = ss.getSheetByName('pedidos');
  if (!hoja) {
    hoja = ss.insertSheet('pedidos');
    hoja.appendRow(['Fecha', 'Cliente', 'WhatsApp', 'Origen', 'Destino', 'Vehículo', 'Ayudantes', 'Qué lleva', 'Cuándo', 'Km', 'Precio', 'Neto', 'Estado', 'Mensaje']);
    hoja.setFrozenRows(1);
    hoja.getRange('A:A').setNumberFormat('dd/mm/yyyy hh:mm');
  }
  return hoja;
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

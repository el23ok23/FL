/**
 * Fletaba · panel del dueño (backend gratuito en Google Apps Script).
 *
 * Qué hace:
 *  - GET  → devuelve la planilla como CSV "clave,valor" (lo leen la app y la web al abrir).
 *  - POST {pin, accion:"verificar"}           → comprueba el PIN.
 *  - POST {pin, accion:"guardar", csv}        → reemplaza la planilla con los valores nuevos.
 *  - POST {pin, accion:"logo", imagen, tipo}  → guarda el logo en Drive y devuelve su enlace público.
 *  - POST {accion:"pedido", ...}              → (sin PIN: lo mandan los clientes) anota el pedido en la hoja
 *                                               "pedidos" con un id, guarda las fotos en Drive, manda un correo
 *                                               al dueño y lo reparte por WhatsApp a cada número con CallMeBot.
 *  - GET  url?pedido=ID                       → estado de ese pedido (lo consulta la app del cliente).
 *  - POST {pin, accion:"pedidos"}             → últimos pedidos (para el panel del dueño).
 *  - POST {pin, accion:"estado", id, estado}  → cambia el estado de un pedido (nuevo, confirmado, en_camino, hecho, cancelado).
 *
 * Cómo se instala (una sola vez por empresa, lo hacemos nosotros en la entrega): ver
 * ventas/CONFIGURAR_PANEL_DEL_DUENO.md
 */

// PIN inicial del panel (el dueño puede cambiarlo desde la app; el nuevo queda guardado en el script).
const PIN = 'salto2026fletes';

// Correo donde el dueño recupera su PIN si lo olvida (también se cambia desde la app).
const CORREO_RECUPERACION = '';

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

function doGet(e) {
  const id = e && e.parameter && e.parameter.pedido;
  if (id) return estadoPedido_(String(id));
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
  if (accion === 'recuperar') return recuperar_();

  if (String(body.pin || '') !== pinActual_()) return json_({ ok: false, error: 'pin' });

  if (accion === 'verificar') return json_({ ok: true, correo: enmascarar_(correoRecuperacion_()) });

  if (accion === 'pedidos') return json_({ ok: true, pedidos: listarPedidos_(Number(body.limite || 60)) });

  if (accion === 'estado') {
    const estado = String(body.estado || '').trim().toLowerCase();
    if (['nuevo', 'confirmado', 'en_camino', 'hecho', 'cancelado'].indexOf(estado) < 0) return json_({ ok: false, error: 'estado' });
    return json_({ ok: cambiarEstado_(String(body.id || ''), estado) });
  }

  if (accion === 'cambiar_pin') {
    const nuevo = String(body.nuevo_pin || '').trim();
    if (nuevo.length < 4 || nuevo.length > 32) return json_({ ok: false, error: 'pin_invalido' });
    PropertiesService.getScriptProperties().setProperty('PIN', nuevo);
    return json_({ ok: true });
  }

  if (accion === 'correo') {
    const correo = String(body.correo || '').trim();
    if (correo && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(correo)) return json_({ ok: false, error: 'correo_invalido' });
    PropertiesService.getScriptProperties().setProperty('CORREO_RECUPERACION', correo);
    return json_({ ok: true, correo: enmascarar_(correo) });
  }

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

// ---- Seguridad --------------------------------------------------------------------------------

function pinActual_() {
  return PropertiesService.getScriptProperties().getProperty('PIN') || PIN;
}

function correoRecuperacion_() {
  const p = PropertiesService.getScriptProperties().getProperty('CORREO_RECUPERACION');
  return p === null ? CORREO_RECUPERACION : p;
}

/** "juan.perez@gmail.com" → "ju•••@gmail.com" */
function enmascarar_(correo) {
  if (!correo) return '';
  const i = correo.indexOf('@');
  if (i < 1) return '•••';
  return correo.slice(0, Math.min(2, i)) + '•••' + correo.slice(i);
}

/** Manda el PIN al correo de recuperación. Máximo 3 veces por hora. */
function recuperar_() {
  const correo = correoRecuperacion_();
  if (!correo) return json_({ ok: false, error: 'sin_correo' });
  const cache = CacheService.getScriptCache();
  const n = Number(cache.get('recuperaciones') || 0);
  if (n >= 3) return json_({ ok: false, error: 'limite' });
  cache.put('recuperaciones', String(n + 1), 3600);
  const nombre = SpreadsheetApp.getActiveSpreadsheet().getName();
  MailApp.sendEmail({
    to: correo,
    subject: 'Tu PIN del panel · ' + nombre,
    body: 'Hola.\n\nTu PIN para entrar al panel del dueño es:\n\n    ' + pinActual_() + '\n\nSi no pediste este correo, cambiá el PIN desde el panel (Seguridad → Cambiar PIN).\n',
  });
  return json_({ ok: true, correo: enmascarar_(correo) });
}

// ---- Pedidos ----------------------------------------------------------------------------------

function pedido_(b) {
  let mensaje = String(b.mensaje || '').slice(0, 4000);
  if (!mensaje) return json_({ ok: false, error: 'mensaje' });
  const id = Utilities.getUuid().slice(0, 8).toUpperCase();

  // Fotos de la carga (base64) → Drive, con enlace público.
  const fotos = [];
  (Array.isArray(b.imagenes) ? b.imagenes.slice(0, 3) : []).forEach((b64, i) => {
    try {
      const bytes = Utilities.base64Decode(String(b64));
      if (!bytes.length || bytes.length > 2 * 1024 * 1024) return;
      const f = carpeta_('Pedidos ' + SpreadsheetApp.getActiveSpreadsheet().getName()).createFile(Utilities.newBlob(bytes, 'image/jpeg', 'pedido-' + id + '-' + (i + 1) + '.jpg'));
      f.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      fotos.push('https://drive.google.com/uc?export=view&id=' + f.getId());
    } catch (err) { /* una foto que falla no frena el pedido */ }
  });
  if (fotos.length) mensaje += '\n\n📷 Fotos: ' + fotos.join(' ');
  mensaje += '\n🆔 Pedido ' + id;

  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const hoja = hojaPedidos_();
    hoja.appendRow([
      new Date(), String(b.nombre || ''), "'" + String(b.whatsapp || ''), String(b.origen || ''), String(b.destino || ''),
      String(b.vehiculo || ''), Number(b.ayudantes || 0), String(b.que_lleva || ''), String(b.cuando || ''),
      Number(b.distancia_km || 0), Number(b.precio || 0), Number(b.neto_fletero || b.precio || 0), 'nuevo', mensaje, id, fotos.join(' '),
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
  return json_({ ok: true, id: id, whatsapps: enviados, fotos: fotos.length });
}

function estadoPedido_(id) {
  const hoja = hojaPedidos_();
  const datos = hoja.getDataRange().getValues();
  for (let i = datos.length - 1; i >= 1; i--) {
    if (String(datos[i][14]) === id) return json_({ ok: true, id: id, estado: String(datos[i][12] || 'nuevo'), fecha: datos[i][0] });
  }
  return json_({ ok: false, error: 'no_existe' });
}

function listarPedidos_(limite) {
  const hoja = hojaPedidos_();
  const datos = hoja.getDataRange().getValues();
  const out = [];
  for (let i = datos.length - 1; i >= 1 && out.length < limite; i--) {
    const f = datos[i];
    out.push({ id: String(f[14] || ''), fecha: f[0], cliente: String(f[1] || ''), whatsapp: String(f[2] || '').replace(/^'/, ''), origen: String(f[3] || ''), destino: String(f[4] || ''), vehiculo: String(f[5] || ''), ayudantes: Number(f[6] || 0), cuando: String(f[8] || ''), km: Number(f[9] || 0), precio: Number(f[10] || 0), estado: String(f[12] || 'nuevo'), fotos: String(f[15] || '') });
  }
  return out;
}

function cambiarEstado_(id, estado) {
  if (!id) return false;
  const hoja = hojaPedidos_();
  const datos = hoja.getDataRange().getValues();
  for (let i = datos.length - 1; i >= 1; i--) {
    if (String(datos[i][14]) === id) { hoja.getRange(i + 1, 13).setValue(estado); return true; }
  }
  return false;
}

function hojaPedidos_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let hoja = ss.getSheetByName('pedidos');
  if (!hoja) {
    hoja = ss.insertSheet('pedidos');
    hoja.appendRow(['Fecha', 'Cliente', 'WhatsApp', 'Origen', 'Destino', 'Vehículo', 'Ayudantes', 'Qué lleva', 'Cuándo', 'Km', 'Precio', 'Neto', 'Estado', 'Mensaje', 'Id', 'Fotos']);
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
  return carpeta_('Logos ' + SpreadsheetApp.getActiveSpreadsheet().getName());
}

function carpeta_(nombre) {
  const it = DriveApp.getFoldersByName(nombre);
  return it.hasNext() ? it.next() : DriveApp.createFolder(nombre);
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

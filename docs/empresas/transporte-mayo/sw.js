// Service worker: deja la app abierta sin conexión (la página y lo que ya se vio). Las búsquedas de
// direcciones, el mapa y el envío del pedido siguen necesitando internet.
const CACHE = 'fletaba-9b46c30';
const CORE = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png'];
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(CORE)).then(() => self.skipWaiting())); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET') return;
  // Datos vivos (precios, pedidos, direcciones, mapas): siempre red; si falla, lo que haya en caché.
  const live = /script\.google|googleusercontent|nominatim|openstreetmap|googleapis\.com\/maps|tile\./.test(u.host + u.pathname);
  if (live) { e.respondWith(fetch(e.request).catch(() => caches.match(e.request))); return; }
  // Página y recursos: caché primero, y se actualiza en segundo plano.
  e.respondWith(caches.match(e.request).then(hit => {
    const net = fetch(e.request).then(r => { if (r && r.ok && (u.origin === location.origin || /cdnjs|fonts\.g/.test(u.host))) caches.open(CACHE).then(c => c.put(e.request, r.clone())); return r; }).catch(() => hit);
    return hit || net;
  }));
});

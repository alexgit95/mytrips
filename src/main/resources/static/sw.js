/**
 * MyTrips – Service Worker
 * Gère le cache des ressources statiques et des pages pour le mode hors ligne.
 */

const CACHE_NAME = 'mytrips-v1';
const STATIC_ASSETS = [
    '/css/style.css',
    '/js/offline.js',
    '/images/logo.png',
    '/images/logo192.png',
    '/images/logo512.png',
    '/manifest.json'
];

const DYNAMIC_CACHE = 'mytrips-dynamic-v1';

// Pages to cache for offline use
const CACHEABLE_PAGES = [
    '/trips'
];

// ─── Install ────────────────────────────────────────────────────────
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => {
            return cache.addAll(STATIC_ASSETS);
        }).then(() => self.skipWaiting())
    );
});

// ─── Activate ───────────────────────────────────────────────────────
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then(keys => {
            return Promise.all(
                keys.filter(key => key !== CACHE_NAME && key !== DYNAMIC_CACHE)
                    .map(key => caches.delete(key))
            );
        }).then(() => self.clients.claim())
    );
});

// ─── Fetch ──────────────────────────────────────────────────────────
self.addEventListener('fetch', (event) => {
    const { request } = event;
    const url = new URL(request.url);

    // Skip non-GET requests (form submissions are handled by offline.js)
    if (request.method !== 'GET') return;

    // Skip API, actuator, and auth requests (must toujours aller au réseau réel)
    if (url.pathname.startsWith('/api/') ||
        url.pathname.startsWith('/actuator/') ||
        url.pathname === '/login' ||
        url.pathname === '/logout') {
        return;
    }

    // Static assets → Cache First
    if (isStaticAsset(url.pathname)) {
        event.respondWith(cacheFirst(request));
        return;
    }

    // HTML pages → Network First (cache fallback)
    if (request.headers.get('accept')?.includes('text/html') || isCacheablePage(url.pathname)) {
        event.respondWith(networkFirst(request));
        return;
    }

    // Everything else → Network First
    event.respondWith(networkFirst(request));
});

// ─── Strategies ─────────────────────────────────────────────────────
async function cacheFirst(request) {
    const cached = await caches.match(request);
    if (cached) return cached;

    try {
        const response = await fetch(request);
        if (response.ok) {
            const cache = await caches.open(CACHE_NAME);
            cache.put(request, response.clone());
        }
        return response;
    } catch (err) {
        return new Response('', { status: 503, statusText: 'Offline' });
    }
}

async function networkFirst(request) {
    try {
        const response = await fetch(request);
        if (response.ok) {
            const cache = await caches.open(DYNAMIC_CACHE);
            cache.put(request, response.clone());
        }
        return response;
    } catch (err) {
        const cached = await caches.match(request);
        if (cached) return cached;

        // If it's a page request, try to serve the trips list as fallback
        if (request.headers.get('accept')?.includes('text/html')) {
            const fallback = await caches.match('/trips');
            if (fallback) return fallback;
        }

        return new Response('<html><body><h1>Hors ligne</h1><p>Cette page n\'est pas disponible hors ligne.</p></body></html>', {
            status: 503,
            headers: { 'Content-Type': 'text/html; charset=utf-8' }
        });
    }
}

// ─── Helpers ────────────────────────────────────────────────────────
function isStaticAsset(pathname) {
    return pathname.startsWith('/css/') ||
           pathname.startsWith('/js/') ||
           pathname.startsWith('/images/') ||
           pathname === '/manifest.json';
}

function isCacheablePage(pathname) {
    // Cache trips list and trip detail/planner/expenses pages
    return pathname === '/trips' ||
           /^\/trips\/\d+$/.test(pathname) ||
           /^\/trips\/\d+\/planner$/.test(pathname) ||
           /^\/trips\/\d+\/expenses\/new$/.test(pathname);
}

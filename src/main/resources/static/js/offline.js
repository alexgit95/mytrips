/**
 * MyTrips – Module Offline
 * Gère la détection réseau, le stockage IndexedDB des actions hors ligne,
 * la synchronisation au retour en ligne, et le bandeau de statut.
 */
(function () {
    'use strict';

    const DB_NAME = 'mytrips-offline';
    const DB_VERSION = 1;
    const STORE_NAME = 'pending-actions';
    const SYNC_URL = '/api/offline/sync';
    const CSRF_META = document.querySelector('meta[name="_csrf"]');
    const CSRF_HEADER_META = document.querySelector('meta[name="_csrf_header"]');

    let db = null;
    let serverReachable = true; // Mis à jour par isServerReachable(); updateBanner() s'en sert

    // ─── IndexedDB Setup ────────────────────────────────────────────
    function openDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(DB_NAME, DB_VERSION);
            request.onupgradeneeded = (e) => {
                const database = e.target.result;
                if (!database.objectStoreNames.contains(STORE_NAME)) {
                    database.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true });
                }
            };
            request.onsuccess = (e) => {
                db = e.target.result;
                resolve(db);
            };
            request.onerror = (e) => reject(e.target.error);
        });
    }

    function addPendingAction(action) {
        return new Promise((resolve, reject) => {
            const tx = db.transaction(STORE_NAME, 'readwrite');
            const store = tx.objectStore(STORE_NAME);
            store.add(action);
            tx.oncomplete = () => {
                resolve();
                updateBanner();
            };
            tx.onerror = (e) => reject(e.target.error);
        });
    }

    function getAllPendingActions() {
        return new Promise((resolve, reject) => {
            const tx = db.transaction(STORE_NAME, 'readonly');
            const store = tx.objectStore(STORE_NAME);
            const request = store.getAll();
            request.onsuccess = () => resolve(request.result);
            request.onerror = (e) => reject(e.target.error);
        });
    }

    function clearAllPendingActions() {
        return new Promise((resolve, reject) => {
            const tx = db.transaction(STORE_NAME, 'readwrite');
            const store = tx.objectStore(STORE_NAME);
            store.clear();
            tx.oncomplete = () => resolve();
            tx.onerror = (e) => reject(e.target.error);
        });
    }

    function countPendingActions() {
        return new Promise((resolve, reject) => {
            const tx = db.transaction(STORE_NAME, 'readonly');
            const store = tx.objectStore(STORE_NAME);
            const request = store.count();
            request.onsuccess = () => resolve(request.result);
            request.onerror = (e) => reject(e.target.error);
        });
    }

    // ─── Banner Management ──────────────────────────────────────────
    function getBanner() {
        return document.getElementById('offline-sync-banner');
    }

    function updateBanner() {
        countPendingActions().then(count => {
            const banner = getBanner();
            if (!banner) return;

            if (!serverReachable || !navigator.onLine) {
                // Hors ligne : toujours visible, avec ou sans éléments en attente
                banner.className = 'offline-sync-banner offline-sync-banner--pending';
                if (count > 0) {
                    banner.innerHTML = '<i class="bi bi-cloud-slash me-2"></i>Mode hors ligne — En attente de synchronisation <span class="badge bg-light text-dark ms-2">' + count + '</span>';
                } else {
                    banner.innerHTML = '<i class="bi bi-wifi-off me-2"></i>Mode hors ligne';
                }
                banner.style.display = 'flex';
            } else if (count > 0) {
                // En ligne mais actions en attente (sync pas encore lancée)
                banner.className = 'offline-sync-banner offline-sync-banner--pending';
                banner.innerHTML = '<i class="bi bi-cloud-slash me-2"></i>En attente de synchronisation <span class="badge bg-light text-dark ms-2">' + count + '</span>';
                banner.style.display = 'flex';
            } else {
                banner.style.display = 'none';
            }
        });
    }

    function showSyncingBanner(total) {
        const banner = getBanner();
        if (!banner) return;
        banner.className = 'offline-sync-banner offline-sync-banner--syncing';
        banner.innerHTML = '<i class="bi bi-arrow-repeat me-2 spin"></i>Synchronisation en cours… <span class="badge bg-light text-dark ms-2">' + total + '</span>';
        banner.style.display = 'flex';
    }

    function showSyncSuccessBanner() {
        const banner = getBanner();
        if (!banner) return;
        stopPeriodicCheck(); // Plus besoin de sonder, synchro terminée
        banner.className = 'offline-sync-banner offline-sync-banner--success';
        banner.innerHTML = '<i class="bi bi-check-circle me-2"></i>Synchronisation réussie';
        banner.style.display = 'flex';
        setTimeout(() => {
            banner.style.display = 'none';
            window.location.reload();
        }, 3000);
    }

    function showSyncErrorBanner(message) {
        const banner = getBanner();
        if (!banner) return;
        const detail = message ? ' — ' + escapeHtml(String(message)) : '';
        banner.className = 'offline-sync-banner offline-sync-banner--error';
        banner.innerHTML = '<i class="bi bi-exclamation-triangle me-2"></i>Erreur de synchronisation' + detail;
        banner.style.display = 'flex';
        setTimeout(() => {
            updateBanner();
        }, 10000);
    }

    // ─── Synchronization ────────────────────────────────────────────
    async function synchronize() {
        if (!navigator.onLine) return;

        const actions = await getAllPendingActions();
        if (actions.length === 0) return;

        showSyncingBanner(actions.length);

        try {
            const headers = { 'Content-Type': 'application/json' };
            if (CSRF_META && CSRF_HEADER_META) {
                headers[CSRF_HEADER_META.content] = CSRF_META.content;
            }

            const response = await fetch(SYNC_URL, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ actions: actions }),
                credentials: 'same-origin'
            });

            if (response.ok) {
                await clearAllPendingActions();
                showSyncSuccessBanner();
            } else {
                // Tenter d'extraire le message d'erreur serveur
                let errorMsg = 'HTTP ' + response.status;
                try {
                    const body = await response.json();
                    errorMsg = body.message || body.error || errorMsg;
                } catch { /* réponse non-JSON, on garde le code HTTP */ }
                console.error('[Offline] Sync erreur serveur:', errorMsg);
                showSyncErrorBanner(errorMsg);
            }
        } catch (err) {
            // TypeError = erreur réseau (hors ligne) → pas de message d'erreur, juste le bandeau "en attente"
            if (err instanceof TypeError) {
                console.warn('[Offline] Sync annulée (hors ligne):', err.message);
                updateBanner();
            } else {
                console.error('[Offline] Sync failed:', err);
                showSyncErrorBanner();
            }
        }
    }

    // ─── Offline Navigation Restrictions ────────────────────────────
    function applyOfflineRestrictions() {
        const isOffline = !navigator.onLine;

        // Hide/disable nav links when offline (except Voyages)
        const navLinks = document.querySelectorAll('#navMenu .nav-link');
        navLinks.forEach(link => {
            const href = link.getAttribute('href') || '';
            const isTripsLink = href === '/trips' || href.endsWith('/trips');
            if (!isTripsLink) {
                if (isOffline) {
                    link.classList.add('disabled', 'text-muted');
                    link.setAttribute('data-original-href', href);
                    link.addEventListener('click', preventNavigation);
                    link.style.pointerEvents = 'none';
                    link.style.opacity = '0.5';
                } else {
                    link.classList.remove('disabled', 'text-muted');
                    link.removeEventListener('click', preventNavigation);
                    link.style.pointerEvents = '';
                    link.style.opacity = '';
                }
            }
        });

        // On the trips list page, grey out non-ongoing trips entirely when offline
        const tripCards = document.querySelectorAll('.trip-card-col');
        tripCards.forEach(card => {
            const isOngoing = card.dataset.tripOngoing === 'true';
            const detailBtn  = card.querySelector('a.btn-sm.btn-primary, a.btn-primary.flex-fill');
            const editBtns   = card.querySelectorAll('a.btn-outline-secondary, button.btn-outline-danger, form');

            if (isOffline && !isOngoing) {
                // Griser la carte
                card.style.opacity = '0.55';
                card.style.filter  = 'grayscale(60%)';
                // Désactiver le bouton Détails
                if (detailBtn) {
                    detailBtn.classList.add('disabled');
                    detailBtn.style.pointerEvents = 'none';
                    detailBtn.setAttribute('data-offline-disabled', 'true');
                }
                // Masquer les boutons d'action (éditer, supprimer)
                editBtns.forEach(el => { el.style.display = 'none'; el.setAttribute('data-offline-hidden', 'true'); });
            } else {
                card.style.opacity = '';
                card.style.filter  = '';
                if (detailBtn && detailBtn.getAttribute('data-offline-disabled')) {
                    detailBtn.classList.remove('disabled');
                    detailBtn.style.pointerEvents = '';
                    detailBtn.removeAttribute('data-offline-disabled');
                }
                editBtns.forEach(el => {
                    if (el.getAttribute('data-offline-hidden')) {
                        el.style.display = '';
                        el.removeAttribute('data-offline-hidden');
                    }
                });
            }
        });

        // On trip detail/expense/planner pages, restrict access if trip is not ongoing
        const tripContext = document.getElementById('trip-context');
        if (tripContext && isOffline) {
            const tripOngoing = tripContext.dataset.tripOngoing === 'true';
            if (!tripOngoing) {
                window.location.href = '/trips';
                return;
            }
        }
    }

    function preventNavigation(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    // ─── Pre-caching ─────────────────────────────────────────────────
    function precacheTripPages() {
        if (!navigator.onLine || !('caches' in window)) return;

        // Detect current trip ID from URL
        const match = window.location.pathname.match(/^\/trips\/(\d+)/);
        if (!match) return;
        const tripId = match[1];

        const pages = [
            `/trips/${tripId}`,
            `/trips/${tripId}/expenses/new`,
            `/trips/${tripId}/planner`
        ];

        caches.open('mytrips-dynamic-v1').then(cache => {
            pages.forEach(url => {
                fetch(url, { credentials: 'same-origin' })
                    .then(response => {
                        if (response.ok) {
                            cache.put(url, response);
                            console.log('[Offline] Pre-cached:', url);
                        }
                    })
                    .catch(() => {});
            });
        });
    }

    // ─── Form Interception (Offline) ────────────────────────────────
    // On intercepte TOUJOURS les formulaires expense/planner.
    // On tente d'abord fetch → si échec réseau (quelle qu'en soit la cause) → save offline.
    // Cela couvre le cas `navigator.onLine = true` mais réseau réellement indisponible.
    function interceptForms() {
        if (document._offlineFormListenerAttached) return;
        document._offlineFormListenerAttached = true;

        document.addEventListener('submit', async function (e) {
            const form = e.target;
            const action = form.action || '';

            const isExpenseForm = action.includes('/expenses') && !action.includes('/delete');
            const isPlannerForm = action.includes('/planner/events')
                && !action.includes('/delete')
                && !action.includes('/comment');

            if (!isExpenseForm && !isPlannerForm) return;

            // Toujours prendre la main sur ces formulaires
            e.preventDefault();
            e.stopPropagation();

            const tripId = extractTripIdFromUrl(action);

            if (navigator.onLine) {
                try {
                    const formData = new FormData(form);
                    const response = await fetch(action, {
                        method: form.method || 'POST',
                        body: formData,
                        credentials: 'same-origin',
                        redirect: 'follow'
                    });

                    if (response.redirected) {
                        // Succès Spring POST-Redirect-GET → navigation vers la page cible
                        window.location.href = response.url;
                        return;
                    }

                    if (response.ok) {
                        // 200 sans redirection = erreur de validation Thymeleaf
                        // On recharge la page du formulaire proprement
                        showOfflineConfirmation('Veuillez vérifier les champs du formulaire.', 'warning');
                        setTimeout(() => window.location.reload(), 1500);
                        return;
                    }

                    // Erreur serveur (5xx) : bascule offline
                    console.warn('[Offline] Erreur serveur, bascule offline:', response.status);
                } catch (err) {
                    // Erreur réseau réelle (navigator.onLine peut encore être true !)
                    console.log('[Offline] Réseau indisponible, sauvegarde offline:', err.message);
                }
            }

            // Hors ligne ou échec réseau → sauvegarde dans IndexedDB
            if (isExpenseForm) {
                saveExpenseOffline(form, tripId);
            } else {
                savePlannerEventOffline(form, tripId);
            }
        }, true);
    }

    function saveExpenseOffline(form, tripId) {
        const action = {
            type: 'expense',
            tripId: tripId,
            timestamp: new Date().toISOString(),
            data: {
                label: form.querySelector('[name="label"]')?.value || '',
                amount: parseFloat(form.querySelector('[name="amount"]')?.value) || 0,
                date: form.querySelector('[name="date"]')?.value || '',
                categoryId: form.querySelector('[name="category"]:checked')?.value
                         || form.querySelector('[name="category"]')?.value || '',
                numberOfDays: parseInt(form.querySelector('[name="numberOfDays"]')?.value) || 1,
                isPaid: form.querySelector('[name="isPaid"]')?.checked || false
            }
        };
        addPendingAction(action).then(() => {
            showOfflineConfirmation('Dépense enregistrée hors ligne');
            form.reset();
            injectPendingExpenses();
            updateBanner();
            startPeriodicCheck();
        });
    }

    function savePlannerEventOffline(form, tripId) {
        const action = {
            type: 'planner_event',
            tripId: tripId,
            timestamp: new Date().toISOString(),
            data: {
                name: form.querySelector('[name="name"]')?.value || '',
                eventDateTime: form.querySelector('[name="eventDateTime"]')?.value || '',
                location: form.querySelector('[name="location"]')?.value || '',
                latitude: parseFloat(form.querySelector('[name="latitude"]')?.value) || null,
                longitude: parseFloat(form.querySelector('[name="longitude"]')?.value) || null,
                comment: form.querySelector('[name="comment"]')?.value || ''
            }
        };
        addPendingAction(action).then(() => {
            showOfflineConfirmation('Événement enregistré hors ligne');
            form.reset();
            injectPendingPlannerEvents();
            updateBanner();
            startPeriodicCheck();
        });
    }

    function extractTripIdFromUrl(url) {
        const match = url.match(/\/trips\/(\d+)/);
        return match ? parseInt(match[1]) : null;
    }

    function showOfflineConfirmation(message, type) {
        const alertDiv = document.createElement('div');
        const cls = type === 'warning' ? 'alert-warning' : 'alert-info';
        alertDiv.className = 'alert ' + cls + ' alert-dismissible fade show position-fixed';
        alertDiv.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px;';
        alertDiv.innerHTML = '<i class="bi bi-cloud-slash me-2"></i>' + message +
            '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';
        document.body.appendChild(alertDiv);
        setTimeout(() => alertDiv.remove(), 4000);
    }

    // ─── Network Event Listeners ────────────────────────────────────
    function onOnline() {
        // Événement navigateur 'online' : arrêt du polling, sonde immédiate
        // Ne pas passer serverReachable à true tout de suite : c'est periodicCheck
        // qui confirme la joignabilité réelle.
        stopPeriodicCheck();
        periodicCheck();
        applyOfflineRestrictions();
    }

    function onOffline() {
        serverReachable = false; // Certain d'être hors ligne
        applyOfflineRestrictions();
        updateBanner();
        // Démarrer le polling si pas encore actif
        startPeriodicCheck();
    }

    // ─── Service Worker Registration ────────────────────────────────
    function registerServiceWorker() {
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('/sw.js')
                .then(reg => {
                    console.log('[Offline] Service Worker registered:', reg.scope);
                })
                .catch(err => {
                    console.error('[Offline] Service Worker registration failed:', err);
                });
        }
    }

    // ─── Connectivity Probe ──────────────────────────────────────────
    // navigator.onLine n'est pas fiable. On teste la connectivité réelle
    // en tentant un fetch léger vers le serveur.
    async function isServerReachable() {
        try {
            const response = await fetch('/actuator/health', {
                method: 'GET',
                cache: 'no-store',
                credentials: 'same-origin',
                signal: AbortSignal.timeout(3000)
            });
            return response.ok;
        } catch {
            return false;
        }
    }

    // ─── Periodic Sync Check ─────────────────────────────────────────
    // Toutes les 5 secondes SI la page a été chargée hors ligne :
    // sonde le serveur, met à jour le bandeau, lance la synchro dès que possible.
    const SYNC_INTERVAL_MS = 5000;
    let periodicCheckInterval = null;

    async function periodicCheck() {
        const reachable = await isServerReachable();
        serverReachable = reachable;

        if (reachable) {
            const count = await countPendingActions();
            if (count > 0) {
                console.log('[Offline] Serveur joignable, lancement de la synchro…');
                synchronize();
            } else {
                // Serveur joignable et rien en attente → bandeau masqué
                updateBanner();
            }
        } else {
            // Serveur non joignable → bandeau "Mode hors ligne"
            updateBanner();
        }
    }

    function startPeriodicCheck() {
        if (periodicCheckInterval) return; // déjà actif
        console.log('[Offline] Démarrage du polling toutes les', SYNC_INTERVAL_MS / 1000, 's');
        periodicCheckInterval = setInterval(periodicCheck, SYNC_INTERVAL_MS);
    }

    function stopPeriodicCheck() {
        if (periodicCheckInterval) {
            clearInterval(periodicCheckInterval);
            periodicCheckInterval = null;
            console.log('[Offline] Polling arrêté (connexion rétablie)');
        }
    }

    // ─── Pending Expenses Display ────────────────────────────────────
    // Injecte les dépenses en attente de synchro dans le tableau de la page detail.
    async function injectPendingExpenses() {
        const tbody = document.getElementById('expenses-tbody');
        if (!tbody) return; // pas sur la page detail

        const tripContext = document.getElementById('trip-context');
        if (!tripContext) return;
        const currentTripId = parseInt(tripContext.dataset.tripId);

        const actions = await getAllPendingActions();
        const pendingExpenses = actions.filter(a => a.type === 'expense' && a.tripId === currentTripId);
        if (pendingExpenses.length === 0) return;

        // Supprimer les lignes offline déjà injectées (pour éviter les doublons)
        tbody.querySelectorAll('tr.offline-pending-row').forEach(r => r.remove());

        pendingExpenses.forEach(action => {
            const d = action.data;
            const tr = document.createElement('tr');
            tr.className = 'offline-pending-row table-warning';
            tr.title = 'En attente de synchronisation';
            tr.innerHTML = `
                <td class="text-nowrap">${d.date || '—'}</td>
                <td>${escapeHtml(d.label || '')} <span class="badge bg-warning text-dark ms-1" style="font-size:.7em"><i class="bi bi-cloud-slash"></i> hors ligne</span></td>
                <td><span class="badge" style="background-color:#e9ecef;color:#212529">—</span></td>
                <td class="text-end fw-semibold">${d.amount ? d.amount.toFixed(2) + ' €' : '—'}</td>
            `;
            tbody.appendChild(tr);
        });

        // Mettre à jour le badge compteur
        const badge = document.getElementById('expenses-count-badge');
        if (badge) {
            const serverCount = tbody.querySelectorAll('tr:not(.offline-pending-row)').length;
            const total = serverCount + pendingExpenses.length;
            badge.textContent = total + ' dépense(s)';
        }
    }

    function escapeHtml(str) {
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    async function injectPendingPlannerEvents() {
        const container = document.getElementById('planner-timeline-body');
        if (!container) return; // pas sur la page planner

        const tripContext = document.getElementById('trip-context');
        if (!tripContext) return;
        const currentTripId = parseInt(tripContext.dataset.tripId);

        // Supprimer le groupe offline déjà injecté (éviter les doublons)
        const existing = container.querySelector('.offline-pending-day-group');
        if (existing) existing.remove();

        const actions = await getAllPendingActions();
        const pendingEvents = actions.filter(a => a.type === 'planner_event' && a.tripId === currentTripId);
        if (pendingEvents.length === 0) return;

        // Construire un groupe "day" offline
        const group = document.createElement('div');
        group.className = 'timeline-day-group offline-pending-day-group';
        group.style.cssText = 'border-left: 3px solid #fd7e14; padding-left: .75rem; margin-top: 1rem;';

        const label = document.createElement('div');
        label.className = 'timeline-day-label';
        label.innerHTML = '<i class="bi bi-cloud-slash me-2 text-warning"></i>'
            + '<span class="text-warning fw-semibold">En attente de synchronisation</span>'
            + ' <span class="badge bg-warning text-dark ms-2">' + pendingEvents.length + '</span>';
        group.appendChild(label);

        pendingEvents.forEach(action => {
            const d = action.data;
            // Formater heure depuis eventDateTime (YYYY-MM-DDTHH:mm)
            const timePart = d.eventDateTime ? d.eventDateTime.split('T')[1] || '—' : '—';
            const datePart = d.eventDateTime ? d.eventDateTime.split('T')[0] || '' : '';

            const wrapper = document.createElement('div');
            wrapper.className = 'mb-2 offline-pending-row';
            wrapper.title = 'En attente de synchronisation';
            wrapper.innerHTML = `
                <div class="card event-card" style="border-left: 3px solid #fd7e14; opacity: .85;">
                    <div class="card-body py-2 px-3">
                        <div class="d-flex align-items-start gap-3">
                            <span class="event-time mt-1">${escapeHtml(timePart)}</span>
                            <div class="flex-grow-1 min-w-0">
                                <div class="event-name">
                                    ${escapeHtml(d.name || '—')}
                                    <span class="badge bg-warning text-dark ms-1" style="font-size:.7em">
                                        <i class="bi bi-cloud-slash"></i> hors ligne
                                    </span>
                                </div>
                                ${d.location ? `<div class="event-location"><i class="bi bi-geo-alt me-1"></i>${escapeHtml(d.location)}</div>` : ''}
                                ${(!d.location && d.latitude) ? `<div class="event-location"><i class="bi bi-crosshair me-1"></i>${d.latitude}, ${d.longitude}</div>` : ''}
                                ${d.comment ? `<div class="event-location mt-1"><i class="bi bi-chat-text me-1"></i>${escapeHtml(d.comment)}</div>` : ''}
                                <div class="event-location text-muted" style="font-size:.75em">${escapeHtml(datePart)}</div>
                            </div>
                        </div>
                    </div>
                </div>`;
            group.appendChild(wrapper);
        });

        container.appendChild(group);
    }

    // ─── Initialization ─────────────────────────────────────────────
    async function init() {
        registerServiceWorker();

        await openDB();

        window.addEventListener('online', onOnline);
        window.addEventListener('offline', onOffline);

        // Apply initial state
        applyOfflineRestrictions();
        interceptForms();
        updateBanner();

        // Injecter les actions en attente dans les pages concernées
        await injectPendingExpenses();
        await injectPendingPlannerEvents();

        // Détecter si le serveur est joignable (test réseau réel, pas navigator.onLine)
        const reachableAtLoad = await isServerReachable();
        serverReachable = reachableAtLoad;
        updateBanner(); // Re-calcul après sonde initiale
        const pendingCount = await countPendingActions();

        if (reachableAtLoad) {
            // En ligne : pré-cache + synchro immédiate si actions en attente
            precacheTripPages();
            if (pendingCount > 0) {
                synchronize();
            }
        } else {
            // Hors ligne au chargement : démarrer le polling
            console.log('[Offline] Page chargée hors ligne, démarrage du polling…');
            startPeriodicCheck();
        }

        // Démarrer le polling aussi si des actions sont en attente (pour couvrir
        // le cas où on revient en ligne sans recharger la page)
        if (pendingCount > 0 && !periodicCheckInterval) {
            console.log('[Offline] Actions en attente, démarrage du polling…');
            startPeriodicCheck();
        }
    }

    // ─── Public API (for "Ici et maintenant" integration) ───────────
    window.MyTripsOffline = {
        addPendingAction: addPendingAction,
        isOffline: () => !navigator.onLine,
        getPendingCount: countPendingActions,
        startPeriodicCheck: startPeriodicCheck,
        injectPendingPlannerEvents: injectPendingPlannerEvents
    };

    // Start
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

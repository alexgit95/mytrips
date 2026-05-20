# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et le versionnage suit [Semantic Versioning](https://semver.org/lang/fr/).

---

## [2.9.0] - 2026-05-19

### Nouveautés

#### Mode hors ligne — utilisation sans connexion internet

- **Service Worker** (`sw.js`) pour la mise en cache des ressources statiques et des pages visitées
- **Détection automatique** de la perte/retour de connexion via les événements navigateur `online`/`offline`
- **Stockage local IndexedDB** des actions effectuées hors ligne (ajout de dépenses, événements planner)
- **Restrictions de navigation hors ligne** :
  - Seul l'onglet « Voyages » reste accessible
  - Le planner et les dépenses ne sont accessibles que pour les **voyages en cours**
  - Ajout de dépenses et d'événements planner (formulaire ou « Ici et maintenant ») uniquement pour les voyages en cours
  - Le bouton « Ici et maintenant » sauvegarde l'événement en IndexedDB si la requête réseau échoue, et affiche un toast de confirmation hors ligne
- **Synchronisation automatique** au retour en ligne via `POST /api/offline/sync`
- **Politique de conflit** : en cas de doublon détecté, la valeur du serveur a priorité (l'entrée hors ligne est ignorée)
- **Bandeau flottant de statut** en bas de l'écran :
  - Gris : « Mode hors ligne » (affiché en permanence tant que la connexion est absente)
  - Gris : « Mode hors ligne — En attente de synchronisation » + compteur (si actions en attente)
  - Bleu : « Synchronisation en cours… » avec compteur
  - Vert : « Synchronisation réussie » (disparaît après 3 secondes)
  - Rouge : « Erreur de synchronisation »
- **Nouveau endpoint REST** : `POST /api/offline/sync` (ADMIN, REPORTER) — réception batch des actions hors ligne
- **Nouveau service** : `OfflineSyncService` — traitement et dédoublonnage des actions hors ligne
- **Tests unitaires** : `OfflineSyncServiceTest` (6 tests) et `OfflineSyncControllerTest` (6 tests)
- **PWA amélioré** : manifest.json enrichi avec référence au service worker

#### Mode hors ligne — corrections et améliorations

- **Bandeau persistant** : le bandeau reste affiché sur toutes les pages tant que le serveur est injoignable ; la détection s'appuie sur une variable `serverReachable` mise à jour à chaque sonde réseau, et non sur `navigator.onLine` qui peut être trompeur
- **Sonde réseau fiable** : le Service Worker n'intercepte plus les requêtes `/actuator/` (ni `/api/`), empêchant que `/actuator/health` retourne une réponse en cache et masque à tort l'état hors ligne
- **Voyages inaccessibles grisés** : dans la liste des voyages, les voyages non-en-cours sont grisés (opacité + désaturation), leur bouton « Détails » est désactivé et leurs boutons d'action (modifier, supprimer) sont masqués
- **Événements planner en attente visibles** : les événements stockés hors ligne apparaissent dans un groupe dédié « En attente de synchronisation » en bas de la timeline, avec bordure orange et badge « hors ligne »
- **Dépenses en attente visibles** : les dépenses stockées hors ligne apparaissent dans le tableau avec un badge orange « hors ligne »
- **Pas d'erreur de synchronisation lors d'une tentative hors ligne** : si la synchronisation échoue avec une erreur réseau (`TypeError`), le bandeau reste en mode « en attente » sans afficher le rouge
- **Message d'erreur détaillé** : en cas d'erreur serveur (HTTP 4xx/5xx), le bandeau rouge affiche le message extrait de la réponse JSON (`message` ou `error`) ou le code HTTP, pendant **10 secondes**

---

## [2.8.0] - 2026-05-12

### Nouveautés

#### Logements — sélecteur de position sur carte OpenStreetMap

- Ajout d'un **bouton 🌍** à côté du champ « Adresse » dans le formulaire de création et d'édition d'un logement
- Un clic ouvre une **modale plein-écran (responsive)** avec une carte interactive **OpenStreetMap via Leaflet**, identique à celle du planner
- L'utilisateur peut :
  - **Cliquer sur la carte** pour placer un marqueur et capturer latitude / longitude
  - **Déplacer le marqueur** par glisser-déposer pour affiner la position
  - Utiliser le bouton **« Ma position »** pour centrer la carte sur sa géolocalisation GPS actuelle
  - **Confirmer** la sélection pour renseigner automatiquement les coordonnées dans le formulaire
- Les coordonnées s'affichent dans une **pastille verte** sous le champ d'adresse, avec un bouton pour les effacer
- La carte se **réouvre centrée** sur les coordonnées déjà enregistrées lors d'une modification
- Les champs de saisie manuelle latitude/longitude sont remplacés par ce sélecteur cartographique

#### Road Trip — itinéraire aller-retour depuis le logement

- Quand un **jour précis** est sélectionné dans la timeline et qu'un **logement géocodé** est actif ce jour-là, l'itinéraire intègre automatiquement le logement comme **point de départ et de retour**
- La route calculée est : **logement → étape 1 → … → étape N → logement**
- La distance affichée est la **distance aller-retour totale depuis le logement**
- La **liste des étapes** affiche le logement en tête et en queue avec un badge violet **🛏 Logement**
- Fonctionne avec un seul waypoint planner (logement → waypoint → logement)
- En vue **« Tout le voyage »**, le comportement est inchangé (pas de bouclage sur logement)
- La note de distance précise « Aller-retour depuis le logement · distance routière via OSRM »

---

## [2.7.0] - 2026-05-12

### Nouveautés

#### Logements par voyage

- Ajout d'une entité **`Accommodation`** (logement) liée à chaque voyage (relation OneToMany)
- Chaque logement contient : **nom** (obligatoire), **adresse**, **date d'arrivée**, **date de départ**, **coordonnées GPS** (latitude/longitude) et **commentaire**
- Nouvelle page **`/trips/{id}/accommodations`** (bouton 🏠 sur la page de détail du voyage) permettant de :
  - Lister tous les logements du voyage ordonnés par date d'arrivée
  - Ajouter un logement (ADMIN)
  - Modifier un logement (ADMIN)
  - Supprimer un logement (ADMIN)

#### Planner — affichage du logement actif par jour

- Dans la **timeline du planner**, chaque journée affiche désormais un **badge 🏠 violet** indiquant le logement actif ce jour-là (règle : `arrivalDate ≤ jour < departureDate`)
- Si une adresse est renseignée, elle s'affiche à côté du nom du logement dans le badge
- Compatibilité null-safe : si aucun logement n'est défini, la timeline s'affiche normalement

#### Road Trip — marqueurs maison sur la carte

- Les logements possédant des **coordonnées GPS** (lat/lng) sont affichés sur la carte Road Trip sous forme de **marqueurs violets 🏠**
- Chaque popup de marqueur maison affiche : nom, adresse (si disponible), plage de dates (arrivée → départ), commentaire
- Les marqueurs maison sont **filtrés par période** lors du filtrage par jour (seuls les logements dont la plage chevauche les dates visibles sont affichés)
- Si aucun waypoint n'est disponible mais que des logements géocodés existent, la carte se centre sur ces logements

#### Export / Import JSON

- L'export JSON inclut désormais un tableau `accommodations` avec tous les logements de tous les voyages
- L'import JSON restaure les logements et les associe aux voyages correspondants
- Compatibilité ascendante préservée : les anciens fichiers JSON sans `accommodations` sont importés sans erreur

---

## [2.6.0] - 2026-05-12

### Nouveautés

#### Planner — sélecteur de position sur carte OpenStreetMap

- Ajout d'un **bouton 🌍** à côté du champ « Lieu / Adresse » dans le formulaire de création et la modale de modification d'un événement
- Un clic ouvre une **modale plein-écran (responsive)** avec une carte interactive **OpenStreetMap via Leaflet**
- L'utilisateur peut :
  - **Cliquer sur la carte** pour placer un marqueur et capturer latitude / longitude
  - **Déplacer le marqueur** par glisser-déposer pour affiner la position
  - Utiliser le bouton **« Ma position »** pour centrer la carte sur sa géolocalisation GPS actuelle
  - **Confirmer** la sélection pour renseigner les coordonnées dans le formulaire
- Après chaque sélection sur la carte, un **reverse geocoding automatique** (via l'endpoint `/geocode` existant) tente de remplir le champ adresse avec le nom du lieu correspondant
- Les coordonnées s'affichent dans une **pastille verte** sous le champ d'adresse, avec un bouton pour les effacer
- Les coordonnées peuvent être saisies **sans adresse texte**, pour les lieux sans adresse postale
- La carte se **réouvre centrée** sur les coordonnées déjà enregistrées lors d'une modification
- Dans la **timeline**, un événement sans adresse mais avec coordonnées affiche les coordonnées GPS et un lien Google Maps direct

#### Road Trip — étapes sans adresse désormais visibles

- Les événements planner **sans adresse texte** mais possédant des coordonnées GPS sont maintenant inclus dans la vue Road Trip
- Dans la liste des étapes et les popups de la carte, les coordonnées (`lat, lng`) s'affichent en fallback quand aucune adresse n'est disponible
- L'**heure** de l'événement est désormais affichée dans la liste des étapes (à côté de la date)
- Le **commentaire** de l'événement est affiché en italique sous la date dans la liste des étapes (si renseigné)
- Message d'alerte mis à jour pour mentionner le sélecteur de carte comme alternative au géocodage

### Améliorations

#### Planner — géocodage automatique à la création/modification

- À la création ou modification d'un événement, si une adresse est renseignée sans coordonnées, le service tente automatiquement un **géocodage forward** via Nominatim (si `GEOCODING_ENABLED=true`) pour renseigner latitude/longitude

#### Planner — propagation des coordonnées lors de la modification

- Le service de mise à jour (`PlannerEventService.update`) propage désormais les coordonnées latitude/longitude depuis le formulaire au lieu de les effacer automatiquement lors d'un changement d'adresse
- La création d'un événement conserve les coordonnées même si aucune adresse texte n'est renseignée (suppression de la réinitialisation automatique lat/lon)

---

## [2.5.2] - 2026-05-05

### Améliorations

#### Road Trip — règle de filtrage du pays d'origine affinée

- **Nouvelle règle de filtrage** pour les voyages à cheval entre le pays d'origine et un pays étranger :
  - Si le nombre d'étapes dans le pays d'origine est **≤ 4** → ces points sont exclus (simples escales de départ / retour, comportement précédent)
  - Si le nombre d'étapes dans le pays d'origine est **> 4** → tous les points sont affichés (voyage multi-pays significatif)
- Le badge dans l'en-tête de la page Road Trip indique désormais :
  - 🔵 « Points du pays d'origine exclus (N ≤ 4) » quand le filtrage s'applique
  - 🟢 « Voyage multi-pays — tous les points affichés » quand le pays d'origine est présent en nombre suffisant
- Exposition des attributs `homePointsFiltered` et `homeCountryPointCount` dans le modèle Thymeleaf

---

## [2.5.1] - 2026-05-05

### Améliorations

#### Road Trip — sélecteur de date (timeline)

- Ajout d'une **barre de dates horizontale défilable** au-dessus de la carte Road Trip, générée automatiquement à partir des jours représentés dans les étapes du voyage
- Un bouton **« 🗺 Tout le voyage »** (sélectionné par défaut) affiche l'itinéraire complet
- Chaque bouton de date filtre la carte pour n'afficher que **les étapes et le trajet du jour sélectionné**, avec recalcul de la distance via OSRM
- La liste des étapes sous la carte se met à jour en cohérence avec le filtre actif
- Design : boutons pill scrollables avec état actif orange (jour) / bleu (tout), connecteurs visuels entre les dates

---

## [2.5.0] - 2026-05-05

### Nouveautés

#### Fonctionnalité Road Trip — carte d'itinéraire

- Ajout d'un **bouton Road Trip** sur la page de détail d'un voyage, disponible uniquement si :
  - Le voyage est **terminé** (date de fin antérieure à aujourd'hui)
  - Le voyage possède **plus de 4 étapes géocodées** dans le planner
- Nouveau panneau **Road Trip** (`/trips/{id}/road-trip`) affichant :
  - Une **carte OpenStreetMap interactive** (Leaflet) avec tous les points GPS du voyage dans l'ordre chronologique
  - Un **itinéraire routier** calculé via l'API OSRM (routage réel sur les routes), avec tracé en dégradé orange → bleu
  - Des **marqueurs SVG numérotés** pour chaque étape avec popups (nom, localisation, date, badge départ/arrivée)
  - Des **flèches directionnelles** le long de l'itinéraire pour indiquer le sens de parcours
  - La **distance totale** du trajet en kilomètres (routière via OSRM, ou à vol d'oiseau si l'API est indisponible)
  - Trois **cartes de statistiques** : distance totale, nombre d'étapes, durée du voyage
  - La **liste détaillée des étapes** (numéro, nom, localisation, date)

#### Pays d'origine configurable

- Nouveau paramètre de configuration **Pays d'origine** dans l'administration (code ISO 3166-1 alpha-2, ex. `FR`, `DE`, `IT`)
- Valeur par défaut : `FR` (France)
- **Filtrage intelligent** sur la vue Road Trip : si le voyage se déroule dans un autre pays que le pays d'origine, les étapes situées dans le pays d'origine sont **automatiquement exclues** de la carte et du calcul de distance

#### Export / Import du pays d'origine

- Le champ `homeCountry` est désormais inclus dans l'export JSON (`ExportDto`)
- Lors d'un import, la valeur `homeCountry` est restaurée si présente dans le fichier (compatibilité ascendante : les anciens exports sans ce champ restent importables)

### Architecture

- Nouvelle entité `AppSettings` (table `app_settings`, singleton id=1) pour stocker les paramètres globaux de l'application
- Nouveau `AppSettingsRepository` et `AppSettingsService`
- Nouveau `RoadTripController` gérant l'endpoint `GET /trips/{id}/road-trip`
- Nouvelle méthode `PlannerEventRepository#countByTripIdWithCoordinates(Long tripId)`
- Le pays d'origine est déterminé pour chaque point via le `GeoCountryResolver` existant (modes local et API)

---

## [2.4.0]

### Nouveautés

#### Carte OpenStreetMap interactive — Onglet Monde

- Ajout d'une **carte interactive Leaflet** en haut de l'onglet "Monde" affichant tous les voyages géographiquement
- Chaque voyage reçoit une **couleur unique** afin de distinguer les marqueurs dans la carte
- Deux types de marqueurs :
  - **🚩 Marqueur de voyage** : placé aux coordonnées GPS du voyage ou au centre du pays si pas de GPS
  - **📍 Marqueur d'étape** : un marqueur pour chaque PlannerEvent (étape) avec localisation enregistrée
- **Clustering automatique** : groupage des marqueurs proches (déclustering au zoom)
- **Zoom automatique** : ajustement de la vue pour afficher tous les marqueurs au chargement
- **Popups interactives** : au clic, affiche image du voyage, nom, localisation et lien rapide "Voir les détails"
- Utilisation d'OpenStreetMap (gratuit, sans clé API) via CDN Leaflet
- La section des statistiques par continent reste accessible en scroll down sous la carte

### Améliorations

- Performance : la carte se charge uniquement si au moins un marqueur est disponible
- Performance géocodage : les coordonnées des étapes sont maintenant persistées sur chaque `PlannerEvent` après le premier géocodage, puis réutilisées aux chargements suivants (plus d'appel Nominatim systématique)
- Administration : suppression du géocodage Nominatim automatique au chargement de la carte Monde
- Administration : lancement manuel du géocodage des événements planner depuis l'IHM, uniquement si le géocodage Nominatim est activé
- Administration : suivi d'avancement en direct dans l'IHM (déjà géocodés, restants, total, progression du run courant, succès/échecs)
- Administration : ajout d'un bouton **Stop** pour interrompre un traitement en cours
- Administration : affichage des horodatages de début et de fin du dernier traitement
- Administration : le traitement manuel de géocodage planner respecte la limite Nominatim gratuite avec **1 appel par seconde**, sans parallélisme sur les requêtes

---

## [2.3.0] - 2026-05-04

### Nouveautés

#### Récapitulatif voyage — histogramme quotidien dépliable

- Ajout en bas de la page de détail voyage d'une section **dépliable** dédiée à l'analyse journalière des dépenses
- Cette section affiche un **histogramme des dépenses par jour** sur toute la période du voyage
- Ajout d'une **ligne horizontale de référence** correspondant au budget journalier prévu (`dailyExpenseBudget`) défini dans les paramètres du voyage
- Rendu en graphique mixte (barres + ligne) via Chart.js, initialisé à l'ouverture de la section pour garder la page légère
- Échelle Y du graphique quotidien calée à **budget journalier paramétré + 100%** (maximum = `dailyExpenseBudget × 2`) pour faciliter la lecture de l'écart

### Correctifs

#### Planner - commentaire vide conserve une valeur vide

- La modale de modification d'un événement planner ne préremplit plus le champ commentaire avec `undefined` lorsqu'aucun commentaire n'existe
- La sauvegarde normalise désormais les commentaires planner absents pour conserver une chaîne vide au lieu de persister `undefined`

#### Bouton "Ici et maintenant" — prise en compte de l'heure locale du client

- La date/heure de l'événement créé via le bouton **"Ici et maintenant"** utilise désormais l'heure locale du navigateur/mobile au lieu de l'heure du serveur
- Corrige les décalages horaires lorsque l'utilisateur se trouve dans un fuseau horaire différent de celui du serveur

---

## [2.2.0] - 2026-04-03

### Nouveautés

#### Indicateur de statut de paiement des dépenses

- Les dépenses peuvent désormais être marquées comme **"déjà payées"** via une case à cocher dans le formulaire de modification
- **Coloration visuelle** : les lignes des dépenses payées sont surlignées en **vert clair** pour une identification rapide
- Un **total payé** s'affiche en bas du tableau si des dépenses sont marquées comme payées
- ⏳ **Affichage conditionnel** : la coloration des lignes et le total payé ne s'affichent que si le voyage est en cours (date actuelle ≤ date de fin). Une fois le voyage terminé, ces informations disparaissent
- Le statut de paiement est **conservé** lors des opérations d'import/export
- Impact limité au module dépenses : aucun changement sur les calculs de budget ou autres fonctionnalités

---

## [2.1.0] — 2026-03-30

### Nouveautés

#### Protection anti-bruteforce sur l'export API key

- Ajout d'un **rate limiter** sur `GET /api/admin/export` pour limiter les tentatives abusives via API key
- Quand la limite est dépassée, l'endpoint renvoie désormais **`429 Too Many Requests`** avec l'en-tête `Retry-After`
- Configuration externalisée via propriétés :
  - `app.security.api-export-rate-limit.enabled`
  - `app.security.api-export-rate-limit.max-requests`
  - `app.security.api-export-rate-limit.window-seconds`

#### Verrouillage temporaire des comptes au login

- En cas de **5 échecs consécutifs** d'authentification, le compte est verrouillé pendant **15 minutes**
- Les tentatives pendant la fenêtre de verrouillage sont refusées et la page de connexion affiche un message dédié
- Après une authentification réussie, le compteur d'échecs est réinitialisé
- Configuration externalisée via propriétés :
  - `app.security.login-lock.max-failures`
  - `app.security.login-lock.lock-minutes`

#### Gestion avancée des clés API dans l'Administration

- **Nom de clé** : lors de la génération, il est désormais possible d'attribuer un nom à la clé (ex. `Home Assistant`, `Backup script`) pour l'identifier facilement
- **Section repliable** : toute la gestion des clés API est regroupée dans une section accordéon (Bootstrap Collapse) sur la page Administration, gardant l'interface aérée
- **Tableau de toutes les clés** : affiche, pour chaque clé, son identifiant, son nom, son statut (Active / Expirée / Révoquée) sous forme de badge coloré, sa date d'expiration, et sa **dernière utilisation avec succès**
- **Ligne rouge** pour les clés expirées ou révoquées, permettant un repérage visuel immédiat
- **Suppression individuelle** d'une clé via un bouton poubelle sur chaque ligne du tableau (confirmation requise)
- La section s'ouvre automatiquement après une génération, suppression ou erreur (via JavaScript Bootstrap)

#### Mise en évidence du jour en cours dans le Planner

- Le groupe du jour en cours est visuellement distingué des autres journées :
  - Point de timeline plus grand avec un **halo jaune**
  - Label de date en **jaune** avec une icône 📍 et un badge **"Aujourd'hui"**
  - Bordure gauche **jaune** sur chaque carte d'événement du jour
  - Fond légèrement teinté (`#fffdf0`) sur les cartes du jour
- Implémenté uniquement en CSS + `th:classappend` Thymeleaf (`#temporals.createToday()`) — aucun changement côté Java

### Technique

- Champ `name` (nullable) ajouté sur l'entité `ApiAccessKey` — Hibernate l'ajoute automatiquement en base sans migration manuelle
- `ApiAccessKeyService` : signature de `generateKey` mise à jour (`Duration, String name`), nouvelles méthodes `findAllKeys()` et `deleteKey(Long id)`
- `ApiAccessKeyRepository` : ajout de `findAllByOrderByCreatedAtDesc()`
- `AdminController` : nouveau endpoint `POST /admin/api-keys/{id}/delete`, alimentation du modèle `apiKeys` à chaque `GET /admin`
- Tests mis à jour pour refléter la nouvelle signature de `generateKey`

---

## [2.0.0] — 2026-03-30

### Nouveautés

#### Export JSON via API Key

- Nouvel endpoint `GET /api/admin/export?apiKey=<clé>` exposant le même contenu JSON que l'export admin, accessible sans session
- Génération de clés API depuis l'interface Administration avec durée de validité configurable (1, 7, 30, 90, 180 ou 365 jours — jusqu'à 12 mois)
- Les clés sont stockées exclusivement sous forme de hash SHA-256 en base de données ; la valeur brute n'est affichée qu'une seule fois au moment de la génération
- Action de **révocation complète** depuis l'interface admin : révoque instantanément toutes les clés API actives
- Toute requête avec clé absente, invalide ou expirée renvoie `401 Unauthorized`
- Clé passée en query param (`?apiKey=...`) — aucun header spécifique requis, utilisable directement dans un navigateur ou un script `curl`

#### Gestion des utilisateurs et rôles

- Trois rôles distincts supportés : **ADMIN**, **REPORTER**, **GUEST**
- L'administrateur peut créer des utilisateurs depuis **Administration → Gérer les utilisateurs** ; un mot de passe aléatoire est généré et affiché une seule fois
- Possibilité de supprimer des utilisateurs depuis la même interface
- Les utilisateurs et leurs rôles (avec mots de passe hashés) sont inclus dans l'export/import JSON, permettant une restauration complète sur une nouvelle instance
- Création automatique d'un utilisateur administrateur au premier démarrage à partir des propriétés `APP_USERNAME` / `APP_PASSWORD`

| Rôle | Permissions |
|---|---|
| **ADMIN** | Accès complet : voyages, dépenses, planner, catégories, utilisateurs, export/import |
| **REPORTER** | Création et modification d'événements planner, « ici et maintenant » ; lecture seule sur le reste |
| **GUEST** | Consultation de toutes les données, aucune modification |

#### CI/CD — GitHub Actions avec gestion des tags

- Nouveau workflow `.github/workflows/docker-build-push.yml` déclenché sur :
  - chaque push sur `main` (branche principale)
  - chaque création de tag Git (`*`)
  - chaque pull request vers `main`
- **Job `test-and-coverage`** (prérequis du build) :
  - Mise en place de Java 21 (Temurin) avec cache Maven
  - Exécution complète de la suite de tests via `mvnw clean test`
  - Génération du rapport de couverture JaCoCo, uploadé comme artefact de workflow
- **Job `build-and-push`** (conditionnel au succès des tests, sur les push uniquement) :
  - Authentification Docker Hub via secrets (`DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`)
  - Publication de l'image `<username>/mytrips:latest` à chaque push sur `main`
  - Publication de l'image versionnée `<username>/mytrips:<tag>` lors de la création d'un tag Git (ex. `v2.0.0`)

**Usage — publier une version taguée :**
```bash
git tag v2.0.0
git push origin v2.0.0
```
L'image `<username>/mytrips:v2.0.0` est construite et publiée automatiquement après que tous les tests sont passés.

### Tests

#### Couverture ajoutée

| Classe de test | Portée |
|---|---|
| `ApiAccessKeyServiceTest` | Génération, authentification, expiration, limite 12 mois, révocation complète |
| `ApiExportApiKeySecurityTest` | Sécurité endpoint `/api/admin/export` : clé absente, invalide, expirée, révoquée, header seul, clé valide ; vérification du contenu JSON retourné |
| `AppUserServiceTest` | Création avec encodage, import avec hash déjà encodé, suppression, comptage |
| `DataImportExportServiceTest` | Délégation au worker, propagation d'exception |
| `ImportExportWorkerTest` | Sérialisation/désérialisation JSON de chaque type d'entité |
| `ImportExportEndToEndTest` | **Test d'intégration complet** : import depuis un backup réel, ajout de données, re-export, re-import ; vérification des compteurs pour tous les types (voyages, dépenses, événements planner, utilisateurs, catégories) |
| `BackupFormatCompatibilityTest` | Compatibilité ascendante du format JSON de backup |
| `CategoryServiceTest` | CRUD catégories, validation contraintes |
| `ExpenseServiceTest` | Calculs sur les dépenses |
| `TripServiceTest` | Logique métier voyages |
| `TripTest` | Modèle entité voyage |
| `GeoCountryResolverTest` | Résolution géographique locale et via API |
| `LocationParserServiceTest` | Parsing des localisations GPS / texte |
| `WorldStatsServiceTest` | Agrégation statistiques carte du monde |
| `ApplicationTests` | Chargement du contexte Spring Boot |

---

## [1.1.0] — antérieur au 2026-03-30

- Import de données HopWallet (CSV)
- Géocodage inverse via Nominatim (coordonnées GPS → adresse)
- Bascule à chaud mode géographique local / API BigDataCloud
- Frise chronologique de tous les voyages
- Carte du monde avec compteurs pays / continents
- Planner d'événements par voyage avec support « ici et maintenant »
- Courbe de tendance projective sur le graphique de dépenses
- Health check Actuator pour Portainer
- Support remember-me persistant (12 mois)
- Déploiement Docker / PostgreSQL via Portainer Stack

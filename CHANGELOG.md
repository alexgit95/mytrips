# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et le versionnage suit [Semantic Versioning](https://semver.org/lang/fr/).

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

# MyTrips

Application web de gestion de voyages — suivi des dépenses, planification d'événements, frise chronologique et carte du monde visitée.

Développée en **Spring Boot 4 / Java 21**, interface **Thymeleaf + Bootstrap 5**, persistance **PostgreSQL** (Docker) ou **SQLite** (local).

---

## Sommaire

1. [Installation dans Portainer](#1-installation-dans-portainer)
2. [Démarrage en local](#2-démarrage-en-local)
3. [Script de déploiement Docker Hub](#3-script-de-déploiement-docker-hub)
4. [Fonctionnalités](#4-fonctionnalités)

---

## 1. Installation dans Portainer

### Prérequis

- Portainer avec accès à un Docker Engine (Raspberry Pi, NAS, VPS…)
- Image publiée sur Docker Hub (voir [section 3](#3-script-de-déploiement-docker-hub))

### Déploiement via Stack Portainer

Dans Portainer, créer une nouvelle **Stack** et coller le contenu suivant :

```yaml
services:

  app:
    image: <votre-dockerhub-username>/mytrips:latest
    container_name: mytrips-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      POSTGRES_USER: mytrips
      POSTGRES_PASSWORD: mytrips_secret
      APP_USERNAME: admin
      APP_PASSWORD: votre_mot_de_passe
      APP_REMEMBER_ME_KEY: une_cle_secrete_longue_et_aleatoire
      GEO_API_ENABLED: "false"
      GEOCODING_ENABLED: "false"
      APP_LOGIN_LOCK_MAX_FAILURES: "5"
      APP_LOGIN_LOCK_MINUTES: "15"
      APP_API_EXPORT_RATE_LIMIT_ENABLED: "true"
      APP_API_EXPORT_RATE_LIMIT_MAX_REQUESTS: "30"
      APP_API_EXPORT_RATE_LIMIT_WINDOW_SECONDS: "60"
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  db:
    image: postgres:16-alpine
    container_name: mytrips-db
    environment:
      POSTGRES_DB: mytrips
      POSTGRES_USER: mytrips
      POSTGRES_PASSWORD: mytrips_secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U mytrips -d mytrips"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  postgres_data:
    name: mytrips_postgres_data
```

### Variables d'environnement

| Variable | Obligatoire | Défaut | Description |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ✅ | — | Doit valoir `docker` pour activer PostgreSQL |
| `POSTGRES_USER` | ✅ | `mytrips` | Utilisateur PostgreSQL (même valeur dans `app` et `db`) |
| `POSTGRES_PASSWORD` | ✅ | `mytrips_secret` | Mot de passe PostgreSQL — **à changer en production** |
| `APP_USERNAME` | ✅ | `admin` | Identifiant de connexion de l'administrateur initial |
| `APP_PASSWORD` | ✅ | `admin` | Mot de passe de l'administrateur initial — **à changer en production** |
| `APP_REMEMBER_ME_KEY` | ✅ | *(valeur par défaut non sécurisée)* | Clé de signature des cookies "Se souvenir de moi" — **générer une chaîne aléatoire longue** |
| `GEO_API_ENABLED` | ❌ | `false` | `true` = active l'API BigDataCloud pour la résolution géographique (page Monde) ; `false` = résolution locale hors-ligne |
| `GEOCODING_ENABLED` | ❌ | `false` | `true` = active l'API Nominatim (OpenStreetMap) pour convertir les coordonnées GPS en adresses dans le planner « ici et maintenant » ; `false` = stocke juste les coordonnées GPS |
| `APP_LOGIN_LOCK_MAX_FAILURES` | ❌ | `5` | Nombre d'échecs de connexion consécutifs avant verrouillage temporaire du compte |
| `APP_LOGIN_LOCK_MINUTES` | ❌ | `15` | Durée du verrouillage temporaire du compte (en minutes) après dépassement du seuil d'échecs |
| `APP_API_EXPORT_RATE_LIMIT_ENABLED` | ❌ | `true` | Active (`true`) ou désactive (`false`) le rate limiter anti-bruteforce de `GET /api/admin/export` |
| `APP_API_EXPORT_RATE_LIMIT_MAX_REQUESTS` | ❌ | `30` | Nombre maximal de requêtes autorisées dans la fenêtre temporelle pour `GET /api/admin/export` |
| `APP_API_EXPORT_RATE_LIMIT_WINDOW_SECONDS` | ❌ | `60` | Durée de la fenêtre (en secondes) utilisée pour limiter les requêtes de `GET /api/admin/export` |

> **Note :** Le schéma de la base de données est créé et mis à jour automatiquement au démarrage (`ddl-auto=update`). Aucun script SQL n'est nécessaire.

### Accès

Une fois la stack démarrée : `http://<ip-du-serveur>:8080`

---

## 2. Démarrage en local

### Prérequis

- Java 21+
- Maven 3.9+ (ou utiliser le wrapper inclus `mvnw`)

### Lancement

```bash
# Cloner le dépôt
git clone https://github.com/alexgit95/mytrips.git
cd mytrips

# Démarrer l'application (profil par défaut = SQLite)
./mvnw spring-boot:run
# ou sur Windows :
mvnw.cmd spring-boot:run
```

L'application démarre sur **http://localhost:8080**.

Un fichier `mytrips.db` (SQLite) est créé automatiquement dans le répertoire courant.

### Identifiants par défaut (local)

| Champ | Valeur |
|---|---|
| Identifiant | `admin` |
| Mot de passe | `admin` |

Au premier démarrage, un utilisateur administrateur est créé automatiquement avec ces identifiants. D'autres utilisateurs peuvent ensuite être créés depuis la page **Administration > Gérer les utilisateurs**.

Ces valeurs sont configurables dans `src/main/resources/application.properties` :

```properties
app.security.username=admin
app.security.password=admin
app.security.remember-me-key=mytrips-remember-me-change-me-in-production
```

### Résolution géographique en local

Par défaut, la résolution est **hors-ligne** (bounding boxes). Pour activer l'API BigDataCloud (nécessite internet) :

```properties
# src/main/resources/application.properties
app.geo.api-enabled=true
```

Le mode peut aussi être basculé à chaud depuis la **page Administration** sans redémarrer.

---

## 3. Script de déploiement Docker Hub

Le script `deploy.sh` automatise la construction et la publication d'une nouvelle version de l'image.

### Ce que fait le script

1. Clone le dépôt GitHub dans un répertoire temporaire
2. Demande le mot de passe Docker Hub (saisie masquée)
3. Se connecte à Docker Hub via `--password-stdin`
4. Construit l'image Docker avec `docker build`
5. Pousse l'image vers `<username>/<repo>:latest`
6. Nettoie le répertoire temporaire

### Prérequis

- Docker installé et démon actif sur la machine
- Accès réseau à GitHub et Docker Hub
- Droits en écriture sur le dépôt Docker Hub cible

### Utilisation

```bash
# Rendre le script exécutable (une seule fois)
chmod +x deploy.sh

# Lancer le déploiement
./deploy.sh <docker-username> <docker-repo>

# Exemple :
./deploy.sh alexgit95 mytrips
```

Le script demande interactivement le mot de passe Docker Hub — il n'est jamais exposé dans la liste des processus.

### Mettre à jour l'application dans Portainer

Après la publication d'une nouvelle image :

1. Dans Portainer → **Stacks** → sélectionner la stack `mytrips`
2. Cliquer sur **Pull and redeploy** (ou **Update the stack**)
3. L'application redémarre avec la nouvelle image ; les données PostgreSQL sont préservées dans le volume `mytrips_postgres_data`

---

## 4. Fonctionnalités

### Liste des voyages

Vue principale listant tous les voyages avec pour chacun : nom, dates, localisation, budget total, montant dépensé, budget restant et barre de progression colorée (vert / orange / rouge selon le taux de consommation).

### Détail d'un voyage

Page centrale d'un voyage comprenant :

- **Informations générales** : nom, dates, destination, description, image de couverture (URL), coordonnées GPS, pays, budget global, budget journalier
- **Statistiques** : total dépensé, budget restant, nombre de dépenses
- **Graphique camembert** : répartition des dépenses par catégorie
- **Graphique linéaire** : courbe d'évolution des dépenses avec ligne de budget et courbe de tendance (voir ci-dessous)
- **Tableau des dépenses** : liste paginable avec date, libellé, catégorie, montant et boutons éditer/supprimer

#### Courbe de projection (tendance)

Le graphique linéaire affiche trois séries :

| Série | Couleur | Description |
|---|---|---|
| **Dépenses cumulées** | Bleu | Cumul réel jour par jour depuis le début du voyage jusqu'à aujourd'hui |
| **Budget** | Rouge pointillé | Ligne horizontale fixe représentant le budget total du voyage |
| **Tendance projetée** | Orange pointillé | Projection du reste du voyage à partir du dernier jour saisi |

**Calcul de la tendance :** à partir du dernier jour pour lequel une dépense a été enregistrée, la courbe prolonge la trajectoire en ajoutant chaque jour le **budget journalier configuré** (`dailyExpenseBudget`). Si des dépenses futures ont déjà été pré-saisies (ex. hôtel réservé), elles s'ajoutent au dessus du budget journalier pour les jours concernés. Cela permet de visualiser si les dépenses actuelles et prévues vont tenir dans le budget total avant la fin du voyage.

> Si aucun budget journalier n'est défini et qu'aucune dépense future n'est pré-saisie, la courbe de tendance n'est pas affichée.

### Gestion des dépenses

Formulaire d'ajout/édition avec : date, libellé, catégorie (icône + nom), montant, et nombre de jours (pour les dépenses qui s'étalent sur plusieurs jours, ex. location de voiture — le montant est réparti équitablement sur chaque jour pour le calcul du graphique).

### Planner

Planificateur d'événements par voyage. Chaque événement contient : date, heure, titre, description, localisation géographique (utilisée pour la résolution pays/département sur la page Monde). Les événements sont affichés sous forme de timeline verticale groupée par journée.

### Frise chronologique

Vue synthétique de tous les voyages classés par année en alternance gauche/droite, avec image de couverture, badge de dates, barre de budget et lien vers le détail.

### Carte du monde (Monde)

Agrège les données de localisation de tous les voyages et événements planner pour afficher :

- **Compteurs** : nombre de pays visités, nombre de continents
- **Détail par continent** : liste des pays avec drapeau emoji, nom en français, nombre de départements (France) ou d'états (USA) visités
- **Sources** : indique si la localisation vient des coordonnées GPS d'un voyage, de son champ pays, ou d'un événement planner

La résolution géographique fonctionne en deux modes configurables depuis l'administration :

| Mode | Description |
|---|---|
| **Local (par défaut)** | Résolution hors-ligne par bounding boxes avec tiebreaker centroïde — aucun appel réseau |
| **API BigDataCloud** | Reverse geocoding précis (pays, département français depuis le code postal, état américain depuis ISO 3166-2) — API gratuite, sans clé |

### Administration

Accessible via le menu **Administration** (visible uniquement pour les administrateurs), regroupe :

- **Export JSON** : téléchargement de l'intégralité des données (voyages, dépenses, événements, utilisateurs) au format JSON
- **Export JSON via API key** : génération et gestion de clés API temporaires pour appeler `GET /api/admin/export` avec le query param `apiKey` (voir ci-dessous)
- **Import JSON** : rechargement complet depuis un fichier d'export (remplace toutes les données existantes, y compris les utilisateurs)
- **Import HopWallet** : import depuis un export CSV de l'application HopWallet (ajout aux données existantes)
- **Catégories** : CRUD complet des catégories de dépenses (nom + icône emoji)
- **Gestion des utilisateurs** : création et suppression d'utilisateurs avec attribution de rôles
- **Résolution géographique** : bascule à chaud entre mode local et mode API BigDataCloud, sans redémarrage ; affiche le mode actuellement actif

#### Export API key (automatisation)

Depuis **Administration**, la section **API Keys** (repliable) permet de :

- **Créer une clé** en lui donnant un **nom** (optionnel, ex. `Home Assistant`, `Backup script`) et une durée de validité (de 1 jour à 12 mois / 365 jours)
- **Visualiser toutes les clés** dans un tableau : nom, statut (Active / Expirée / Révoquée) avec badge coloré, date d'expiration, **dernière utilisation avec succès** ; les lignes des clés expirées ou révoquées apparaissent en rouge
- **Supprimer** une clé individuelle (bouton poubelle, confirmation requise)
- **Révoquer toutes les clés actives** en une seule action

La clé brute n'est affichée qu'une seule fois au moment de la génération — elle est stockée sous forme de hash SHA-256 en base de données. Une clé expirée, révoquée ou invalide renvoie `401 Unauthorized`. En cas de dépassement du rate limiter anti-bruteforce sur cet endpoint, l'API renvoie `429 Too Many Requests` (avec en-tête `Retry-After`).

Exemple d'appel :

```bash
curl "http://localhost:8080/api/admin/export?apiKey=mtk_xxx" -o mytrips-export.json
```

### Gestion des utilisateurs et rôles

L'application supporte trois niveaux de rôles avec des permissions différenciées :

| Rôle | Description | Permissions |
|---|---|---|
| **🔑 Administrateur** | Accès complet | Toutes les fonctionnalités : CRUD voyages, dépenses, planner, catégories, gestion des utilisateurs, export/import |
| **📝 Reporter** | Lecture + planner limité | Peut créer et modifier les événements du planner, utiliser « ici et maintenant », ajouter/modifier des commentaires. Lecture seule pour les autres données. Pas d'accès à l'administration |
| **👁 Invité** | Lecture seule | Consultation de toutes les données (voyages, dépenses, planner, frise, carte du monde). Aucune modification possible. Pas d'accès à l'administration |

#### Création d'un utilisateur

Depuis **Administration > Gérer les utilisateurs**, l'administrateur peut :

1. Saisir un **nom d'utilisateur**
2. Choisir un **rôle** (Administrateur, Reporter ou Invité)
3. Valider : un **mot de passe est généré automatiquement** et affiché une seule fois

> **Important :** le mot de passe généré n'est affiché qu'au moment de la création. Il doit être noté et communiqué à l'utilisateur concerné.

Au premier démarrage de l'application, un utilisateur administrateur est créé automatiquement avec les identifiants configurés dans les propriétés (`APP_USERNAME` / `APP_PASSWORD` ou `app.security.username` / `app.security.password`).

#### Export / Import des utilisateurs

Les utilisateurs et leurs rôles sont inclus dans l'export JSON. Lors d'un import, les utilisateurs sont restaurés avec leurs mots de passe hashés, permettant une restauration complète de l'application sur une nouvelle instance.

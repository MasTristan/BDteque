# CONTRATS DE DONNÉES

> Référence normative des formats et des schémas. Tout écart est un défaut.
> Pilotes : CHARPENTE (schémas de base), SOURCE (formats de catalogue).

---

## 1. Les trois moitiés

```
Moitié A — LA COLLECTION            Moitié B — LES SORTIES        Moitié C — LE CATALOGUE
Room · bdshelf.db                   JSON · filesDir               Room · catalog.db
propriété de l'utilisateur          donnée curée, signée          donnée publique, signée
IRREMPLAÇABLE                       jetable                       jetable
sauvegardée, exportée               ni sauvegardée ni exportée    ni sauvegardée ni exportée
lecture / écriture                  lecture seule                 lecture seule
```

**Interdits absolus** — aucune clé étrangère, jointure SQL ou transaction ne traverse
A↔B, A↔C ou B↔C. Le croisement se fait **en mémoire**, dans le domaine, par identifiant.
Modèle de référence : `buildShoppingList()`, qui combine déjà A et B sans les mélanger.

---

## 2. Moitié A — schéma de la collection

### 2.1 État cible (version 4)

```
series
  id              TEXT     PK    slug stable, ex. "thorgal"
  title           TEXT     NN
  status          TEXT     NN    ONGOING | FINISHED | UNKNOWN
  isTracked       INTEGER  NN
  color           INTEGER  NN    ARGB, déterministe depuis id
  knownTomeCount  INTEGER  NULL  null = série ouverte
  notes           TEXT     NULL
  catalogSeriesId TEXT     NULL  v3 — lien vers Moitié C, NON contraint

albums
  id              TEXT     PK    "${seriesId}-${tomeNumber}" | "${seriesId}-hs-${index}"
  seriesId        TEXT     NN    FK → series(id) ON DELETE CASCADE
  tomeNumber      INTEGER  NULL  null = hors-série
  title           TEXT     NULL
  owned           INTEGER  NN    false = trou connu
  readStatus      TEXT     NN    UNREAD | READ | LENT
  edition         TEXT     NULL
  ean             TEXT     NULL  UNIQUE
  dateAdded       INTEGER  NN    epoch millis
  lentTo          TEXT     NULL  v4 — significatif si readStatus = LENT
  lentAt          INTEGER  NULL  v4
  wishlisted      INTEGER  NN    v4 — défaut 0, exclu des compteurs

isbn_lookup_cache
  isbn            TEXT     PK
  title           TEXT     NN
  seriesName      TEXT     NULL
  tomeNumber      INTEGER  NULL
  authors         TEXT     NN
  source          TEXT     NN    "bnf" | "openlibrary"
  fetchedAt       INTEGER  NN

index : albums(seriesId) · albums(ean) UNIQUE · series(catalogSeriesId)
```

### 2.2 Historique des versions

| Version | Contenu | Statut |
|---|---|---|
| 1 | `series`, `albums` | Livrée |
| 2 | `isbn_lookup_cache` | Livrée — **migration jamais testée** (dette D3) |
| 3 | `series.catalogSeriesId` | [E1](E1-catalogue.md) |
| 4 | `albums.lentTo`, `lentAt`, `wishlisted` | [E5](E5-partage.md) |

### 2.3 Règles de migration

- `app/schemas/*.json` versionné au dépôt, **rétroactivement pour 1 et 2**.
- Chaque version dispose d'un test de migration sur base **peuplée**.
- Le chemin complet 1 → N est testé, pas seulement les sauts unitaires : c'est le
  chemin réel d'un utilisateur de la v1.
- `fallbackToDestructiveMigration()` **interdit**, vérifié par analyse statique.
- Une PR modifiant une entité sans migration ni schéma est refusée automatiquement.

---

## 3. Moitié A — formats d'échange

### 3.1 Sauvegarde v2

```json
{
  "formatVersion": 2,
  "app": { "versionName": "2.0.0", "dbVersion": 3 },
  "createdAt": "2026-11-14T09:12:00Z",
  "counts": { "series": 47, "albums": 612 },
  "checksum": "sha256:…",
  "collection": {
    "series": [ { "id": "thorgal", "title": "Thorgal", "status": "ONGOING", "isTracked": true,
                  "color": 4287269261, "knownTomeCount": null, "notes": null,
                  "catalogSeriesId": "thorgal" } ],
    "albums": [ { "id": "thorgal-40", "seriesId": "thorgal", "tomeNumber": 40,
                  "title": "Le Feu écarlate", "owned": true, "readStatus": "READ",
                  "edition": null, "ean": "9782803680122", "dateAdded": 1763107920000,
                  "lentTo": null, "lentAt": null, "wishlisted": false } ]
  }
}
```

**Règles**
- `checksum` couvre l'objet `collection` uniquement (stable à la relecture).
- L'en-tête permet la prévisualisation **sans charger tout le fichier**.
- Le format v1 (objet `CollectionSnapshot` nu) reste lisible : détection par absence de
  `formatVersion`.
- Validation via `SnapshotValidation` **avant** toute écriture.
- Chiffrement optionnel : AES-256-GCM, clé dérivée par Argon2id, en-tête en clair pour
  permettre la prévisualisation du nombre d'albums.

### 3.2 Export CSV

Format lisible, déjà produit par `CollectionExport`. Colonnes stables — c'est un contrat
public : des gens ouvrent ce fichier dans un tableur.

```csv
Série;Tome;Titre;Possédé;Lecture;Édition;Code-barres;Ajouté le
Thorgal;40;Le Feu écarlate;oui;lu;;9782803680122;2026-11-14
```

- Séparateur `;` (tableurs francophones), encodage UTF-8 avec BOM.
- En-têtes **en français** : ce fichier est lu par des humains.
- Réimportable par [E3](E3-amorcage.md) sans perte.

### 3.3 Liste partagée `.bdliste`

```json
{
  "formatVersion": 1,
  "kind": "wishlist",
  "label": "Ce qu'il manque à la collection de Michel",
  "createdAt": "2026-11-14",
  "items": [
    { "series": "Thorgal", "tome": 21, "title": "La Couronne d'Ogotaï", "ean": "9782803610730" }
  ]
}
```

**Interdits, vérifiés par test automatisé** ([ADR-008](../DECISIONS.md#adr-008)) :
identifiant d'appareil ou d'installation · URL · horodatage plus précis que le jour ·
tout élément de collection au-delà des items listés.

---

## 4. Moitié B — les sorties

Format existant, **conservé**, enrichi d'une signature ([ADR-007](../DECISIONS.md#adr-007)).

```json
{
  "version": 1,
  "updatedAt": "2026-11-14",
  "releases": [
    { "seriesId": "wunderwaffen", "seriesTitle": "Wunderwaffen", "tomeNumber": 13,
      "title": "Le Dernier Recours", "expectedDate": "2026-09-18",
      "status": "UPCOMING", "note": "" }
  ]
}
```

- `status` : `UPCOMING` | `RELEASED`.
- Dédoublonnage par (série, tome) à la lecture — comportement existant, conservé.
- Cache : fichier JSON dans `filesDir`, **jamais** dans Room.
- Après [E1](E1-catalogue.md), ce fichier devient **facultatif** : il ne sert plus qu'à
  annoncer un album absent du dépôt légal. La source principale est le catalogue.
- Signature Ed25519 en fichier `.sig` associé, avec une version de tolérance pour ne pas
  casser les installations existantes.

---

## 5. Moitié C — le catalogue

### 5.1 Schéma `catalog.db`

```
catalog_series
  id              TEXT     PK
  title           TEXT     NN
  normalizedTitle TEXT     NN    via TextNormalization
  publisher       TEXT     NULL
  status          TEXT     NN
  firstYear       INTEGER  NULL
  tomeCount       INTEGER  NULL  ← alimente la détection de trous réels

catalog_album
  ean             TEXT     PK    EAN-13 canonique
  seriesId        TEXT     NN    PAS de clé étrangère (volontaire)
  tomeNumber      INTEGER  NULL
  title           TEXT     NN
  normalizedTitle TEXT     NN
  publisher       TEXT     NULL
  publishedDate   TEXT     NULL  ISO-8601, précision variable
  pageCount       INTEGER  NULL

catalog_alias
  normalizedAlias TEXT     PK(1)
  seriesId        TEXT     PK(2)

catalog_meta
  id              INTEGER  PK = 1
  version         TEXT     NN    "2026.11"
  builtAt         INTEGER  NN
  sourceLabel     TEXT     NN    mention Etalab, obligatoire à l'affichage
  albumCount      INTEGER  NN
  seriesCount     INTEGER  NN

+ catalog_series_fts (FTS4 sur normalizedTitle)
index : catalog_album(seriesId) · catalog_album(normalizedTitle)
```

**Absence de clé étrangère assumée** : un bundle partiel ou une série retirée ne doit
jamais empêcher un import. Un album orphelin est une donnée dégradée acceptable ; la
rigueur relationnelle appartient à la Moitié A, où la donnée est irremplaçable.

### 5.2 Manifeste de bundle

```json
{
  "version": "2026.11",
  "schemaVersion": 1,
  "builtAt": "2026-11-01T02:00:00Z",
  "source": "Catalogue général de la Bibliothèque nationale de France — Licence Ouverte Etalab 2.0",
  "sourceUpdatedAt": "2026-10-28",
  "counts": { "series": 4120, "albums": 34887 },
  "body": { "file": "catalog-2026.11.jsonl.zst", "bytes": 11238400, "sha256": "…" },
  "supersedes": "2026.10"
}
```

### 5.3 Corps du bundle (JSON Lines, Zstandard)

Une entité par ligne — lisible en flux, sans charger 35 000 albums en mémoire.

```jsonl
{"t":"s","id":"thorgal","title":"Thorgal","publisher":"Le Lombard","status":"ONGOING","firstYear":1980,"tomeCount":41}
{"t":"a","ean":"9782803680122","seriesId":"thorgal","tome":40,"title":"Le Feu écarlate","published":"2024-11-15","pages":48}
{"t":"x","alias":"thorgal la jeunesse","seriesId":"thorgal-jeunesse"}
```

| `t` | Entité | Champs |
|---|---|---|
| `s` | série | `id` `title` `publisher?` `status` `firstYear?` `tomeCount?` |
| `a` | album | `ean` `seriesId` `tome?` `title` `published?` `pages?` |
| `x` | alias | `alias` `seriesId` |

**Règles de lecture**
- Type `t` inconnu → **ligne ignorée**, pas d'erreur. C'est ce qui permettra d'enrichir
  le format sans casser les anciennes versions de l'application.
- Ligne malformée → ignorée et comptée ; au-delà de 1 % de lignes ignorées, l'import
  est rejeté par les contrôles de vraisemblance.
- Ordre non garanti : les albums peuvent précéder leur série.

### 5.4 Chaîne de confiance

```
latest.json (signé) → manifeste (signé) → SHA-256 du corps → import en base neuve
                                                            → contrôles de vraisemblance
                                                            → bascule atomique
```

- Signature **Ed25519**, clé publique dans `BuildConfig.CATALOG_PUBLIC_KEY`.
- Deux clés acceptées simultanément (rotation sans casse).
- Échec à n'importe quelle étape → arrêt silencieux, artefact précédent conservé,
  entrée dans le journal local, **aucun envoi**.
- Contrôles de vraisemblance : `albumCount` à ±5 % du manifeste · ≥ 1 000 séries ·
  aucune table vide.

### 5.5 Sous-ensemble embarqué

`assets/catalog_starter.jsonl.zst` — 400 séries, ≈ 6 000 albums, ≈ 2 Mo.
Même format, même chaîne de vérification. **Aucune exception au chemin de confiance**,
y compris pour un fichier livré dans l'APK.

---

## 6. Amorçage historique

`assets/seed-collection.json` — format inchangé, importé **une seule fois**
(garde `seed_imported` en DataStore), idempotent.

```json
{ "version": 1, "series": [
  { "id": "thorgal", "title": "Thorgal", "status": "ONGOING", "tracked": true,
    "knownTomeCount": null, "ownedTomes": [1,2,3], "notes": "" } ] }
```

Ce fichier reste destiné à l'utilisateur d'origine. Pour tout nouvel utilisateur, c'est
[E3](E3-amorcage.md) qui prend le relais.

---

## 7. Budgets opposables

| Contrat | Budget | Contrôle |
|---|---|---|
| Bundle catalogue compressé | < 15 Mo | CI du pipeline |
| `catalog.db` installée | < 60 Mo | CI applicative |
| Sous-ensemble embarqué | < 3 Mo | CI applicative |
| Sauvegarde d'une collection de 2 000 albums | < 2 Mo | Test |
| Import de bundle complet | < 90 s en tâche de fond | Banc de performance |
| Résolution EAN hors-ligne | p95 < 200 ms | Banc de performance |
| Recherche dans le catalogue | < 150 ms | Banc de performance |
</content>

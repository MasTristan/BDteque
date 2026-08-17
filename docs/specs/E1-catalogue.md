# E1 — LE CATALOGUE

> Référentiel BD franco-belge embarqué, signé, hors-ligne.
> **24 sprints-personne · H1 (sprints 1–6) · pilote : SOURCE, avec CHARPENTE**
> Décisions applicables : [ADR-003](../DECISIONS.md#adr-003), [ADR-004](../DECISIONS.md#adr-004), [ADR-007](../DECISIONS.md#adr-007)

---

## 1. Pourquoi

L'application ne connaît aujourd'hui que ce que l'utilisateur lui a dit. Trois
conséquences, toutes graves :

**D5 — Les trous sont faux.** `GapDetector.gaps()` ne peut chercher des trous qu'entre
1 et le plus grand tome connu. Une personne possédant les tomes 1 à 13 d'une série qui
en compte 22 voit « collection complète ». C'est exactement la question posée en
librairie, et la réponse est fausse.

**D6 — Les sorties dépendent d'une personne.** `releases.json` est maintenu à la main.
Il est aujourd'hui **vide** : l'écran « À paraître » fonctionne parfaitement et n'a rien
à afficher.

**D4 — Chaque scan fuite.** `IsbnLookupService` envoie l'ISBN scanné à la BnF et à Open
Library. Aucune donnée personnelle au sens du RGPD, mais le flux des requêtes corrélé à
une adresse IP raconte ce qu'une personne achète, où et quand. Face à la promesse
« aucune collecte » du README, c'est une contradiction.

**Un seul dispositif résout les trois** : télécharger le catalogue **en entier**, une
fois par mois, et l'interroger localement. On ne pose plus de questions à personne —
donc plus rien à fuiter.

---

## 2. Périmètre

### Dans le périmètre
- Pipeline de moissonnage BnF → bundle catalogue versionné et signé (dépôt séparé).
- Format de bundle, signature Ed25519, manifeste, deltas mensuels.
- Base Room séparée `catalog.db`, alimentée par import de bundle.
- Sous-ensemble d'amorçage embarqué dans l'APK (fonctionne au premier lancement, sans réseau).
- Résolution EAN → album, et série → liste complète des tomes.
- Correspondance catalogue ↔ séries existantes de l'utilisateur.
- Détection de trous **réelle**, au-delà du dernier tome possédé.
- Alimentation de l'écran « À paraître » depuis le catalogue.
- Écran de gestion du catalogue dans les réglages.

### Hors périmètre
- Manga et comics (réexamen T3 2027, [ROADMAP §6](../ROADMAP.md)).
- Couvertures dans le bundle ([ADR-006](../DECISIONS.md#adr-006) : poids et droits distincts).
- Toute modification du catalogue par l'utilisateur — il est en lecture seule. Une
  correction se fait dans **sa** collection, qui a toujours raison contre le catalogue.
- Contribution communautaire de données (autre produit, exigerait un compte).
- Prix, cotes, disponibilité en librairie ([ADR-004](../DECISIONS.md#adr-004)).

---

## 3. Récits utilisateur

### US-1.1 — Scanner un album inconnu, sans réseau
> En tant que collectionneur en librairie sans réseau, je veux savoir quel album je
> tiens, même s'il n'est pas encore dans ma collection.

**Critères d'acceptation**

```
Étant donné  un téléphone en mode avion, catalogue installé
Quand        je scanne le code-barres d'un album de BD franco-belge absent de ma collection
Alors        le verdict affiche le titre de l'album, sa série et son numéro de tome
Et           l'affichage complet intervient en moins de 1,5 s (p95)
Et           aucune requête réseau n'est émise
```
```
Étant donné  un album dont l'EAN est absent du catalogue
Quand        je le scanne hors-ligne
Alors        le verdict « Nouveau code-barres » s'affiche comme aujourd'hui
Et           aucun message d'erreur technique n'apparaît
Et           la saisie manuelle assistée reste proposée
```

### US-1.2 — Voir ses vrais trous
> En tant que collectionneur, je veux savoir ce qu'il me manque dans une série, y
> compris les tomes parus après le dernier que je possède.

```
Étant donné  une série dont je possède les tomes 1 à 13, et dont le catalogue en connaît 22
Quand        j'ouvre l'étagère de cette série
Alors        les tomes 14 à 22 apparaissent comme manquants
Et           ils sont visuellement distincts des trous internes (tomes jamais possédés
             entre 1 et 13), car ils n'ont pas la même signification
```
```
Étant donné  une série absente du catalogue
Quand        j'ouvre son étagère
Alors        le comportement actuel est conservé à l'identique (trous jusqu'au tome max connu)
Et           aucune régression n'est introduite
```

### US-1.3 — La collection a toujours raison
> En tant qu'utilisateur, je veux que mes corrections priment sur le catalogue.

```
Étant donné  un album dont j'ai corrigé le titre à la main
Quand        le catalogue est mis à jour avec un titre différent
Alors        mon titre est conservé
Et           le catalogue n'écrase jamais une donnée saisie par l'utilisateur
```

### US-1.4 — Mettre à jour le catalogue
```
Étant donné  une nouvelle version du catalogue disponible
Quand        le téléphone est sur Wi-Fi et en charge (ou que je le demande explicitement)
Alors        la mise à jour se fait en tâche de fond, sans interrompre l'usage
Et           l'écran Réglages affiche la version installée et sa date
```
```
Étant donné  un téléchargement de catalogue interrompu à 80 %
Quand        je rouvre l'application
Alors        le catalogue précédent est intact et utilisable
Et           le téléchargement partiel a été supprimé
Et           une nouvelle tentative repart proprement
```

### US-1.5 — Refuser une donnée non authentique
```
Étant donné  un bundle dont la signature est invalide ou absente
Quand        l'application tente de l'importer
Alors        l'import est refusé
Et           le catalogue précédent reste en place
Et           aucune donnée du bundle rejeté n'atteint la base
Et           l'incident est consigné dans le journal local, sans envoi
```

### US-1.6 — Les sorties sans intervention humaine
```
Étant donné  un catalogue à jour contenant des albums à paraître dans les 6 prochains mois
Quand        j'ouvre l'écran « À paraître »
Alors        les sorties des séries que je suis apparaissent, triées par date
Et           aucun fichier maintenu à la main n'est nécessaire
```

### US-1.7 — Premier lancement sans réseau
```
Étant donné  une installation neuve sur un téléphone sans réseau
Quand        je scanne un album d'une série majeure
Alors        le sous-ensemble embarqué dans l'application permet de l'identifier
Et           l'application propose de télécharger le catalogue complet plus tard
```

---

## 4. Conception technique

### 4.1 Vue d'ensemble

```
   [ Dépôt bdteque-catalog ]                    [ Application ]

   BnF SRU / UNIMARC                            catalog_starter.zst  (assets, ~2 Mo)
          │  moissonnage mensuel                         │ premier lancement
          ▼                                              ▼
   normalisation + déduplication               ┌──────────────────────┐
          │                                    │   catalog.db (Room)  │  ← lecture seule
          ▼                                    │   35 000 albums      │
   catalog-YYYY.MM.bundle.zst                  └──────────┬───────────┘
   + manifest.json + signature Ed25519                    │
          │                                               │ en mémoire, par identifiant
          ▼  publication (stockage objet statique)        ▼
   HTTPS ──────── téléchargement mensuel ──────►  bdshelf.db (collection)
                  vérification signature                  ▲
                  import atomique                          │ jamais de jointure SQL
```

**Aucun serveur.** Le point de distribution est un stockage de fichiers statiques.
Rien n'est calculé à la demande, rien n'est journalisé côté service, il n'y a pas
d'API. C'est ce qui rend le pilier P5 (durabilité à dix ans) tenable.

### 4.2 Base `catalog.db` (Moitié C)

Instance Room **distincte**, créée dans `BdShelfApplication`, ouverte en lecture seule
par l'application ([ADR-003](../DECISIONS.md#adr-003)).

```kotlin
@Entity(tableName = "catalog_series")
data class CatalogSeries(
    @PrimaryKey val id: String,        // slug stable, ex: "thorgal"
    val title: String,
    val normalizedTitle: String,       // via TextNormalization, pour la recherche
    val publisher: String?,
    val status: String,                // ONGOING | FINISHED | UNKNOWN
    val firstYear: Int?,
    val tomeCount: Int?,               // nb de tomes connus — alimente les trous réels
)

@Entity(tableName = "catalog_album", indices = [Index("seriesId"), Index("normalizedTitle")])
data class CatalogAlbum(
    @PrimaryKey val ean: String,       // EAN-13 canonique
    val seriesId: String,              // pas de clé étrangère : un bundle partiel reste valide
    val tomeNumber: Int?,
    val title: String,
    val normalizedTitle: String,
    val publisher: String?,
    val publishedDate: String?,        // ISO-8601, précision variable : "2026", "2026-09", "2026-09-18"
    val pageCount: Int?,
)

@Entity(tableName = "catalog_alias", primaryKeys = ["normalizedAlias", "seriesId"])
data class CatalogAlias(                // "thorgal la jeunesse" → thorgal-jeunesse
    val normalizedAlias: String,
    val seriesId: String,
)

@Entity(tableName = "catalog_meta")
data class CatalogMeta(
    @PrimaryKey val id: Int = 1,
    val version: String,               // "2026.11"
    val builtAt: Long,
    val sourceLabel: String,           // mention Etalab obligatoire, affichée dans À propos
    val albumCount: Int,
    val seriesCount: Int,
)
```

Plus une table virtuelle **FTS4** `catalog_series_fts` sur `normalizedTitle` pour la
recherche de série (budget : < 150 ms sur 35 000 entrées, Galaxy A14).

> **Note de conception.** `CatalogAlbum.seriesId` n'est **pas** une clé étrangère.
> Un bundle partiel ou une série retirée ne doit jamais empêcher l'import : un album
> orphelin est une donnée dégradée acceptable, pas une erreur d'intégrité. La rigueur
> relationnelle appartient à la Moitié A, où la donnée est irremplaçable.

### 4.3 Modification de la Moitié A — migration v2 → v3

```sql
ALTER TABLE series ADD COLUMN catalogSeriesId TEXT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS index_series_catalogSeriesId ON series(catalogSeriesId);
```

C'est **la seule** modification de la collection. Un simple lien, nullable, non
contraint : il peut pointer vers un identifiant absent du catalogue courant sans rien
casser.

**Exigences de migration** (porte G5, [ADR-005](../DECISIONS.md#adr-005))
- `app/schemas/2.json` et `3.json` versionnés au dépôt.
- Test de migration 1→2→3 sur base réelle peuplée.
- Test de migration destructrice **interdit** : `fallbackToDestructiveMigration()` ne
  doit apparaître nulle part dans le code.

### 4.4 Format de bundle

**Fichiers publiés** pour une version `2026.11` :

| Fichier | Contenu |
|---|---|
| `catalog-2026.11.jsonl.zst` | Corps : une entité JSON par ligne, compressé Zstandard |
| `catalog-2026.11.manifest.json` | Métadonnées + empreinte SHA-256 du corps |
| `catalog-2026.11.manifest.json.sig` | Signature Ed25519 **du manifeste** |
| `latest.json` | Pointeur vers la dernière version (petit, signé également) |

```json
{
  "version": "2026.11",
  "schemaVersion": 1,
  "builtAt": "2026-11-01T02:00:00Z",
  "source": "Catalogue général de la Bibliothèque nationale de France — Licence Ouverte Etalab 2.0",
  "sourceUpdatedAt": "2026-10-28",
  "counts": { "series": 4120, "albums": 34887 },
  "body": {
    "file": "catalog-2026.11.jsonl.zst",
    "bytes": 11238400,
    "sha256": "…"
  },
  "supersedes": "2026.10"
}
```

**Format du corps** (JSON Lines, une entité par ligne — lisible en flux, sans charger
35 000 albums en mémoire) :

```jsonl
{"t":"s","id":"thorgal","title":"Thorgal","publisher":"Le Lombard","status":"ONGOING","firstYear":1980,"tomeCount":41}
{"t":"a","ean":"9782803680122","seriesId":"thorgal","tome":40,"title":"Le Feu écarlate","published":"2024-11-15","pages":48}
{"t":"x","alias":"thorgal la jeunesse","seriesId":"thorgal-jeunesse"}
```

Contrat complet : [CONTRATS-DE-DONNEES.md](CONTRATS-DE-DONNEES.md).

### 4.5 Chaîne de vérification et d'import

Séquence obligatoire, dans cet ordre, sans exception :

1. Télécharger `latest.json`, vérifier sa signature. **Échec → arrêt silencieux.**
2. Comparer à la version installée. Identique → arrêt.
3. Télécharger le manifeste et sa signature, vérifier avec la clé publique embarquée.
   **Échec → arrêt, journalisation locale.**
4. Télécharger le corps dans `filesDir/catalog/tmp/`.
5. Calculer le SHA-256 du corps, comparer au manifeste. **Écart → suppression, arrêt.**
6. Importer dans `catalog_new.db` (base neuve, jamais la base en service).
7. Contrôles de vraisemblance : `albumCount` à ±5 % du manifeste, ≥ 1 000 séries,
   aucune table vide. **Échec → suppression, arrêt.**
8. **Bascule atomique** : fermer `catalog.db`, renommer `catalog_new.db` → `catalog.db`,
   rouvrir. Échec de renommage → conservation de l'ancienne.
9. Nettoyer le répertoire temporaire.

À aucun moment entre l'étape 1 et l'étape 8 le catalogue en service n'est dégradé.
C'est la même discipline d'écriture atomique que celle déjà appliquée aux couvertures
et aux sauvegardes dans le code existant — on l'étend, on ne l'invente pas.

**Clé publique** : constante `BuildConfig.CATALOG_PUBLIC_KEY`. Deux clés acceptées
simultanément pour permettre une rotation sans casse ([ADR-007](../DECISIONS.md#adr-007)).

### 4.6 Moteur de correspondance

Nouveau composant de domaine, en Kotlin pur (module `:core:domain`), donc testable sans
Android.

```kotlin
interface CatalogMatcher {
    /** EAN → album du catalogue. Exact uniquement, aucune approximation. */
    suspend fun byEan(ean: String): CatalogAlbum?

    /** Rattache une série de l'utilisateur à une série du catalogue. */
    suspend fun matchSeries(userSeries: Series): SeriesMatch?

    /** Liste complète des tomes connus d'une série du catalogue. */
    suspend fun tomesOf(catalogSeriesId: String): List<CatalogAlbum>
}

data class SeriesMatch(
    val catalogSeriesId: String,
    val confidence: Float,       // 0..1
    val matchedOn: MatchReason,  // EAN_D_UN_ALBUM | TITRE_EXACT | ALIAS | TITRE_APPROCHANT
)
```

**Algorithme de rattachement**, par ordre de priorité décroissante :

| Rang | Règle | Confiance |
|---|---|---|
| 1 | Un album de la série de l'utilisateur porte un EAN présent au catalogue | **1,00** |
| 2 | `normalizedTitle` identique | 0,95 |
| 3 | Correspondance d'alias | 0,90 |
| 4 | Distance de Levenshtein normalisée ≥ 0,88 **et** un seul candidat au-dessus du seuil | 0,75 |
| — | Sinon | aucun rattachement |

**Règles impératives**
- Rattachement automatique **seulement** au-dessus de 0,90. Entre 0,75 et 0,90 :
  proposition à confirmer par l'utilisateur, jamais d'application silencieuse.
- Un rattachement est **réversible** depuis la fiche série (« Ce n'est pas la bonne série »).
- La normalisation réutilise `TextNormalization` existant (insensible à la casse et aux
  accents), déjà testé. **Ne pas réimplémenter.**

### 4.7 Détection de trous, version catalogue

`GapDetector` est étendu **sans casser son comportement actuel** — il reste une fonction
pure sur une liste d'albums, plus une source optionnelle de tomes connus.

```kotlin
fun gaps(albums: List<Album>, catalogTomeCount: Int? = null): GapReport

data class GapReport(
    val internal: List<Int>,   // trous entre 1 et max(possédés) — sémantique actuelle
    val ahead: List<Int>,      // tomes parus au-delà de max(possédés) — nouveau
)
```

**Pourquoi deux listes.** Un trou interne (« j'ai le 1 à 13 sauf le 7 ») et un retard
(« il en existe 22, j'en ai 13 ») ne sont pas la même information et n'appellent pas la
même action. Les confondre en un seul nombre affaiblirait l'écran le plus utile de
l'application. `catalogTomeCount = null` restitue exactement le comportement actuel :
la compatibilité est structurelle, pas promise.

### 4.8 Sous-ensemble embarqué

`assets/catalog_starter.jsonl.zst` — les **400 séries franco-belges les plus diffusées**,
soit environ 6 000 albums, ≈ 2 Mo compressés.

- Importé au premier lancement, en tâche de fond, sans bloquer l'onboarding.
- Permet le pilier P2 dès la première seconde, sans réseau.
- Signé et vérifié comme un bundle distant : **aucune exception au chemin de confiance**.
- Régénéré à chaque release applicative.

### 4.9 Alimentation de « À paraître »

La Moitié B ne disparaît pas — elle change de source.

- Les sorties sont **dérivées** du catalogue : albums dont `publishedDate` est dans les
  6 mois à venir, appartenant à une série suivie par l'utilisateur.
- Le fichier `releases.json` reste supporté en surcouche pour l'annonce manuelle d'un
  événement (album annoncé mais pas encore au dépôt légal). Il devient **facultatif**.
- La règle « aucune sortie stockée dans la base de la collection » reste absolue :
  la dérivation se fait en mémoire, à la lecture, exactement comme
  `buildShoppingList()` procède aujourd'hui.

### 4.10 Comportements en erreur (exigence DoR n°5)

| Situation | Comportement |
|---|---|
| Aucun réseau à la mise à jour | Silencieux. Le catalogue en place continue de servir. |
| Réseau coupé à 80 % du téléchargement | Fichier temporaire supprimé, catalogue intact, nouvelle tentative à la prochaine occasion. Pas de reprise partielle. |
| Disque plein | Import annulé, temporaires purgés, message non bloquant dans Réglages. |
| Signature invalide | Rejet, catalogue conservé, entrée dans le journal local, **aucun envoi**. |
| Corps corrompu (SHA-256 différent) | Idem. |
| Bundle vraisemblablement tronqué (< 1 000 séries) | Rejet par les contrôles de vraisemblance. |
| Application tuée pendant l'import | La base en service est intacte (import dans une base neuve). Les temporaires sont purgés au démarrage suivant. |
| `catalog.db` corrompue | Détectée à l'ouverture → suppression → repli sur le sous-ensemble embarqué → retéléchargement proposé. **Jamais** de plantage. |
| Catalogue absent | Toutes les fonctions catalogue se dégradent au comportement v1. Aucun écran ne devient inaccessible. |

---

## 5. Interface & accessibilité

### 5.1 Verdict enrichi (écran existant, `VerdictScreen`)

L'état « Inconnu » gagne un bloc d'identification alimenté **localement** :

```
┌──────────────────────────────────────┐
│  Nouveau code-barres                 │
│                                      │
│  ┌────┐  Thorgal                     │
│  │ 📕 │  Tome 40 — Le Feu écarlate   │
│  └────┘  Le Lombard · 2024           │
│                                      │
│  Cette série est dans ta collection. │
│  Il te manquait ce tome.             │
│                                      │
│  [   Je viens de l'acheter   ]       │
│  [   Ce n'est pas ça          ]      │
└──────────────────────────────────────┘
```

- L'identification vient du catalogue local : **aucune attente réseau**, plus de bloc
  qui apparaît après coup.
- « Ce n'est pas ça » revient toujours à la recherche manuelle actuelle. Le catalogue
  propose, il ne décide pas.
- La couverture reste opt-in ([ADR-006](../DECISIONS.md#adr-006)) et ne bloque jamais
  l'affichage.

### 5.2 Étagère — les tomes en retard

Trois états visuels au lieu de deux :

| État | Rendu | Description vocale |
|---|---|---|
| Possédé | tranche pleine, couleur de série | « Tome 7, Les Trois Vieillards, possédé » |
| Manquant (trou interne) | contour pointillé `ghost` | « Tome 7, manquant » |
| **Paru depuis (nouveau)** | contour pointillé + liseré `accent` en pied | « Tome 20, paru, pas encore dans ta collection » |

**Exigences d'accessibilité**
- Les trois états sont distinguables **sans la couleur** (plein / pointillé / pointillé + liseré).
- Contraste ≥ 4,5:1 pour tout texte porteur de sens, en thème clair **et** sombre.
- Description vocale complète, en toutes lettres — pas de « T7 ✓ ».
- La [vue liste équivalente](../DECISIONS.md#adr-009) affiche les trois états en mots.

### 5.3 Réglages → « Le catalogue »

```
Le catalogue
  Version installée      2026.11 (4 120 séries · 34 887 albums)
  Mise à jour            automatique en Wi-Fi        [●]
  [ Mettre à jour maintenant ]
  Espace utilisé         48 Mo
  [ Supprimer le catalogue ]

  Source : Catalogue général de la Bibliothèque nationale de France,
  mis à jour le 28 octobre 2026. Licence Ouverte Etalab 2.0.
```

- Langage sans jargon : « le catalogue », jamais « référentiel », « bundle » ou « base ».
- La mention de source est **obligatoire** (Etalab 2.0, [ADR-004](../DECISIONS.md#adr-004))
  et non masquable.
- « Supprimer le catalogue » : confirmation explicite, et texte rassurant — *« Ta
  collection ne sera pas touchée. »* C'est la question que l'utilisateur va se poser.

---

## 6. Plan de test

### Tests unitaires (`:core:domain`, JVM)
- `CatalogMatcherTest` — les 4 règles de rattachement, seuils, cas ambigus, aucun
  rattachement sous 0,75.
- `GapDetectorTest` **étendu** — `catalogTomeCount = null` produit exactement le résultat
  actuel (non-régression stricte, cas par cas sur les fixtures existantes).
- `BundleParserTest` — JSONL valide, ligne corrompue au milieu, type inconnu ignoré,
  fichier vide, fichier tronqué.
- `ManifestVerifierTest` — signature valide, invalide, absente, clé inconnue, SHA-256 discordant.
- `CatalogPlausibilityTest` — contrôles de vraisemblance, bornes hautes et basses.

### Tests instrumentés (`androidTest`)
- `CatalogImportTest` — import complet, bascule atomique, mort du processus pendant
  l'import (l'ancienne base survit).
- `CatalogMigrationTest` — migrations 1→2→3 sur base peuplée, aucune perte.
- `OfflineVerdictTest` — mode avion, scan d'un EAN du catalogue, verdict complet < 1,5 s.
- `CorruptedCatalogTest` — `catalog.db` volontairement corrompue → repli propre, zéro plantage.

### Tests de terrain
- **Corpus de 500 EAN réels** (BOUSSOLE, collectés en librairie) → taux de résolution
  mesuré. **Seuil d'acceptation : 90 %. Seuil d'alerte : 85 %. Clause d'arrêt : 75 %.**
- 30 séries du panel rattachées manuellement à la main → vérification du taux de
  rattachement automatique correct (cible ≥ 95 %, **faux rattachement toléré : 0**).

### Performance (banc CI, Galaxy A14)
- Résolution EAN hors-ligne : p95 < 200 ms.
- Recherche de série dans 35 000 albums : < 150 ms.
- Import de bundle complet : < 90 s, en tâche de fond, sans blocage de l'interface.
- Taille installée de `catalog.db` : < 60 Mo.

---

## 7. Risques & questions ouvertes

| # | Sujet | Titulaire | Traitement |
|---|---|---|---|
| Q1 | **Le taux de couverture réel de la BnF est inconnu.** Le dépôt légal est exhaustif en droit, incomplet en pratique sur les métadonnées de série et de tome — le point faible connu de l'UNIMARC pour la BD. | SOURCE | Mesure au sprint 2 sur 500 EAN. **Clause d'arrêt à 75 %** avant engagement des 24 sprints. |
| Q2 | **La normalisation des séries est le vrai point dur.** « Thorgal », « Thorgal — La Jeunesse », « Les Mondes de Thorgal », intégrales, rééditions, tirages de tête. Une mauvaise normalisation produit des trous fantômes — pire que pas de catalogue. | SOURCE | Règles documentées, table d'alias, revue manuelle des 200 séries les plus diffusées. Aucune fusion automatique en dessous de 0,90. |
| Q3 | Poids du bundle si la couverture est meilleure qu'espéré | CHARPENTE | Budget dur 15 Mo. Au-delà : découpage par période de parution, le récent d'abord. |
| Q4 | Hébergement du bundle | FILET | GitHub Releases en H1 (gratuit, durable, versionné). Stockage objet si le volume l'exige. Aucune dépendance à un service payant (P5). |
| Q5 | Gestion de la clé privée de signature | FILET + GARDE-FOU | Secret de CI, rotation annuelle, deux clés acceptées en parallèle. Procédure de compromission écrite. |

---

## 8. Ce qui prouve que c'est terminé

- [ ] Un téléphone en mode avion, catalogue installé, identifie un album jamais vu en
      moins de 1,5 s.
- [ ] Une série dont on possède 13 tomes sur 22 affiche 9 tomes parus manquants.
- [ ] Un bundle dont on a modifié un octet est refusé, et le catalogue précédent survit.
- [ ] Tuer l'application pendant un import laisse une base parfaitement utilisable.
- [ ] Taux de résolution ≥ 90 % sur le corpus de 500 EAN.
- [ ] **Zéro requête réseau** émise lors d'un scan, vérifié par capture réseau.
- [ ] Supprimer le catalogue ne touche pas un seul album de la collection.
- [ ] La mention Etalab est affichée dans À propos, avec la date de la source.
</content>

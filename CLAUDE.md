# CLAUDE.md — BDShelf

> **Lis ce fichier au début de chaque session, avant d'écrire la moindre ligne de code.**
> Ce fichier = état courant + référence rapide. SPEC.md = rationale complet.
> Principe : si la réponse est ici, ne pas aller dans SPEC.md. Aller dans SPEC.md seulement pour les décisions de conception profondes.

---

## PROJECT

| | |
|---|---|
| App name | **BDShelf** |
| Package | `com.bdshelf.app` |
| minSdk | 26 |
| targetSdk / compileSdk | 35 |
| Langue UI | **Français** (toutes les strings visibles par l'utilisateur) |
| Unique utilisateur | non technicien, âgé → accessibilité non négociable |
| Cloud / compte | **AUCUN**. Aucune authentification. Aucun telemetry. |

---

## CURRENT PHASE ← **METTRE À JOUR ICI À CHAQUE FIN DE PHASE**

```
TERMINÉ → Phase 5 : Finition (toutes phases implémentées, voir notes de compilation ci-dessous)
```

| Phase | Description | Statut |
|---|---|---|
| 0 | Socle — gradle, thème, polices, nav squelette | **DONE** |
| 1 | Données — Room, seed import | **DONE** |
| 2 | Cœur consultation — séries, étagère, tomes (MVP livrable) | **DONE** |
| 3 | Scan — CameraX + ML Kit + Verdict | **DONE** |
| 4 | Sorties — fetch releases.json, cache, écran À paraître | **DONE** |
| 5 | Finition — WorkManager, notifications, export/import, a11y, animations | **DONE** |

> **Note** : l'environnement de build de cette session n'a pas accès au SDK Android
> (`dl.google.com` bloqué), donc `./gradlew assembleDebug` n'a pas pu être exécuté
> pour vérifier la compilation complète. Le code a été relu manuellement
> (imports, signatures, requêtes DAO) ; un import manquant
> (`kotlinx.serialization.encodeToString`) a été trouvé et corrigé. À vérifier
> avec un vrai build dès que possible.
>
> **Hors scope** : l'enrichissement optionnel via Google Books (§6.4, "dégradation
> silencieuse si hors-ligne ou sans résultat") n'a pas été implémenté — le verdict
> "Inconnu" fonctionne sans cet enrichissement.

### Checklist Phase 0
- [x] `build.gradle.kts` (app + projet) avec toutes les dépendances §STACK
- [x] `BdShelfTheme` — tous les jetons couleur + typographie
- [x] Polices `fraunces_*.ttf` + `atkinson_hyperlegible_*.ttf` dans `res/font/`
- [x] `NavGraph.kt` — toutes les routes déclarées
- [x] `MainActivity.kt` — single activity, `BdShelfTheme`, `NavHost`
- [ ] Compile et lance sans crash (non vérifiable dans cet environnement, voir note ci-dessus)

### Checklist Phase 1
- [x] `Series`, `Album`, enums `SeriesStatus`, `ReadStatus` (Room entities exactes §SCHEMA)
- [x] `SeriesDao`, `AlbumDao` (CRUD + queries §DAO_QUERIES)
- [x] `AppDatabase` (version 1, exportSchema true)
- [x] `SeedImporter` — lit `assets/seed-collection.json`, règles §SEED_RULES
- [x] `CollectionRepository` (suspend funs + Flows)
- [x] Prefs DataStore : clé `owner_name` (String), `seed_imported` (Boolean)
- [x] Premier lancement : écran Onboarding → saisie prénom → import seed → Home
- [x] Vérifiable : écran SeriesList affiche les séries depuis Room

### Checklist Phase 2
- [x] `SeriesListScreen` — liste alphabétique + recherche insensible accents
- [x] `SeriesDetailScreen` — étagère `SpineTile` horizontale + en-tête stats
- [x] `SpineTile` composable — états owned/missing/read/lent (§SPINE)
- [x] `AlbumFormScreen` — ajout/édition album, validation doublon tomeNumber
- [x] Toggle owned → animation tampon (§ANIMATION)
- [ ] APK debug installable et fonctionnel sans réseau (non vérifiable dans cet environnement)

### Checklist Phase 3
- [x] `ScannerScreen` — CameraX + ML Kit, gestion permission caméra
- [x] `VerdictScreen` — 3 états (possédé / manquant / inconnu), liaison EAN→album
- [x] Création d'album depuis un verdict inconnu, EAN pré-rempli

### Checklist Phase 4
- [x] `ReleasesApi` + `ReleasesRepository` — cache JSON dans `filesDir`, jamais dans Room
- [x] `ReleasesScreen` — sections « À venir » / « Déjà paru », badges possédé/manquant
- [x] Bouton « Mettre à jour les nouveautés », fonctionne hors-ligne depuis le cache

### Checklist Phase 5
- [x] `ReleasesSyncWorker` — WorkManager périodique (1×/jour, réseau + batterie), notifications locales (§7)
- [x] `SettingsScreen` — prénom, notifications (+ permission POST_NOTIFICATIONS), URL avancée
- [x] Export JSON (sauvegarde) + CSV (lisible), import JSON via sélecteur de fichiers
- [x] À propos / dédicace
- [x] Animations tampon respectant `LocalReduceMotion`

---

## STACK

```kotlin
// Version BOM Compose
implementation(platform("androidx.compose:compose-bom:2025.05.00"))

// Core
androidx.compose.ui, material3, activity-compose, navigation-compose:2.8.x
androidx.room:room-runtime:2.7.x + room-ktx + ksp room-compiler
androidx.datastore:datastore-preferences:1.1.x
androidx.work:work-runtime-ktx:2.10.x
org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.x

// Caméra & scan
androidx.camera:camera-camera2:1.4.x + camera-lifecycle + camera-view
com.google.mlkit:barcode-scanning:17.3.x   // modèle embarqué, offline

// Polices (embarquées, pas Google Fonts)
// res/font/fraunces_*.ttf  + res/font/atkinson_hyperlegible_*.ttf

// KSP (pas kapt)
com.google.devtools.ksp:<version>
```

**Pas de Hilt en v1.** Injection manuelle dans `Application.kt` (`AppDatabase`, `Repositories`).
Pas de Retrofit. Pas de Coil (pas de couvertures). Pas de Dagger.

---

## ARCHITECTURE

Deux règles, absolues :

1. **Collection (Room) ≠ Sorties (remote JSON).** Ne jamais passer une entité Room à un composant Releases. Ne jamais stocker une release dans Room. Le cache releases = fichier JSON local dans `filesDir`.
2. **ViewModel → Repository → (Dao | Api).** Jamais de Dao direct dans un Screen. Jamais de logique métier dans un Screen.

Pattern par feature : `Screen.kt` + `ViewModel.kt` (StateFlow<UiState>) + appel via `CollectionRepository` ou `ReleasesRepository`.

---

## SCHEMA

### entities (Room)

```
Series
  id            : String   PK  — slug stable ex: "buck-danny-classic"
  title         : String
  status        : SeriesStatus  [ONGOING | FINISHED | UNKNOWN]
  isTracked     : Boolean   — alimente l'écran À paraître
  color         : Long      — ARGB, auto-assigné §SPINE_COLORS
  knownTomeCount: Int?      — null si série ouverte
  notes         : String?

Album
  id            : String   PK  — ex: "buck-danny-classic-7"
  seriesId      : String   FK→Series (CASCADE DELETE)
  tomeNumber    : Int?      — null = hors-série
  title         : String?
  owned         : Boolean   — cœur de l'app
  readStatus    : ReadStatus [UNREAD | READ | LENT]
  edition       : String?   — "souple" | "intégrale" | "collector"
  ean           : String?   UNIQUE nullable — appris au scan
  dateAdded     : Long      — epoch millis
```

Index Room : `Album(seriesId)`, `Album(ean)` unique.
`owned=false` = trou connu. Les trous existent en base — c'est ce qui affiche les vides sur l'étagère.

### DAO_QUERIES (à implémenter exactement)

```kotlin
// SeriesDao
@Query("SELECT * FROM series ORDER BY title ASC") fun allSeries(): Flow<List<Series>>
@Query("SELECT * FROM series WHERE title LIKE '%' || :q || '%'") fun search(q: String): Flow<List<Series>>
@Query("SELECT * FROM series WHERE id = :id") suspend fun byId(id: String): Series?

// AlbumDao
@Query("SELECT * FROM albums WHERE seriesId = :sid ORDER BY tomeNumber ASC") fun forSeries(sid: String): Flow<List<Album>>
@Query("SELECT * FROM albums WHERE ean = :ean LIMIT 1") suspend fun byEan(ean: String): Album?
@Query("SELECT COUNT(*) FROM albums WHERE seriesId = :sid AND owned = 1") suspend fun ownedCount(sid: String): Int
@Query("SELECT COUNT(*) FROM albums WHERE seriesId = :sid AND owned = 0") suspend fun missingCount(sid: String): Int
@Query("SELECT * FROM albums WHERE seriesId = :sid AND tomeNumber = :n LIMIT 1") suspend fun bySeriesAndTome(sid: String, n: Int): Album?
```

---

## THEME TOKENS

Fichier : `ui/theme/Color.kt` + `ui/theme/Type.kt` + `ui/theme/Theme.kt`

### Couleurs

```kotlin
val Paper      = Color(0xFFF5EFE2)  // fond général
val Ink        = Color(0xFF1C1A17)  // texte principal
val InkSoft    = Color(0xFF5A534A)  // texte secondaire
val Accent     = Color(0xFFC0392B)  // rouge BD — CTA uniquement
val OwnedGreen = Color(0xFF2E7D54)  // verdict "possédé"
val Surface    = Color(0xFFFFFDF7)  // cartes
val Ghost      = Color(0xFFC9C1B2)  // tranches manquantes
```

MaterialTheme mapping :
- `background` → Paper
- `surface` → Surface
- `primary` → Accent
- `onBackground` / `onSurface` → Ink
- `outline` → Ghost

Mode sombre : **non implémenté en v1.** `dynamicColor = false`, `darkTheme = false` dans `BdShelfTheme`.

### Typographie

```kotlin
// Fraunces = display / titres écran
displayLarge   = 34.sp  Fraunces   // wordmark
titleLarge     = 28.sp  Fraunces   // titres d'écran
titleMedium    = 22.sp  Fraunces   // numéro de tome sur tranche

// Atkinson Hyperlegible = tout le reste
titleSmall     = 20.sp  AtkinsonHyperlegible  // titre de série liste
bodyLarge      = 18.sp  AtkinsonHyperlegible  // corps de texte
bodyMedium     = 16.sp  AtkinsonHyperlegible
labelMedium    = 15.sp  AtkinsonHyperlegible  // légendes, badges
```

**Respecter `fontScale` système.** Ne jamais utiliser `.sp` avec `.nonScaledSp` ou équivalent. Les layouts doivent tenir à fontScale 1.5×.

---

## SPINE COLORS (palette auto-assignée)

```kotlin
val SpinePalette = listOf(
    Color(0xFF7B6B8D), // violet poussiéreux
    Color(0xFF5B8A6F), // vert sauge
    Color(0xFF8B6B3D), // noyer
    Color(0xFF4A7A9B), // bleu acier
    Color(0xFF9B7B4A), // ambre
    Color(0xFF6B8B7B), // teal gris
    Color(0xFF8B5B6B), // mauve
    Color(0xFF5B7B5B), // mousse
    Color(0xFF9B6B5B), // terracotta
    Color(0xFF5B6B9B), // ardoise bleue
    Color(0xFF7B8B5B), // olive
    Color(0xFF6B5B8B), // violet gris
)

fun seriesSpineColor(seriesId: String): Color =
    SpinePalette[abs(seriesId.hashCode()) % SpinePalette.size]
```

Ne jamais utiliser `Accent` (#C0392B) comme couleur de tranche.

---

## SPINE (composable SpineTile)

```
Largeur : 52.dp fixe
Hauteur : entre 160.dp et 180.dp (légère variation par série pour l'effet rayonnage)

État OWNED  : fond = seriesColor (saturé), numéro Fraunces 22sp blanc
État MISSING: fond = Paper, border pointillé 1.5dp Ghost, numéro InkSoft estompé (alpha 0.4)
Marqueur bas: point plein Ink = READ | anneau Ink = LENT | rien = UNREAD
```

L'étagère = `LazyRow` de `SpineTile`, triée par `tomeNumber ASC`, nulls en fin.

---

## NAVIGATION ROUTES

```kotlin
object Routes {
    const val ONBOARDING   = "onboarding"
    const val HOME         = "home"
    const val SCANNER      = "scanner"
    const val VERDICT      = "verdict/{ean}"           // ean = scanned value
    const val SERIES_LIST  = "series_list"
    const val SERIES_DETAIL= "series_detail/{seriesId}"
    const val ALBUM_FORM   = "album_form/{seriesId}?albumId={albumId}"
    const val SERIES_FORM  = "series_form?seriesId={seriesId}"
    const val RELEASES     = "releases"
    const val SETTINGS     = "settings"
}
```

Logique de démarrage dans `MainActivity` : si `seed_imported == false` → `startDestination = ONBOARDING`, sinon `HOME`.

---

## FILE MAP

```
app/src/main/
  java/com/bdshelf/app/
    BdShelfApplication.kt     // AppDatabase, Repositories (injection manuelle)
    MainActivity.kt
    NavGraph.kt
    data/
      local/
        AppDatabase.kt
        entities/Series.kt, Album.kt, Enums.kt
        dao/SeriesDao.kt, AlbumDao.kt
      remote/
        ReleasesApi.kt        // fetchReleases(): suspend → ReleasesDocument
        ReleasesDto.kt        // @Serializable data classes (miroir §JSON_SCHEMAS)
      seed/
        SeedImporter.kt       // règles §SEED_RULES
        SeedDto.kt            // @Serializable
      repo/
        CollectionRepository.kt
        ReleasesRepository.kt // cache dans filesDir/releases_cache.json
    domain/
      SpineColor.kt           // seriesSpineColor()
      GapDetector.kt          // gaps(albums: List<Album>): List<Int>
      CollectionStats.kt      // data class CollectionStats(series, owned, missing)
    ui/
      theme/Color.kt, Type.kt, Theme.kt
      components/
        SpineTile.kt
        Shelf.kt              // LazyRow de SpineTile
        BigScanButton.kt
        StampAnimation.kt
      onboarding/  OnboardingScreen.kt, OnboardingViewModel.kt
      home/        HomeScreen.kt, HomeViewModel.kt
      scanner/     ScannerScreen.kt, ScannerViewModel.kt
      verdict/     VerdictScreen.kt, VerdictViewModel.kt
      series/      SeriesListScreen.kt, SeriesListViewModel.kt
      seriesdetail/SeriesDetailScreen.kt, SeriesDetailViewModel.kt
      albumform/   AlbumFormScreen.kt, AlbumFormViewModel.kt
      releases/    ReleasesScreen.kt, ReleasesViewModel.kt
      settings/    SettingsScreen.kt, SettingsViewModel.kt
    work/
      ReleasesSyncWorker.kt
  res/
    font/          fraunces_*.ttf, atkinson_hyperlegible_*.ttf
    values/        strings.xml (FR), themes.xml (vide — tout dans Compose)
  assets/
    seed-collection.json
    releases_default.json    // fallback vide si pas encore de cache réseau
```

---

## SEED_RULES (SeedImporter)

Exécuté **une seule fois** (guard : DataStore `seed_imported`). Idempotent si relancé (upsert).

```
Pour chaque série dans seed-collection.json :
  1. Insérer Series avec color = seriesSpineColor(id)
  2. Pour chaque n dans ownedTomes : insérer Album(id="${seriesId}-${n}", owned=true, readStatus=UNREAD)
  3. Si knownTomeCount != null et knownTomeCount > max(ownedTomes) :
       Pour n de (max(ownedTomes)+1) jusqu'à knownTomeCount :
         insérer Album(id="${seriesId}-${n}", owned=false) ← trous visibles
  4. Détecter les trous internes : pour chaque n manquant entre 1 et max(ownedTomes) :
       insérer Album(id="${seriesId}-${n}", owned=false)
  5. Marquer seed_imported = true
```

---

## JSON_SCHEMAS (compact)

### seed-collection.json (assets)
```json
{ "version": 1, "series": [
  { "id": "buck-danny-classic", "title": "Buck Danny Classic",
    "status": "ONGOING", "tracked": true, "knownTomeCount": null,
    "ownedTomes": [1,2,3,4,5,6,7,8,9,10,11,12,13], "notes": "" }
]}
```

### releases.json (remote)
```json
{ "version": 1, "updatedAt": "2026-06-12", "releases": [
  { "seriesId": "wunderwaffen", "seriesTitle": "Wunderwaffen",
    "tomeNumber": 13, "title": "Le Dernier Recours",
    "expectedDate": "2026-09-18", "status": "UPCOMING", "note": "" }
]}
```
`status` : `UPCOMING` | `RELEASED`
URL source : constante `BuildConfig.RELEASES_URL` (définie dans `build.gradle.kts` `buildConfigField`).

---

## ANIMATION (StampAnimation)

Au passage `owned = false → true` sur un `SpineTile` :
- Scale: 1.0 → 1.15 → 1.0 sur 300ms, easing `FastOutSlowIn`
- Simultané : alpha de la bordure Ghost → 0
- Respecter `LocalReduceMotion` : si true, skip l'animation, appliquer l'état final directement

---

## CODING CONVENTIONS

- Fichier Kotlin : 1 classe publique principale par fichier, nom = classe
- `UiState` : sealed class ou data class dans le ViewModel, jamais d'état mutable dans le Screen
- Flows Room → `collectAsStateWithLifecycle()` (pas `collectAsState()`)
- Strings UI : **toutes dans `strings.xml`**, jamais hardcodées dans le Compose (même les mots courts)
- Nomenclature strings.xml : `screen_element_description` ex: `home_scan_button`, `verdict_owned_label`
- IDs Album : toujours `"${seriesId}-${tomeNumber}"`. Hors-série : `"${seriesId}-hs-${index}"`
- Pas de `Thread.sleep`, pas de `runBlocking` dans le code UI
- Chaque `ViewModel` expose un seul `StateFlow<XxxUiState>` + des fonctions `onXxx()`
- Taille tactile minimum : `Modifier.defaultMinSize(minHeight = 56.dp)` sur tout composant interactif

---

## NEVER

Ces décisions sont arrêtées. Ne pas questionner, ne pas proposer d'alternative.

- **Pas de couvertures** (téléchargées ou cachées). Zéro image réseau.
- **Pas de scraping** Bédéthèque, BDGest, Amazon ou tout autre site.
- **Pas de Room pour les releases**. Le cache releases = fichier JSON dans `context.filesDir`.
- **Pas de compte / Firebase / Analytics / Crashlytics / tout SDK Google sauf ML Kit + CameraX**.
- **Pas de mode sombre** en v1. `darkTheme` forcé `false`.
- **Pas de `dynamicColor = true`**. Les jetons de couleur ci-dessus sont absolus.
- **Pas de Hilt**. Injection manuelle dans `BdShelfApplication`.
- **Pas de multi-module**. Un seul module `app`.
- **Pas d'écran hors Phase courante**. Implémenter uniquement ce que la checklist active liste.
- **Pas de `TODO` silencieux**. Un placeholder d'écran = `Box(Modifier.fillMaxSize()) { Text("TODO: NomEcran") }` explicite.

---

## ACCESSIBILITY (non négociable, toutes phases)

- `contentDescription` non null sur **tout** `Icon`, `Image`, `IconButton`, `FloatingActionButton`
- Cibles tactiles ≥ 56.dp (hauteur). Bouton scan Home ≥ 120.dp hauteur.
- Jamais `hardcoded` color pour du texte porteur de sens — utiliser les tokens
- Test mental fontScale 1.5× : les textes ne doivent pas se tronquer ni se superposer
- Libellés en langue claire : « code-barres » (pas EAN), « mettre à jour les nouveautés » (pas synchroniser)

---

## COMMANDS

```bash
# Build debug
./gradlew assembleDebug

# Install sur device connecté
./gradlew installDebug

# Tests unitaires
./gradlew test

# Tests instrumentés
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Nettoyer
./gradlew clean
```

APK de sortie : `app/build/outputs/apk/debug/app-debug.apk`

---

## DEEP SPEC

Consulter `SPEC.md` uniquement pour :
- Rationale d'une décision d'architecture (§1–§3)
- Comportement détaillé d'un écran spécifique (§6.x)
- Critères d'acceptation complets d'une phase (§9)
- Définition de terminé v1 (§10)

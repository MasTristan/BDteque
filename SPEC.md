# SPEC v1 — Application Android de gestion d'une collection de bandes dessinées

> Document destiné à Claude Code. Il décrit le périmètre, l'architecture, le modèle de données, le design et l'ordre de construction de la v1. À placer à la racine du repo. Tout ce qui n'est pas explicitement listé en « hors périmètre » est attendu.

---

## 1. Contexte et intention

Application **personnelle**, offerte par un fils à son père. Le destinataire est un lecteur **âgé**, non technicien. La valeur n'est pas de concurrencer les apps existantes (Bubble, BDGest) mais d'offrir un outil **simple, robuste, durable et chaleureux**, taillé pour *sa* collection.

Deux usages réels priment :

1. **En librairie** : « est-ce que je possède déjà ce tome ? » et « qu'est-ce qu'il me manque dans cette série ? » — réponse en moins de 3 secondes, **y compris sans réseau**.
2. **À la maison** : « quelles nouveautés sont sorties ou à paraître dans les séries que je suis ? »

### Principes directeurs (non négociables)

- **Offline-first absolu.** La consultation de la collection ne dépend JAMAIS du réseau. Toute la donnée de collection vit en local (Room). Le réseau ne sert qu'à rafraîchir le flux des sorties, et son absence ne casse rien.
- **Aucun compte, aucune authentification, aucun cloud.** Premier lancement = utilisable immédiatement.
- **Sobriété cognitive.** Profondeur de navigation maximale : 2 niveaux. Le geste central de l'app accueil est le scan.
- **Lisibilité avant esthétique**, mais esthétique forte et personnelle quand elle ne nuit pas à la lisibilité.

---

## 2. Pile technique imposée

| Domaine | Choix | Notes |
|---|---|---|
| Langage | **Kotlin** | |
| UI | **Jetpack Compose + Material 3** | thème **entièrement personnalisé** (voir §5), pas le violet par défaut |
| Persistance | **Room** (SQLite) | source de vérité de la collection |
| Scan code-barres | **ML Kit Barcode Scanning** (`com.google.mlkit:barcode-scanning`, modèle embarqué) | fonctionne hors-ligne, formats EAN-13 / EAN-8 / ISBN |
| Caméra | **CameraX** | preview + analyse |
| Sérialisation | **kotlinx.serialization** | lecture des fichiers JSON (seed + sorties) |
| Préférences | **DataStore (Preferences)** | nom du père, réglages |
| Tâches de fond | **WorkManager** | rafraîchissement périodique des sorties + notification |
| Navigation | **Navigation Compose** | |
| Polices | **Atkinson Hyperlegible** (corps) + **Fraunces** (titres) | embarquées en `res/font`, voir §5 |

- `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`.
- Architecture **MVVM + Repository**, module unique, pas de sur-ingénierie. Injection manuelle ou Hilt léger — Hilt acceptable mais non requis.
- Pas de dépendance réseau lourde : un simple client HTTP (Ktor client ou `HttpURLConnection`) suffit pour récupérer un fichier JSON distant.

### Hors périmètre v1 (à ne PAS implémenter)

- Lecture de BD numériques.
- Visuels de couvertures téléchargés (remplacés par le système de tranches, §5.4).
- Synchronisation multi-appareils / comptes.
- Achat / e-commerce / liens libraires.
- Scraping de Bédéthèque ou de tout site (interdit : fragile et contraire aux CGU). Les sorties proviennent **exclusivement** d'un fichier JSON curé fourni (§4.3).

---

## 3. Architecture des données : deux moitiés séparées

C'est le point structurant. Ne pas les coupler.

**Moitié A — la collection (locale, autonome, pérenne).**
Tout ce que le père possède. Vit dans Room. Aucune dépendance externe. Doit fonctionner sans modification pendant des années.

**Moitié B — les sorties (donnée curée, rafraîchissable).**
Un fichier `releases.json` maintenu *par le fils* (hébergé sur une URL qu'il contrôle : fichier brut GitHub ou Google Sheet publié en JSON). L'app **lit** ce fichier, le met en cache local, et ne l'interroge jamais en dur. Hors-ligne, elle utilise le dernier cache.

Conséquence : l'app n'« interroge » aucune base BD. Le travail de veille est humain (assisté par IA côté fils) et se matérialise par la mise à jour d'un fichier. C'est volontaire : rythme de parution lent (1–2 tomes/série/an), périmètre réduit (~35 séries), zéro fragilité technique.

---

## 4. Modèle de données

### 4.1 Entités Room

```kotlin
// Une série suivie ou possédée
@Entity(tableName = "series")
data class Series(
    @PrimaryKey val id: String,        // slug stable, ex: "buck-danny-classic"
    val title: String,                 // "Buck Danny Classic"
    val status: SeriesStatus,          // ONGOING, FINISHED, UNKNOWN
    val isTracked: Boolean,            // alimente l'écran "À paraître"
    val color: Long,                   // couleur de tranche (ARGB) — voir §5.4
    val knownTomeCount: Int?,          // nb total connu de tomes, si fini (nullable)
    val notes: String?                 // libre
)

enum class SeriesStatus { ONGOING, FINISHED, UNKNOWN }

// Un album / tome
@Entity(
    tableName = "albums",
    foreignKeys = [ForeignKey(
        entity = Series::class, parentColumns = ["id"],
        childColumns = ["seriesId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("seriesId"), Index(value = ["ean"], unique = true)]
)
data class Album(
    @PrimaryKey val id: String,        // ex: "buck-danny-classic-7"
    val seriesId: String,
    val tomeNumber: Int?,              // null possible (hors-série)
    val title: String?,               // titre du tome, optionnel
    val owned: Boolean,               // possédé ou non (le cœur)
    val readStatus: ReadStatus,       // UNREAD, READ, LENT
    val edition: String?,             // "souple", "intégrale", "collector"...
    val ean: String?,                 // EAN appris au scan, unique, nullable
    val dateAdded: Long               // epoch millis
)

enum class ReadStatus { UNREAD, READ, LENT }
```

> Note : `owned` est délibérément un booléen sur l'album et non une liste séparée. Un album « connu mais non possédé » (un trou de série) existe en base avec `owned = false`. C'est ce qui permet d'afficher les vides sur l'étagère.

### 4.2 Schéma du fichier d'amorçage `seed-collection.json`

Fourni séparément (généré à partir de la liste réelle du père). Importé au **premier lancement uniquement**, puis ignoré.

```json
{
  "version": 1,
  "series": [
    {
      "id": "buck-danny-classic",
      "title": "Buck Danny Classic",
      "status": "ONGOING",
      "tracked": true,
      "knownTomeCount": null,
      "ownedTomes": [1,2,3,4,5,6,7,8,9,10,11,12,13],
      "notes": ""
    }
  ]
}
```

Règle d'import : pour chaque série, créer la `Series`, puis créer un `Album owned=true` pour chaque entier de `ownedTomes`. Si `knownTomeCount` > max(ownedTomes), créer les tomes manquants intermédiaires en `owned=false` (ce sont les trous). La couleur de série est attribuée automatiquement (§5.4) si non fournie.

### 4.3 Schéma du fichier distant `releases.json`

```json
{
  "version": 1,
  "updatedAt": "2026-06-12",
  "releases": [
    {
      "seriesId": "wunderwaffen",
      "seriesTitle": "Wunderwaffen",
      "tomeNumber": 13,
      "title": "Le Dernier Recours",
      "expectedDate": "2026-09-18",
      "status": "UPCOMING",
      "note": ""
    }
  ]
}
```

- `status` : `UPCOMING` (à paraître) ou `RELEASED` (déjà paru).
- À l'ingestion : croiser chaque release avec la collection. Si `owned=false` (ou tome absent) pour une série **trackée**, c'est une nouveauté à signaler.
- L'URL du fichier est une constante de build (`BuildConfig.RELEASES_URL`), surchargeable dans les réglages (champ masqué/avancé).

---

## 5. Direction UX / UI — « L'étagère »

L'app doit ressembler à une bibliothèque chaleureuse en papier et encre, pas à une base de données. Métaphore centrale : **une étagère où les tomes sont des tranches de livres, et où un trou de collection est un vide visible.**

### 5.1 Jetons de couleur (thème clair uniquement en v1)

| Rôle | Hex | Usage |
|---|---|---|
| `paper` (fond) | `#F5EFE2` | fond papier chaud |
| `ink` (texte) | `#1C1A17` | texte principal, fort contraste |
| `inkSoft` | `#5A534A` | texte secondaire |
| `accent` (primaire) | `#C0392B` | rouge BD franco-belge, CTA, accents |
| `owned` (succès) | `#2E7D54` | verdict « possédé » |
| `surface` (cartes) | `#FFFDF7` | cartes, feuilles |
| `ghost` (manquant) | `#C9C1B2` | contour des tranches manquantes |

- Contraste texte/fond ≥ 7:1. Pas de gris clair sur clair pour du texte porteur de sens.
- Mode sombre : **hors périmètre v1** (à noter en TODO, ne pas implémenter).

### 5.2 Typographie

- **Fraunces** (display, optical/soft) : wordmark, titres d'écran, numéros de tome sur les tranches.
- **Atkinson Hyperlegible** (corps, titres de série, listes) : police conçue pour la basse vision — choix délibéré pour le destinataire.
- Échelle (sp), pensée grande dès le départ :
  - Display: 34 / Titre écran: 28 / Titre série: 22 / Corps: 18 / Légende: 15.
- **Respecter `fontScale` système** : si le père agrandit la police Android, l'app suit sans casser la mise en page (tests à 1.3× et 1.5×).

### 5.3 Cibles tactiles & accessibilité

- Toute zone tappable ≥ **56 dp**. Bouton de scan accueil ≥ 120 dp de haut.
- Espacement généreux, une action principale par écran.
- `contentDescription` sur tous les éléments interactifs (TalkBack).
- Confirmations explicites pour les suppressions, jamais d'action destructive en un seul tap.
- Langue **simple, sans jargon** : pas de « EAN », « métadonnées », « synchroniser ». On dit « code-barres », « mettre à jour les nouveautés ».

### 5.4 Le système de tranches (signature visuelle)

Chaque tome = une **tranche verticale** (book spine) :

- Largeur fixe (~52 dp), hauteur variable légère pour l'effet « rayonnage ».
- Couleur de fond = couleur de la série. Numéro de tome en gros (Fraunces) sur la tranche.
- **Possédé** : tranche pleine, couleur saturée, numéro en clair.
- **Manquant** : tranche en contour pointillé (`ghost`), fond papier, numéro estompé. Le vide se voit.
- **Lu / prêté** : petit marqueur discret en pied de tranche (point plein = lu, anneau = prêté).
- Attribution automatique des couleurs de série : palette de ~12 teintes chaudes désaturées, assignées de façon déterministe (hash de l'`id`) pour stabilité. Éviter le rouge `accent` pur (réservé aux CTA).

L'écran détail d'une série = **une étagère de ces tranches**, scrollable horizontalement, dans l'ordre des numéros. On voit d'un coup d'œil les trous.

### 5.5 Motion (sobre, thématique)

- Validation d'ajout d'un tome possédé : courte animation de **tampon encreur** (« stamp ») sur la tranche. Récompense tactile, jamais bloquante (≤ 300 ms).
- Transitions d'écran : fondu/translation discrets. Pas d'effets gratuits.
- Respecter « réduire les animations » du système.

---

## 6. Écrans (v1)

Marqués **[CŒUR]** (indispensable au MVP) ou **[PLUS]** (dans la v1 mais après le cœur).

### 6.1 Premier lancement **[CŒUR]**
- Écran d'accueil unique : saisir le prénom du père (utilisé dans le titre et la dédicace). Bouton « C'est parti ».
- Import silencieux de `seed-collection.json`.
- **Critères d'acceptation** : à la fin, la collection est peuplée et consultable ; ce flux ne réapparaît jamais.

### 6.2 Accueil **[CŒUR]**
- Wordmark : « La Bédéthèque de {Prénom} ».
- **Bouton SCANNER** énorme, centré, dominant (geste principal).
- Deux cartes : « Ma collection » (n séries · n albums · n trous) et « À paraître » (n nouveautés).
- **Critères** : le scan est atteignable en 1 tap depuis l'ouverture de l'app.

### 6.3 Scanner **[CŒUR]**
- Preview CameraX plein écran, cadre de visée, instruction courte « Visez le code-barres ».
- Détection EAN → écran Verdict (§6.4).
- Bouton de repli « Saisir à la main » → recherche série (§6.5).
- **Critères** : un EAN valide déclenche le verdict en < 2 s ; refus de permission caméra géré proprement (message + bouton réglages).

### 6.4 Verdict de scan **[CŒUR]** — trois états plein écran
1. **Possédé** (EAN connu, `owned=true`) : fond `owned`, grande coche dessinée, « Tu l'as déjà » + titre série/tome.
2. **Manquant** (EAN connu, `owned=false`) : ouvre l'étagère de la série avec le tome **mis en évidence**, libellé « Il te manque ! ». Bouton « Je viens de l'acheter » → passe `owned=true` (animation tampon).
3. **Inconnu** (EAN absent de la base) : neutre. « Nouveau code-barres. » Propose : (a) lier à un tome existant via recherche série → enregistre l'EAN sur cet album ; (b) créer un nouvel album. *Enrichissement en ligne optionnel : si réseau, tenter une requête ISBN (Google Books) pour pré-remplir titre/série — dégradation silencieuse si hors-ligne ou sans résultat.*
- **Critères** : chaque verdict en plain language, une seule action évidente, retour accueil en 1 tap. La liaison d'un EAN inconnu à un album le rend reconnu aux scans suivants.

### 6.5 Séries & recherche **[CŒUR]**
- Liste alphabétique des séries + champ de recherche (filtre instantané sur le titre).
- Chaque ligne : titre, couleur de série, compteur « x/y » et indicateur de trous.
- **Critères** : recherche tolérante (insensible casse/accents), résultat en < 300 ms sur ~35 séries.

### 6.6 Détail série = l'étagère **[CŒUR]**
- En-tête : titre, statut (En cours / Terminée), x possédés / y connus.
- **Étagère de tranches** (§5.4). Tap sur une tranche → fiche tome (toggle possédé, lu/prêté, édition, supprimer).
- Tap long sur un vide → « ajouter ce tome ».
- Bouton « + Ajouter un tome » (numéro suivant pré-rempli).
- **Critères** : basculer un tome possédé/manquant met l'étagère à jour immédiatement, avec l'animation tampon à l'ajout.

### 6.7 À paraître **[PLUS]**
- Liste des releases des séries **trackées**, triées par date, séparées en « Déjà paru » / « À venir ».
- Chaque item : série, tome, date, et badge « tu l'as » / « il te manque ».
- Bouton « Mettre à jour les nouveautés » (déclenche le fetch manuel).
- Vide élégant si aucune nouveauté.
- **Critères** : fonctionne depuis le cache hors-ligne ; le fetch échoué affiche un message non bloquant et conserve le cache.

### 6.8 Ajout manuel série / album **[CŒUR pour album, PLUS pour série]**
- Album : choisir série existante, numéro, options. Série : titre, statut, suivi oui/non.
- **Critères** : impossible de créer un doublon de numéro dans une série (message clair).

### 6.9 Réglages & À propos **[PLUS]**
- Prénom du père (édition).
- « Mettre à jour les nouveautés maintenant ».
- Notifications : activer/désactiver (gère la permission `POST_NOTIFICATIONS` ≥ API 33).
- **Sauvegarde / restauration** : export de toute la collection en JSON (et CSV lisible) via le sélecteur de fichiers ; import depuis un fichier. Anti-perte de données, anti-lock-in.
- **À propos** : version + courte **dédicace** (« Développé avec soin par {fils}, pour {Prénom}. »).
- (Avancé, discret) URL du fichier des sorties.
- **Critères** : un export puis un import sur installation neuve restituent la collection à l'identique.

---

## 7. Notifications de sorties **[PLUS]**

- **WorkManager** périodique (1×/jour, contraintes : réseau disponible, batterie non faible).
- À chaque exécution : fetch `releases.json`, diff avec la collection. Pour toute nouvelle entrée concernant une série trackée et un tome non possédé → **une notification locale** : « Nouveau tome dans {Série} : T{n} ».
- Regrouper si plusieurs (notification résumé). Tap → écran « À paraître ».
- Jamais de notification pour un tome déjà possédé.

---

## 8. Structure de projet suggérée

```
app/src/main/java/<pkg>/
  data/
    local/        (AppDatabase, SeriesDao, AlbumDao, entities)
    remote/       (ReleasesApi, ReleasesDto, fetch JSON)
    seed/         (SeedImporter, parsing seed-collection.json)
    repo/         (CollectionRepository, ReleasesRepository)
  domain/         (modèles d'affichage, logique de "trous", couleurs)
  ui/
    theme/        (Color, Type, Theme — jetons §5)
    components/   (SpineTile, Shelf, BigScanButton, VerdictScreen...)
    home/  scanner/  series/  seriesdetail/  releases/  settings/  onboarding/
  work/           (ReleasesSyncWorker)
  MainActivity.kt
app/src/main/res/font/   (Fraunces, AtkinsonHyperlegible)
app/src/main/assets/     (seed-collection.json, releases.json par défaut)
```

---

## 9. Ordre de construction (phases testables)

Construire et valider phase par phase. Chaque phase doit compiler et être démontrable.

- **Phase 0 — Socle.** Projet, dépendances, thème complet (§5.1–5.2), polices, navigation squelette, écran accueil statique.
- **Phase 1 — Données.** Entités/DAOs Room, repository, import du seed au premier lancement. Vérifiable via la liste des séries peuplée.
- **Phase 2 — Cœur consultation (MVP livrable).** Recherche séries + détail série avec **l'étagère de tranches** + toggle possédé/manquant + fiche tome + ajout manuel d'album. **À ce stade, l'app remplit déjà l'usage librairie.**
- **Phase 3 — Scan.** CameraX + ML Kit, écran scanner, verdict 3 états, liaison EAN→album.
- **Phase 4 — Sorties.** Fetch `releases.json`, cache, écran « À paraître », diff collection.
- **Phase 5 — Fond & finition.** WorkManager + notifications, réglages, export/import JSON+CSV, À propos/dédicace, passes d'accessibilité (fontScale 1.5×, TalkBack), animations tampon.

Livrer un APK signé en debug à la fin de la Phase 2 pour un premier test réel, puis itérer.

---

## 10. Définition de « terminé » pour la v1

- L'usage librairie fonctionne **hors-ligne** de bout en bout (recherche série → étagère → trous).
- Le scan reconnaît un EAN déjà appris et propose proprement la liaison d'un EAN inconnu.
- « À paraître » s'alimente du fichier curé, en ligne comme depuis le cache.
- Une notification arrive pour un nouveau tome d'une série suivie non possédée.
- Export/import restituent fidèlement la collection.
- L'app passe les tests d'accessibilité (police agrandie, contraste, TalkBack) sans casse.
- Aucune fonctionnalité ne dépend d'un compte ou d'un service tiers obligatoire.

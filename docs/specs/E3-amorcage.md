# E3 — L'AMORÇAGE

> De zéro à deux mille albums en une soirée.
> **14 sprints-personne · H2 (sprints 9–10) · pilote : MAIN, avec LISIBILITÉ**
> Dépend de : [E1](E1-catalogue.md) livré.

---

## 1. Pourquoi

Constat le plus brutal de la recherche utilisateur ([R2](../COMITES.md#r2--terrain--ce-que-veulent-vraiment-les-gens))
: **4 personnes sur 15 ont abandonné une application concurrente parce qu'il fallait
ressaisir 400 albums.**

L'application actuelle possède un `SeedImporter` — mais il lit un fichier
`assets/seed-collection.json` **fabriqué à la main pour un utilisateur unique**. C'est
une solution parfaite pour la personne à qui l'application a été offerte, et
inutilisable pour quiconque d'autre.

Sans amorçage, le produit est mort au premier lancement pour tout nouvel utilisateur :
il ouvre une étagère vide et referme l'application. Aucune autre fonctionnalité ne
compense cela.

---

## 2. Périmètre

### Dans le périmètre
- Import de fichiers CSV et JSON depuis les applications concurrentes (Bubble/BDovore,
  BDGest) et depuis un tableur quelconque.
- Assistant d'import : détection de format, correspondance des colonnes,
  prévisualisation, rapport de résultat.
- « Ajouter une série entière » depuis le catalogue, avec grille de sélection des tomes.
- Amorçage par scan guidé : le mode inventaire présenté comme parcours de démarrage.
- Refonte du premier lancement : quatre chemins, aucun obligatoire.

### Hors périmètre
- Photographie d'étagère → R&D H3, plafonnée et conditionnée à un go/no-go
  ([R7](../COMITES.md#r7--arbitrage-de-la-feuille-de-route)).
- Import depuis un compte en ligne d'un concurrent (exigerait des identifiants : refus net).
- Reprise du `SeedImporter` historique, qui reste tel quel pour l'utilisateur d'origine.

---

## 3. Récits utilisateur

### US-3.1 — Import d'un export concurrent
```
Étant donné  un fichier CSV exporté depuis une autre application (600 lignes)
Quand        je le sélectionne dans l'assistant d'import
Alors        le format est reconnu automatiquement
Et           je vois un aperçu des 10 premières lignes avec les colonnes interprétées
Et           je peux corriger l'affectation de chaque colonne avant de valider
Et           l'import de 600 albums prend moins de 30 secondes
```

### US-3.2 — Un import ne détruit jamais rien
```
Étant donné  une collection déjà peuplée
Quand        j'importe un fichier
Alors        une sauvegarde automatique est prise avant toute écriture
Et           l'import AJOUTE, il ne remplace pas
Et           les doublons (même série, même tome) sont détectés et signalés, pas dupliqués
Et           un rapport final indique : n ajoutés · n déjà présents · n ignorés, avec le motif
```
> Distinction essentielle avec l'import de sauvegarde existant
> (`CollectionRepository.importSnapshot`), qui **remplace intégralement** la collection
> dans une transaction. Ce sont deux opérations différentes, avec deux mots différents
> dans l'interface : **« restaurer »** (remplace) et **« importer »** (ajoute).
> Les confondre ferait perdre une collection.

### US-3.3 — Ajouter une série entière
```
Étant donné  une série du catalogue comptant 22 tomes
Quand        je choisis « Ajouter cette série »
Alors        une grille des 22 tomes s'affiche, tous décochés
Et           « Tout cocher jusqu'au tome… » me permet de saisir 13 et de tout cocher d'un coup
Et           la validation crée 13 albums possédés et 9 manquants
Et           l'opération complète prend moins de 20 secondes
```

### US-3.4 — Un premier lancement qui n'impose rien
```
Étant donné  une installation neuve
Quand        j'arrive au premier écran après la saisie de mon prénom
Alors        quatre chemins me sont proposés, aucun obligatoire :
             « Scanner mes albums » · « Importer un fichier » · « Chercher mes séries »
             · « Je commencerai plus tard »
Et           quel que soit mon choix, j'atteins un état utilisable
```

### US-3.5 — Un fichier illisible ne casse rien
```
Étant donné  un fichier corrompu, vide, ou d'un format inattendu
Quand        je tente de l'importer
Alors        un message en langage clair explique ce qui n'a pas fonctionné
Et           aucune écriture n'a eu lieu dans ma collection
Et           je peux réessayer avec un autre fichier
```

---

## 4. Conception technique

### 4.1 Formats reconnus

| Format | Détection | Colonnes attendues |
|---|---|---|
| **BDteque JSON** | clé `series` + `version` | Format natif — c'est aussi notre export |
| **BDteque CSV** | en-tête connu | Format natif lisible, déjà produit par `CollectionExport` |
| **CSV générique** | délimiteur `,` `;` ou tabulation | Correspondance manuelle guidée |
| **Export Bubble/BDovore** | signature d'en-tête | Correspondance pré-établie |
| **Export BDGest** | signature d'en-tête | Correspondance pré-établie |

**Colonnes reconnues**, par ordre de priorité : `ISBN`/`EAN` (le plus fiable — permet la
résolution par le catalogue), puis `Série` + `Tome`, puis `Titre` seul (résolution
approximative, marqué « à vérifier »).

### 4.2 Moteur d'import

Dans `:core:domain`, donc pur et testable sans Android.

```kotlin
data class ImportRow(
    val ean: String?, val seriesTitle: String?, val tomeNumber: Int?,
    val albumTitle: String?, val owned: Boolean = true, val sourceLine: Int,
)

sealed interface RowOutcome {
    data class Added(val album: Album) : RowOutcome
    data class AlreadyPresent(val existingId: String) : RowOutcome
    data class NeedsReview(val row: ImportRow, val reason: String) : RowOutcome
    data class Rejected(val row: ImportRow, val reason: String) : RowOutcome
}

data class ImportReport(
    val added: Int, val alreadyPresent: Int,
    val needsReview: List<RowOutcome.NeedsReview>,
    val rejected: List<RowOutcome.Rejected>,
    val backupFile: File?,
)
```

**Séquence d'exécution**, non négociable :

1. Lecture et analyse **complètes en mémoire** — aucune écriture avant la fin de
   l'analyse. Un fichier corrompu à la ligne 400 ne doit pas laisser 399 albums importés.
2. Résolution de chaque ligne : EAN → catalogue ([E1](E1-catalogue.md)), sinon
   titre de série → correspondance approchée, sinon `NeedsReview`.
3. Détection de doublons contre la collection **et** à l'intérieur du fichier.
4. **Sauvegarde automatique** via `BackupManager.backupNow()` — le mécanisme existe
   déjà, on le réutilise.
5. Écriture **dans une seule transaction Room** (comme `importSnapshot` le fait déjà).
6. Rapport affiché, exportable en texte pour vérification ultérieure.

### 4.3 Grille de sélection de tomes

Composant `TomeGrid` : une grille de pastilles numérotées, 5 par ligne.

- Toucher une pastille bascule possédé / manquant.
- « Cocher jusqu'au tome… » — le geste qui fait gagner le plus de temps, validé au
  chronomètre avec le panel.
- « Tout cocher » / « Tout décocher ».
- Cibles ≥ 56 dp ; à `fontScale 2.0`, la grille passe à 3 colonnes plutôt que de tronquer.
- Description vocale par pastille : « Tome 7, non possédé, appuyer pour ajouter ».

### 4.4 Comportements en erreur

| Situation | Comportement |
|---|---|
| Fichier vide ou illisible | Message clair, aucune écriture |
| Encodage non UTF-8 (Latin-1 fréquent sur les vieux tableurs) | Détection, conversion, ou message explicite |
| Colonnes non reconnues | Assistant de correspondance manuelle, jamais un échec sec |
| Ligne isolée invalide | Ligne rejetée avec son numéro, l'import continue |
| Plus de 20 % de lignes rejetées | Import **suspendu**, confirmation demandée : probablement un mauvais fichier |
| Interruption pendant l'écriture | Transaction annulée par Room, collection intacte |
| Disque plein pendant la sauvegarde préalable | Import refusé — on n'importe jamais sans filet |
| Fichier de 50 000 lignes | Traitement par lots, indicateur de progression, annulation possible |

---

## 5. Interface & accessibilité

### 5.1 Premier lancement, écran de démarrage

```
┌──────────────────────────────────┐
│  Bonjour Michel.                 │
│  Comment veux-tu commencer ?     │
│                                  │
│  ┌────────────────────────────┐  │
│  │ 📷  Scanner mes albums     │  │
│  │     Un par un, à ton rythme│  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ 📄  Importer un fichier    │  │
│  │     Depuis une autre appli │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ 🔍  Chercher mes séries    │  │
│  │     Thorgal, XIII, Astérix…│  │
│  └────────────────────────────┘  │
│                                  │
│  Je commencerai plus tard        │
└──────────────────────────────────┘
```

- Aucun chemin n'est mis en avant : les trois cartes ont le même poids visuel.
- « Je commencerai plus tard » est en retrait mais **toujours accessible** : imposer
  une saisie au premier lancement est le meilleur moyen de perdre l'utilisateur.
- Cartes ≥ 88 dp, texte d'explication en corps de texte, jamais en légende.

### 5.2 Rapport d'import

```
┌──────────────────────────────────┐
│  C'est fait.                     │
│                                  │
│  612 albums ajoutés              │
│  14 étaient déjà là              │
│  7 à vérifier                    │
│                                  │
│  [ Voir les 7 à vérifier ]       │
│  [ Voir ma collection ]          │
│                                  │
│  Une sauvegarde a été faite      │
│  avant l'import, au cas où.      │
└──────────────────────────────────┘
```

La dernière phrase est spécifiée telle quelle. C'est la phrase qui rend un import
acceptable pour quelqu'un qui a peur de casser quelque chose.

---

## 6. Plan de test

### Tests unitaires
- `CsvParserTest` — délimiteurs, guillemets, champs contenant le délimiteur, encodages,
  fins de ligne Windows et Unix, en-tête absent.
- `ColumnMapperTest` — détection des formats connus, correspondance manuelle, colonnes
  manquantes.
- `ImportEngineTest` — doublons collection, doublons intra-fichier, lignes invalides,
  seuil de 20 % de rejets, rapport.
- `TomeGridStateTest` — cocher jusqu'à N, tout cocher, basculement individuel.

### Tests instrumentés
- `ImportFlowTest` — fichier de 600 lignes, de bout en bout, avec sauvegarde préalable.
- `ImportRollbackTest` — interruption en cours d'écriture, collection intacte.
- `AddWholeSeriesTest` — 22 tomes, sélection jusqu'à 13, vérification du résultat.

### Tests de terrain
- Exports réels fournis par le panel : au moins 3 formats différents, tous importés
  sans perte.
- **Chronomètre** : 600 albums importés en moins de 5 minutes, prise en main comprise,
  par une personne n'ayant jamais vu l'application.

---

## 7. Risques

| # | Risque | Titulaire | Parade |
|---|---|---|---|
| Q1 | Les formats d'export concurrents ne sont pas documentés et peuvent changer. | BOUSSOLE | Collecte de fichiers réels auprès du panel. Le CSV générique avec correspondance manuelle est le vrai filet : il fonctionne quel que soit le format. |
| Q2 | Confusion « importer » / « restaurer » — la plus dangereuse de l'application. | LISIBILITÉ | Deux mots distincts, deux emplacements distincts, deux formulations de confirmation distinctes. Test d'usage dédié auprès de 5 personnes. |
| Q3 | Import massif de lignes sans EAN → collection remplie de données approximatives. | MAIN | Marquage « à vérifier », écran de revue dédié, jamais de correspondance approximative silencieuse. |

---

## 8. Ce qui prouve que c'est terminé

- [ ] Un export concurrent de 600 lignes est importé en moins de 30 secondes, sans perte.
- [ ] Une sauvegarde est systématiquement prise avant tout import, et vérifiée.
- [ ] Un fichier corrompu ne laisse aucune trace dans la collection.
- [ ] Une série de 22 tomes est ajoutée, avec 13 possédés, en moins de 20 secondes.
- [ ] Cinq personnes du panel distinguent sans hésitation « importer » de « restaurer ».
- [ ] Le premier lancement n'impose aucune saisie.
</content>

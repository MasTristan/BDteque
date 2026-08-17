# E2 — LE VERDICT

> Un scan qui ne renonce jamais.
> **16 sprints-personne · H2 (sprints 7–8) · pilote : ŒIL, avec MAIN**
> Dépend de : [E1](E1-catalogue.md) livré.

---

## 1. Pourquoi

Le scan est **le** geste du produit. Il fonctionne aujourd'hui : lecture confirmée sur
plusieurs images, somme de contrôle EAN, cache d'identification, mode inventaire en
rafale. C'est du bon travail.

Mais il s'arrête à la première difficulté, et les difficultés sont fréquentes chez notre
utilisateur de référence :

- **Les albums d'avant 1990 n'ont pas de code-barres.** Une partie substantielle d'une
  collection ancienne est hors d'atteinte. Les trois personnes de plus de 65 ans du
  panel (constat [R2](../COMITES.md#r2--terrain--ce-que-veulent-vraiment-les-gens)) sont
  précisément dans ce cas.
- **Les sur-couvertures plastifiées** des éditions de bibliothèque réfléchissent la
  lumière et empêchent la lecture.
- **Les dos abîmés** d'albums d'occasion rendent le code illisible.
- **Les codes-barres de prix** collés par le libraire cachent l'EAN, ou pire, sont lus
  à sa place.

Dans tous ces cas, l'application renvoie vers un formulaire de saisie manuelle : l'échec
exact qu'elle prétend éviter. Une application qui exige un code-barres est inutile sur
la moitié d'une étagère ancienne.

---

## 2. Périmètre

### Dans le périmètre
- Chaîne de résolution complète à 5 étages, avec repli à chaque échec.
- Reconnaissance de texte sur couverture (ML Kit Text Recognition, embarqué) → catalogue.
- Rejet des codes-barres non bibliographiques (codes de prix internes).
- Durcissement du mode inventaire : file d'attente, annulation, résumé de session.
- Ergonomie à une main : lampe, retour haptique, mode discret.
- Saisie manuelle assistée par le catalogue en dernier recours.

### Hors périmètre
- Reconnaissance d'étagère complète → [E3](E3-amorcage.md), R&D encadrée.
- Toute inférence hors de l'appareil. **Aucune image ne quitte le téléphone**, jamais,
  sous aucun prétexte — sans quoi le pilier P3 tombe.
- Reconnaissance de planches ou de contenu.

---

## 3. Récits utilisateur

### US-2.1 — Un album sans code-barres
```
Étant donné  un album de 1985 sans code-barres
Quand        je vise sa couverture et que rien n'est détecté pendant 4 secondes
Alors        l'application propose « Pas de code-barres ? Photographie la couverture »
Et           après la photo, elle propose les albums correspondants du catalogue
Et           je choisis, je ne subis jamais une sélection automatique
```

### US-2.2 — Zéro faux positif
```
Étant donné  une reconnaissance de couverture dont la confiance est inférieure à 0,80
Quand        l'analyse se termine
Alors        aucune proposition n'est affichée
Et           la saisie manuelle assistée est proposée à la place
```
> **Règle absolue de l'épopée.** Proposer le mauvais album est pire que ne rien
> proposer : l'utilisateur enregistre une donnée fausse dans une collection qu'il ne
> revérifiera jamais. **Le silence est un résultat acceptable ; l'erreur ne l'est pas.**

### US-2.3 — Ignorer le code-barres du libraire
```
Étant donné  un album portant une étiquette de prix à code-barres
Quand        je scanne
Alors        seul un EAN-13 commençant par 978 ou 979 est retenu
Et           les codes internes (préfixes 2x, 02x) sont ignorés silencieusement
Et           la lecture continue jusqu'à trouver un code bibliographique
```

### US-2.4 — Une pile de 30 albums
```
Étant donné  le mode inventaire actif
Quand        je scanne 30 albums d'affilée
Alors        chaque scan est confirmé par un retour bref (haptique + visuel, son optionnel)
Et           l'écran affiche en continu « n ajoutés · n déjà possédés · n à vérifier »
Et           je peux annuler le dernier scan d'un geste
Et           en fin de session, un résumé permet de revoir et corriger avant validation
```

### US-2.5 — Une main, dans une librairie
```
Étant donné  un téléphone tenu d'une seule main dans une librairie mal éclairée
Quand        j'ouvre le scanner
Alors        la lampe est activable sans changer de main (zone tactile basse, ≥ 56 dp)
Et           le mode discret coupe tous les sons sans couper le retour haptique
Et           la caméra est prête en moins de 800 ms
```

---

## 4. Conception technique

### 4.1 La chaîne de résolution

```
                    ┌─────────────────────────────────┐
   code-barres  →   │ 1. Collection locale (par EAN)   │ → VERDICT « Tu l'as »
                    └────────────┬────────────────────┘   ou « Il te manque »
                                 │ absent
                    ┌────────────▼────────────────────┐
                    │ 2. Catalogue local (E1)          │ → VERDICT enrichi, hors-ligne
                    └────────────┬────────────────────┘
                                 │ absent
                    ┌────────────▼────────────────────┐
                    │ 3. Cache d'identification (Room) │ → VERDICT enrichi
                    └────────────┬────────────────────┘
                                 │ absent
                    ┌────────────▼────────────────────┐
                    │ 4. Réseau BnF / Open Library     │ → VERDICT enrichi
                    │    OPT-IN, jamais par défaut     │   + mise en cache
                    └────────────┬────────────────────┘
                                 │ absent / hors-ligne / refusé
   pas de code-barres            │
   ────────────────►┌────────────▼────────────────────┐
                    │ 5. Photo de couverture → OCR     │ → PROPOSITIONS (≥ 0,80)
                    │    → recherche dans le catalogue │
                    └────────────┬────────────────────┘
                                 │ échec ou confiance faible
                    ┌────────────▼────────────────────┐
                    │ 6. Saisie manuelle assistée      │ → toujours disponible
                    └─────────────────────────────────┘
```

**Changement majeur par rapport à l'existant** : l'étage 4 (réseau) passe **après** le
catalogue local et devient explicitement opt-in. Aujourd'hui il est systématique et
silencieux — c'est la dette D4. Un réglage clair le gouverne :
*« Chercher sur internet les albums que le catalogue ne connaît pas »*, désactivé par
défaut, avec l'explication en langage simple de ce que cela implique.

### 4.2 Filtrage des codes-barres

Le filtre étend `Ean.kt` existant (somme de contrôle déjà implémentée et testée).

```kotlin
enum class BarcodeVerdict { BIBLIOGRAPHIQUE, PRIX_INTERNE, INVALIDE }

fun classify(raw: String): BarcodeVerdict = when {
    !isValidEan13(raw)                             -> BarcodeVerdict.INVALIDE
    raw.startsWith("978") || raw.startsWith("979") -> BarcodeVerdict.BIBLIOGRAPHIQUE
    raw.startsWith("2") || raw.startsWith("02")    -> BarcodeVerdict.PRIX_INTERNE
    else                                           -> BarcodeVerdict.INVALIDE
}
```

Un `PRIX_INTERNE` n'interrompt pas la lecture : l'analyse continue, sans message —
l'utilisateur ne doit jamais avoir à comprendre pourquoi une étiquette est ignorée.

### 4.3 Reconnaissance de couverture

**Déclenchement** : jamais automatique. Proposé après 4 secondes sans détection, ou par
un bouton explicite « Pas de code-barres ? ».

**Traitement**, intégralement sur l'appareil :

1. Capture d'une image fixe (pas de flux — meilleure qualité, moins de calcul).
2. ML Kit Text Recognition v2, modèle latin embarqué.
3. Extraction des blocs de texte, tri par surface décroissante (le titre est
   généralement le plus grand texte de la couverture).
4. Normalisation par `TextNormalization` **existant** — ne pas réimplémenter.
5. Recherche FTS dans `catalog_series_fts` + `catalog_album`.
6. Score combiné : correspondance textuelle (0,7) + surface relative du bloc (0,3).
7. **Aucune proposition en dessous de 0,80.** Au plus 3 propositions, ordonnées.
8. L'image est **détruite immédiatement** après analyse. Jamais écrite sur le disque,
   jamais transmise, jamais conservée en mémoire au-delà du traitement.

```kotlin
data class CoverMatch(
    val album: CatalogAlbum,
    val confidence: Float,       // ≥ 0.80 pour être affiché
    val matchedText: String,     // le texte reconnu, montré à l'utilisateur
)
```

> Montrer le texte reconnu n'est pas un détail technique : c'est ce qui permet à
> l'utilisateur de comprendre *pourquoi* on lui propose cet album, et donc de juger.
> Une proposition inexplicable est une proposition qu'on accepte à tort.

### 4.4 Mode inventaire durci

L'existant (`InventoryViewModel`, 179 lignes) est étendu :

| Aspect | Aujourd'hui | Cible |
|---|---|---|
| File d'attente | Traitement immédiat | File asynchrone, la caméra ne bloque jamais |
| Annulation | Aucune | Annuler le dernier · annuler un élément du résumé |
| Doublon dans la session | Ajouté deux fois | Détecté, signalé, compté une fois |
| Fin de session | Sortie directe | Résumé récapitulatif, correction possible, validation explicite |
| Retour sensoriel | Visuel | Haptique + visuel, son optionnel (mode discret) |
| Interruption | État perdu | Session persistée, reprise proposée au retour |

**Persistance de session** : `DataStore`, pas Room. Une session d'inventaire est un état
transitoire d'interface, pas une donnée de collection — elle n'a rien à faire dans la
Moitié A.

### 4.5 Comportements en erreur

| Situation | Comportement |
|---|---|
| Permission caméra refusée | Écran explicatif + bouton vers les réglages système (déjà implémenté, conservé) |
| Permission refusée définitivement | Message clair, saisie manuelle proposée, pas de boucle de demande |
| Caméra occupée par une autre application | Message explicite, nouvelle tentative possible |
| Éclairage insuffisant | Après 3 s sans détection : suggestion d'activer la lampe |
| ML Kit indisponible | Repli sur la saisie manuelle, aucun plantage |
| Mémoire insuffisante pour l'OCR | Analyse abandonnée, message non bloquant |
| Application interrompue pendant l'inventaire | Session restaurée au retour, avec confirmation |

---

## 5. Interface & accessibilité

### 5.1 Écran scanner

```
┌──────────────────────────────────┐
│                                  │
│      ┌ ─ ─ ─ ─ ─ ─ ─ ─ ┐         │
│      │                 │         │   cadre de visée
│      │                 │         │
│      └ ─ ─ ─ ─ ─ ─ ─ ─ ┘         │
│                                  │
│   Vise le code-barres            │
│                                  │
│  [ 🔦 ]        [ Pas de          │   ← zone basse, atteignable au pouce
│                  code-barres ? ] │
│  [ Saisir à la main ]            │
└──────────────────────────────────┘
```

- Le bouton « Pas de code-barres ? » n'apparaît **qu'après 4 secondes** sans détection :
  ne pas encombrer le cas nominal, ne pas suggérer l'échec avant qu'il survienne.
- Zone basse : tout ce qui est tactile est à portée de pouce sur un écran de 6,5".
- Cibles ≥ 56 dp, y compris la lampe.

### 5.2 Écran de proposition après OCR

```
┌──────────────────────────────────┐
│  J'ai lu sur la couverture :     │
│  « LE FEU ÉCARLATE · THORGAL »   │
│                                  │
│  Est-ce l'un de ceux-ci ?        │
│  ┌────────────────────────────┐  │
│  │ Thorgal — T40              │  │
│  │ Le Feu écarlate            │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ Thorgal — T39              │  │
│  │ Le Barbare                 │  │
│  └────────────────────────────┘  │
│                                  │
│  [ Aucun de ceux-là ]            │
└──────────────────────────────────┘
```

**Exigences d'accessibilité**
- Le texte reconnu est affiché : la proposition est explicable, donc jugeable.
- Chaque proposition ≥ 72 dp de haut (plus que le minimum : on choisit un livre, pas
  une ligne de liste).
- « Aucun de ceux-là » a le même poids visuel que les propositions — refuser doit être
  aussi facile qu'accepter.
- Annonce vocale : « Proposition 1 sur 3 : Thorgal, tome 40, Le Feu écarlate ».
- Aucune proposition ne peut être validée par un geste accidentel : pas de balayage,
  pas de sélection par défaut, pas de bouton pré-focalisé.

---

## 6. Plan de test

### Tests unitaires
- `BarcodeClassifierTest` — 978, 979, préfixes de prix, sommes de contrôle invalides,
  chaînes vides, EAN-8.
- `CoverMatchScorerTest` — combinaison des scores, seuil 0,80, ordonnancement,
  ex æquo, texte vide.
- `InventorySessionTest` — file, annulation, doublons intra-session, reprise après
  interruption.

### Tests instrumentés
- `ScannerPermissionTest` — accordée, refusée, refusée définitivement.
- `InventoryFlowTest` — 30 scans simulés, annulation, résumé, validation.
- `NoNetworkDuringScanTest` — **aucune requête réseau émise** quand l'étage 4 est
  désactivé, vérifié par interception.

### Tests de terrain (corpus ŒIL, 300 photos réelles)
| Catégorie | Volume | Cible |
|---|---|---|
| Codes-barres nets | 100 | 99 % |
| Codes-barres abîmés ou sous plastique | 100 | 80 % |
| Albums sans code-barres (OCR) | 100 | **70 % au premier choix, 0 faux positif** |
| **Taux de résolution global** | 300 | **≥ 90 %** |

### Performance
- Caméra prête : < 800 ms (Galaxy A14).
- Détection → verdict, catalogue local : p95 < 1,5 s.
- OCR de couverture, de la capture aux propositions : < 2,5 s.

---

## 7. Risques

| # | Risque | Titulaire | Parade |
|---|---|---|---|
| Q1 | La qualité de l'OCR sur des couvertures anciennes (typographies dessinées, texte intégré à l'illustration) est incertaine. C'est le point faible connu de la reconnaissance de texte sur des visuels de BD. | ŒIL | Corpus de 300 photos constitué **avant** le développement. Seuil de confiance élevé plutôt qu'un taux de couverture flatteur. |
| Q2 | Le seuil 0,80 pourrait s'avérer trop permissif en conditions réelles. | ŒIL | Paramètre de build, ajusté sur mesures. Le déplacer vers le haut est toujours autorisé ; vers le bas, jamais sans mesure. |
| Q3 | Le repli OCR pourrait devenir le chemin principal si le catalogue est incomplet. | SOURCE + ŒIL | Suivi du taux d'usage relatif des étages. Un recours massif à l'étage 5 est un signal d'alarme sur E1, pas un succès de E2. |

---

## 8. Ce qui prouve que c'est terminé

- [ ] Un album de 1985 sans code-barres est identifié par photo de couverture.
- [ ] Sur 300 photos réelles, taux de résolution ≥ 90 %, **0 faux positif**.
- [ ] Une étiquette de prix ne perturbe pas la lecture, sans message à l'utilisateur.
- [ ] 30 albums scannés en rafale, avec annulation et correction avant validation.
- [ ] Aucune image ne quitte l'appareil ni n'est écrite sur le disque, vérifié par
      capture réseau et inspection du système de fichiers.
- [ ] Le scanner est utilisable d'une seule main sur un écran de 6,5".
</content>

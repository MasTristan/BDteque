# E7 — L'ÉTAGÈRE VIVANTE

> Le plaisir. Volontairement en dernier : on gagne le droit de faire du beau.
> **12 sprints-personne · H3 (sprints 16–19) · pilote : LISIBILITÉ, avec MAIN**

---

## 1. Pourquoi

L'étagère de tranches est la signature du produit — et elle n'existe aujourd'hui qu'à
l'intérieur d'une série. On ne voit jamais **sa bibliothèque**. L'écran d'accueil résume
la collection en trois nombres ; c'est efficace, ce n'est pas émouvant.

Or ce produit ne se vend pas sur ses fonctionnalités. Il se transmet parce qu'on a envie
de le montrer. *« Regarde, c'est ma collection »* est un usage réel, observé dans le
panel, et c'est le meilleur canal d'acquisition dont nous disposerons jamais — nous
n'avons ni publicité, ni télémétrie, ni parrainage.

Le widget d'écran d'accueil relève d'une autre logique : il économise deux taps sur le
geste central, en librairie, sur un téléphone tenu d'une main. Jugé « gadget » par trois
personnes en R7, il a été maintenu pour cette raison. **C'est P1, pas de la décoration.**

---

## 2. Périmètre

### Dans le périmètre
- Mur d'étagères : toute la collection, série par série, en une vue continue.
- Statistiques de collection, sobres et locales.
- Widget d'écran d'accueil : manquants + bouton scanner.
- Raccourci de scan (appui long sur l'icône, tuile de réglages rapides).
- Écran « Ma bibliothèque en un an ».

### Hors périmètre
- Partage d'une image de sa collection sur les réseaux (exigerait un rendu hors écran
  et ouvrirait la porte à des considérations de droits sur les couvertures).
- Classements, comparaisons entre utilisateurs (viole P3).
- Thème personnalisable ([ROADMAP §6](../ROADMAP.md) : coût d'accessibilité).

---

## 3. Récits utilisateur

### US-7.1 — Voir sa bibliothèque
```
Étant donné  une collection de 47 séries et 612 albums
Quand        j'ouvre « Ma bibliothèque »
Alors        je vois toutes mes séries en étagères successives, dans une vue continue
Et           le défilement reste fluide, sans image perdue
Et           je peux basculer entre le tri alphabétique, par complétion, et par ajout récent
```

### US-7.2 — Le widget en librairie
```
Étant donné  le widget installé sur mon écran d'accueil
Quand        je le regarde
Alors        il affiche « 12 albums te manquent »
Et           un appui ouvre directement le scanner, sans passer par l'accueil
Et           il se met à jour au plus une fois par heure, sans réveiller l'application
```

### US-7.3 — Une année de lecture
```
Étant donné  une collection alimentée depuis plus d'un an
Quand        j'ouvre « Ma bibliothèque en un an »
Alors        je vois : albums ajoutés, séries commencées, séries complétées,
             et la série la plus proche d'être complète
Et           tout est calculé localement, à partir de dateAdded
Et           rien n'est envoyé nulle part
```

### US-7.4 — Une collection débutante n'est pas humiliée
```
Étant donné  une collection de 3 albums
Quand        j'ouvre les statistiques
Alors        l'écran reste chaleureux et n'affiche ni vide décourageant, ni graphique plat
Et           il propose une action utile plutôt qu'un constat
```

---

## 4. Conception technique

### 4.1 Mur d'étagères

`LazyColumn` de `Shelf`, chaque `Shelf` restant le `LazyRow` de `SpineTile` existant.
Le composant n'est pas réécrit : il est réutilisé tel quel.

**Contraintes de performance** (budget : 0 image perdue à 2 000 albums, Pixel 6a) :

- Clés stables sur tous les éléments (`key = album.id`) — indispensable, et pas
  seulement pour la performance : sans clé, l'animation de tampon se déclenche sur le
  mauvais élément après recomposition.
- Les rangées hors écran ne composent pas leurs tranches.
- Couleurs de série calculées une fois, jamais dans la composition (`SpineColor` est
  déjà déterministe : mémoriser le résultat).
- Couvertures : uniquement les fichiers **déjà locaux** ([ADR-006](../DECISIONS.md#adr-006)),
  décodées à la taille d'affichage, jamais en pleine résolution.

### 4.2 Statistiques

Fonctions pures, dans `:core:domain`, sans état :

```kotlin
data class YearReview(
    val albumsAdded: Int,
    val seriesStarted: Int,
    val seriesCompleted: Int,
    val closestToComplete: SeriesProgress?,
    val longestGap: SeriesProgress?,      // la série la plus incomplète
)
```

Calculées à partir de `dateAdded` (déjà présent sur `Album`). **Aucune nouvelle donnée
n'est collectée** pour produire ces statistiques : tout est dérivé de ce que
l'utilisateur a lui-même saisi. C'est une différence de nature avec une analyse d'usage.

### 4.3 Widget

`AppWidgetProvider` + Glance.

- Mise à jour au plus une fois par heure, et à chaque modification de la collection.
- Deux tailles : 2×1 (nombre + bouton scan), 4×2 (+ trois séries les plus incomplètes).
- **Ne réveille jamais l'application** : lecture d'un instantané écrit dans DataStore
  par la couche collection.
- Fonctionne sans catalogue et sans réseau.
- Description de contenu complète : « 12 albums te manquent. Appuyer pour scanner. »

### 4.4 Comportements en erreur

| Situation | Comportement |
|---|---|
| Collection vide | Mur d'étagères affichant une invitation, pas un vide |
| Moins d'un an de données | Statistiques adaptées à la période réelle disponible |
| Widget sans données (premier lancement) | « Ouvre l'application pour commencer », bouton actif |
| Couverture locale corrompue | Repli sur la tranche colorée, sans message |
| Mémoire contrainte | Décodage réduit, jamais de plantage |

---

## 5. Interface & accessibilité

### 5.1 Mur d'étagères

```
┌──────────────────────────────────┐
│  Ma bibliothèque                 │
│  47 séries · 612 albums · 38 ⌷   │
│                                  │
│  Astérix              38/39      │
│  ▐▐▐▐▐▐▐▐▐▐▐▐▐▐▐░▐▐▐▐            │
│                                  │
│  Blake et Mortimer    25/29      │
│  ▐▐▐▐▐▐▐▐▐▐▐░▐▐▐▐▐▐▐░░░          │
│                                  │
│  Buck Danny Classic   13/13  ✓   │
│  ▐▐▐▐▐▐▐▐▐▐▐▐▐                   │
└──────────────────────────────────┘
```

**Exigences d'accessibilité**
- Chaque rangée est **un seul élément** pour le lecteur d'écran : « Astérix, 38 albums
  sur 39, il manque le tome 16 ». Sans cela, le mur devient un tunnel de 612 éléments —
  la même erreur que A-02, à plus grande échelle.
- La [vue liste](../DECISIONS.md#adr-009) s'applique aussi au mur.
- La complétion est portée par le **texte** (« 38/39 »), pas seulement par la barre.
- À `fontScale 2.0`, les rangées s'empilent au lieu de tronquer.

### 5.2 Ton des statistiques

Interdit : « Vous n'avez ajouté que 3 albums cette année. »
Attendu : « 3 albums sont arrivés sur ton étagère cette année. »

Le produit n'évalue pas son utilisateur. Il ne compare pas, il ne classe pas, il ne
motive pas. Il raconte. La formulation est une exigence de spécification, pas une
préférence rédactionnelle : elle est revue par LISIBILITÉ avant livraison.

---

## 6. Plan de test

### Tests unitaires
- `YearReviewTest` — collection vide, moins d'un an, année complète, séries complétées,
  bornes de date, changement d'année.
- `SeriesProgressTest` — complétion, séries ouvertes (`knownTomeCount = null`),
  hors-séries exclus.

### Tests instrumentés
- `LibraryWallScrollTest` — 2 000 albums, mesure de fluidité, budget 0 image perdue.
- `WidgetUpdateTest` — mise à jour, appui, ouverture directe du scanner.
- `EmptyCollectionTest` — aucun écran vide décourageant.

### Performance
- Mur d'étagères, 2 000 albums, Pixel 6a : 0 image perdue.
- Mur d'étagères, 2 000 albums, Galaxy A14 : < 1 % d'images perdues.
- Widget : aucun réveil de processus mesurable.

---

## 7. Risques

| # | Risque | Titulaire | Parade |
|---|---|---|---|
| Q1 | Le mur d'étagères peut devenir lourd avec les couvertures locales. | CHARPENTE | Décodage à la taille d'affichage, budget mesuré en CI, repli sur tranches colorées. |
| Q2 | Les statistiques peuvent glisser vers la gamification, incompatible avec le ton. | LISIBILITÉ | Aucun objectif, aucun classement, aucune série de records. Revue de formulation obligatoire. |
| Q3 | Glance impose des contraintes fortes sur le rendu du widget. | MAIN | Maquette validée tôt, repli sur un widget minimal (nombre + bouton) si nécessaire. |

---

## 8. Ce qui prouve que c'est terminé

- [ ] 612 albums défilent sans image perdue sur le Pixel 6a.
- [ ] Le lecteur d'écran annonce une rangée entière en une phrase utile.
- [ ] Le widget ouvre le scanner en un appui, sans réveiller l'application.
- [ ] Les statistiques sont calculées uniquement à partir de données saisies par l'utilisateur.
- [ ] Une collection de 3 albums produit un écran chaleureux.
- [ ] Aucune formulation n'évalue, ne compare ou ne classe l'utilisateur.
</content>

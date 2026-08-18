# DESIGN SYSTEM — « Papier & Encre »

> Document de référence. Source unique de vérité pour les jetons.
> Pilote : LISIBILITÉ. Toute couleur n'appartenant pas à ce document est refusée en revue.
> Décision applicable : [ADR-009](../DECISIONS.md#adr-009).

---

## 1. Intention

Une bibliothèque chaleureuse en papier et en encre, pas une base de données. La
métaphore centrale est **l'étagère où un tome manquant se voit comme un vide**.

Deux règles président à toutes les autres :

1. **La lisibilité prime sur l'esthétique** — mais l'esthétique est forte et personnelle
   partout où elle ne coûte rien à la lisibilité.
2. **Aucune information n'est portée par la seule couleur.** Un état se lit à la forme,
   au texte, et à la couleur — dans cet ordre de priorité.

---

## 2. Audit de contraste — mesures réelles

Ratios calculés selon WCAG 2.2 sur les jetons **actuellement en production**
(`ui/theme/Color.kt`). Ce tableau est l'anomalie **A-01** de l'audit
([R5](../COMITES.md#r5--design-system--accessibilité)), chiffrée.

### Thème clair

| Jeton | Sur | Ratio mesuré | AA texte (4,5) | Objectif projet (7,0) |
|---|---|---:|:---:|:---:|
| `Ink` #1C1A17 | `Paper` #F5EFE2 | **15,15:1** | ✅ | ✅ |
| `Ink` | `Surface` #FFFDF7 | **17,07:1** | ✅ | ✅ |
| `InkSoft` #5A534A | `Paper` | **6,61:1** | ✅ | ⚠️ 6,61 |
| `InkSoft` | `Surface` | **7,45:1** | ✅ | ✅ |
| `Accent` #C0392B | `Paper` | **4,75:1** | ✅ *(limite)* | ❌ |
| `OwnedGreen` #2E7D54 | `Paper` | **4,39:1** | ❌ **ÉCHEC** | ❌ |
| `Ghost` #C9C1B2 | `Paper` | **1,56:1** | ❌ **ÉCHEC** (non-texte : 3,0 requis) | ❌ |

### Thème sombre

| Jeton | Sur | Ratio mesuré | AA texte (4,5) | Objectif projet (7,0) |
|---|---|---:|:---:|:---:|
| `InkDark` #EDE6D8 | `PaperDark` #17140F | **14,79:1** | ✅ | ✅ |
| `InkSoftDark` #B8AF9F | `PaperDark` | **8,46:1** | ✅ | ✅ |
| `AccentDark` #E0796B | `PaperDark` | **6,22:1** | ✅ | ⚠️ |
| `OwnedGreenDark` #7BC9A0 | `PaperDark` | **9,36:1** | ✅ | ✅ |
| `GhostDark` #57503F | `PaperDark` | **2,30:1** | ❌ **ÉCHEC** (non-texte : 3,0 requis) | ❌ |

### Les trois défauts avérés

**Défaut 1 — `OwnedGreen` sur `Paper` : 4,39:1.** En dessous du minimum AA pour du
texte. Or c'est la couleur du verdict « Tu l'as déjà » — l'écran le plus consulté du
produit, souvent lu dans une librairie mal éclairée, par quelqu'un qui voit mal.

**Défaut 2 — `Ghost` sur `Paper` : 1,56:1.** Le contour d'une tranche manquante est un
composant d'interface porteur d'information : WCAG 2.2 exige 3:1 (critère 1.4.11).
Nous sommes à la moitié. Concrètement : **le vide, qui est l'idée centrale du produit,
est presque invisible pour une personne malvoyante.** C'est le défaut le plus grave des
trois, parce qu'il touche la métaphore fondatrice.

**Défaut 3 — `GhostDark` sur `PaperDark` : 2,30:1.** Même défaut en thème sombre.

> Le thème sombre, ajouté après coup, a dupliqué la structure des jetons sans revérifier
> les ratios. Ce n'est pas une négligence : c'est ce qui arrive à tout projet sans
> contrôle automatisé de contraste. D'où le test de la porte **G3** ([E6](E6-qualite.md)).

---

## 3. Jetons corrigés (cible v2)

Corrections **minimales** : on ne redessine pas l'identité, on rend lisible ce qui ne
l'est pas. Les jetons non listés sont inchangés.

### Thème clair

| Jeton | Avant | **Après** | Nouveau ratio /Paper | Justification |
|---|---|---|---:|---|
| `OwnedGreen` | #2E7D54 | **#1F5E3E** | **6,72:1** | Passe AA largement, garde la teinte |
| `Ghost` | #C9C1B2 | **#8C8371** | **3,27:1** | Passe le seuil non-texte de 3:1 |
| `Accent` | #C0392B | **#A83024** | **5,89:1** | Marge au-dessus de la limite ; rouge BD conservé |
| `Paper` `Ink` `InkSoft` `Surface` | — | inchangés | — | Conformes |

### Thème sombre

| Jeton | Avant | **Après** | Nouveau ratio /PaperDark | Justification |
|---|---|---|---:|---|
| `GhostDark` | #57503F | **#7C7460** | **3,96:1** | Passe le seuil non-texte avec marge |
| autres | — | inchangés | — | Conformes |

**Note sur `Ghost`.** Assombrir le contour rend le vide plus présent — ce qui va dans
le sens du produit, pas contre lui. Le tome manquant doit se voir. Le rendu est validé
par le panel avant livraison ; l'accessibilité fixe le plancher, le panel arbitre
au-dessus.

**Règle permanente** : tout nouveau jeton est mesuré avant d'entrer dans le code, et le
test automatisé de contraste (porte G3) vérifie l'ensemble à chaque build. Les valeurs
de ce document sont la référence ; le code s'y aligne, jamais l'inverse.

---

## 4. Typographie

Inchangée — elle est le meilleur choix du projet et ne se discute pas.

| Rôle | Police | Taille | Usage |
|---|---|---|---|
| `displayLarge` | Fraunces | 34 sp | Wordmark |
| `titleLarge` | Fraunces | 28 sp | Titres d'écran |
| `titleMedium` | Fraunces | 22 sp | Numéro de tome sur la tranche |
| `titleSmall` | Atkinson Hyperlegible | 20 sp | Titre de série en liste |
| `bodyLarge` | Atkinson Hyperlegible | 18 sp | Corps de texte |
| `bodyMedium` | Atkinson Hyperlegible | 16 sp | Texte secondaire |
| `labelMedium` | Atkinson Hyperlegible | 15 sp | Légendes, badges |

**Atkinson Hyperlegible** est conçue pour la basse vision par le Braille Institute.
C'est un choix produit, pas un choix graphique, et il n'est pas rediscutable.

**Règles**
- `sp` partout pour le texte. Toute neutralisation de `fontScale` est interdite.
- Toutes les vues tiennent à **`fontScale 2.0`** : les mises en page se réorganisent
  (grilles qui passent de 5 à 3 colonnes, lignes qui s'empilent), elles ne tronquent jamais.
- Aucun texte porteur de sens en dessous de 15 sp.

---

## 5. États de tranche

C'est le composant signature. Trois états visuels, chacun distinguable **sans la couleur**.

| État | Forme | Couleur | Texte alternatif |
|---|---|---|---|
| **Possédé** | tranche pleine | couleur de série saturée | « Tome 7, Les Trois Vieillards, possédé » |
| **Manquant** (trou interne) | contour pointillé | `Ghost` corrigé | « Tome 7, manquant » |
| **Paru depuis** ([E1](E1-catalogue.md)) | contour pointillé **+ liseré bas** | `Ghost` + `Accent` | « Tome 20, paru, pas encore dans ta collection » |

Marqueurs de statut de lecture, en pied de tranche :
point plein = lu · anneau = prêté · rien = non lu.
Chacun a son équivalent textuel dans la description vocale et dans la vue liste.

**Palette de tranches** : 12 teintes chaudes désaturées, assignées de façon
déterministe depuis l'identifiant de série (`SpineColor.kt`, déjà implémenté).
`Accent` n'est **jamais** une couleur de tranche : il est réservé aux actions.

---

## 6. Cibles tactiles & interaction

| Élément | Minimum | Note |
|---|---|---|
| Tout composant interactif | **56 dp** | `Modifier.defaultMinSize(minHeight = 56.dp)` |
| Bouton de scan de l'accueil | **120 dp** | Le geste central du produit |
| Carte de proposition (choix d'album) | **72 dp** | On choisit un livre, pas une ligne de liste |
| Carte de parcours (amorçage) | **88 dp** | Décision structurante |
| Espacement entre cibles | **8 dp** | Évite les touchers accidentels |

**Interdits**
- Balayage comme **seul** moyen d'accéder à une action.
- Appui long comme seul moyen (toujours doublé d'un chemin visible).
- Action destructive en un seul geste, sans confirmation énonçant la conséquence.
- Élément pré-focalisé sur un écran de choix : rien ne doit se valider par inadvertance.

---

## 7. Mouvement

- Tampon encreur au passage manquant → possédé : 300 ms, `FastOutSlowIn`, jamais bloquant.
- Transitions d'écran : fondu et translation discrets.
- `LocalReduceMotion` respecté partout : état final appliqué directement, sans animation.
- Aucune animation ne retarde une information : le verdict s'affiche, puis s'anime.

---

## 8. Langue

Le vocabulaire fait partie du design system. Il est normatif.

| ✅ On dit | ❌ On ne dit pas |
|---|---|
| code-barres | EAN, ISBN, identifiant |
| le catalogue | référentiel, bundle, base de données, index |
| mettre à jour les nouveautés | synchroniser, rafraîchir |
| il te manque | album non possédé, statut `owned = false` |
| tu l'as déjà | doublon détecté |
| ta collection ne sera pas touchée | opération non destructive |
| une sauvegarde a été faite, au cas où | sauvegarde préalable effectuée |
| il me l'a rendu | marquer comme retourné |

**Tutoiement**, cohérent avec l'origine du produit — une application offerte par un
fils à son père. Le vouvoiement en ferait un logiciel.

**Ton** : l'application n'évalue jamais son utilisateur. Elle ne compare pas, ne classe
pas, ne félicite pas, ne réprimande pas. Elle constate et elle raconte
(voir [E7 §5.2](E7-etagere.md)).

---

## 9. Ce que le design system interdit

- Toute couleur écrite en dur dans un composable.
- Toute chaîne de caractères hors de `strings.xml`.
- Toute information portée par la seule couleur.
- Tout écran sans équivalent accessible ([ADR-009](../DECISIONS.md#adr-009)).
- Toute mise en page qui tronque à `fontScale 2.0`.
- Tout jeton ajouté sans mesure de contraste préalable.
</content>

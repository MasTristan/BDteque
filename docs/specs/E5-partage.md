# E5 — PRÊTS, SOUHAITS & PARTAGE

> Le besoin est relationnel, pas seulement personnel.
> **10 sprints-personne · H3 (sprints 13–15) · pilote : MAIN, avec LISIBILITÉ**
> Décision applicable : [ADR-008](../DECISIONS.md#adr-008) — ce qui sort du téléphone est inerte.

---

## 1. Pourquoi

Le constat le plus inattendu de la recherche utilisateur
([R2](../COMITES.md#r2--terrain--ce-que-veulent-vraiment-les-gens)) : **11 personnes
sur 15** ont spontanément décrit un besoin qui n'est pas le leur.

> *« Le problème, ce n'est pas moi. C'est ma femme qui ne sait jamais quoi m'offrir. »*

Et du côté des libraires, la même chose vue de l'autre bout du comptoir : les clients
arrivent avec une capture d'écran ou un bout de papier. Ils ne veulent pas d'un
système, ils veulent **une liste lisible**.

Le produit possède déjà les deux briques : `buildShoppingList()` calcule la liste des
manquants, `toShareText()` la met en forme pour n'importe quelle messagerie. Le travail
n'est pas de construire, c'est de **rendre visible et d'enrichir** — plus une gestion
des prêts, qui est la même famille de besoin : savoir où sont ses albums quand ils ne
sont pas sur l'étagère.

**La ligne rouge**, posée en R2 par LISIBILITÉ et retenue : partager ne doit pas
reconstruire un réseau social par la bande. Le premier lien partagé appelle un
destinataire, qui appelle un identifiant, qui appelle un compte. Six mois plus tard,
le pilier P3 est mort. D'où [ADR-008](../DECISIONS.md#adr-008).

---

## 2. Périmètre

### Dans le périmètre
- Prêts : à qui, depuis quand, rappel doux.
- Liste de souhaits, distincte des trous de collection.
- Partage de la liste d'achats : texte enrichi, fichier autonome, QR.
- Liste d'achats mise en avant dans la navigation.

### Hors périmètre
- Tout service, lien, identifiant ou canal de retour ([ADR-008](../DECISIONS.md#adr-008)).
- Notification au prêteur *chez l'emprunteur* (exigerait deux installations couplées).
- Liste vivante partagée qui se met à jour à distance. Un partage est une
  **photographie**, et c'est volontaire.

---

## 3. Récits utilisateur

### US-5.1 — Savoir à qui on a prêté
```
Étant donné  un album de ma collection
Quand        je le marque « prêté »
Alors        je peux saisir un prénom (facultatif) et la date est enregistrée
Et           l'étagère montre l'album comme prêté, avec le prénom en vue liste
Et           un écran « Mes albums prêtés » les regroupe, du plus ancien au plus récent
```
```
Étant donné  un album prêté depuis plus de 3 mois
Quand        j'ouvre l'écran des prêts
Alors        il apparaît en tête, avec la durée en clair : « Prêté à Michel, il y a 4 mois »
Et           aucune notification n'est envoyée — c'est un aide-mémoire, pas un huissier
```

### US-5.2 — Ce que je veux, sans l'avoir raté
```
Étant donné  une série que je ne collectionne pas encore
Quand        je l'ajoute à mes souhaits depuis le catalogue
Alors        elle apparaît dans « Mes envies », séparée de ma collection
Et           elle n'est comptée ni dans mes albums, ni dans mes trous
```
> Distinction structurante : un **trou** est un album d'une série que je collectionne.
> Une **envie** est un album que je n'ai pas commencé. Les mélanger fausserait les
> compteurs de complétion, qui sont le cœur de la valeur perçue.

### US-5.3 — La liste pour le libraire
```
Étant donné  ma liste d'achats
Quand        je choisis « Partager »
Alors        trois formats me sont proposés : texte simple · fichier · QR à montrer
Et           le texte est lisible tel quel dans n'importe quelle messagerie
Et           il ne contient aucun lien, aucun identifiant, aucune adresse
```

### US-5.4 — La liste pour la famille
```
Étant donné  que je veux dire à mes proches quoi m'offrir
Quand        je partage ma liste « pour offrir »
Alors        le texte est formulé pour un tiers, pas pour moi :
             « Ce qu'il manque à la collection de Michel »
Et           il est groupé par série, avec les numéros de tome et les titres
Et           il tient dans un message, sans pièce jointe
```

### US-5.5 — Recevoir une liste
```
Étant donné  un fichier de liste reçu de quelqu'un d'autre
Quand        je l'ouvre avec l'application
Alors        je la consulte comme une liste de courses
Et           elle n'est JAMAIS fusionnée avec ma propre collection
Et           je peux cocher les albums achetés, localement, sans rien renvoyer
```

---

## 4. Conception technique

### 4.1 Modification de schéma — migration v3 → v4

```sql
ALTER TABLE albums ADD COLUMN lentTo TEXT DEFAULT NULL;
ALTER TABLE albums ADD COLUMN lentAt INTEGER DEFAULT NULL;
ALTER TABLE albums ADD COLUMN wishlisted INTEGER NOT NULL DEFAULT 0;
```

- `ReadStatus.LENT` existe déjà : ces colonnes le **complètent**, sans changer sa
  sémantique ni casser l'existant.
- `lentTo` est facultatif : prêter sans nommer reste possible, et c'est le cas courant.
- Cohérence : `lentTo` et `lentAt` ne sont significatifs que si `readStatus == LENT`.
  Vérifié dans `SnapshotValidation`, pas par une contrainte SQL — la souplesse de la
  base protège les imports.
- Un album `wishlisted = 1` est exclu de tous les compteurs de collection et de trous.

### 4.2 Format de liste partagée

**Texte** — évolution de `toShareText()` existant, avec un en-tête adapté au destinataire :

```
Ce qu'il manque à la collection de Michel

Thorgal
  - Tome 21 : La Couronne d'Ogotaï
  - Tome 37 : L'Ermite de Skellingar

Blake et Mortimer
  - Tome 12 : Les 3 Formules du professeur Satō

Liste du 14 novembre 2026 — 3 albums
```

**Fichier** `.bdliste` (JSON, extension propre pour l'ouverture par association) :

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

**Interdits absolus** dans ce fichier, vérifiés par test automatisé :
- aucun identifiant d'appareil, d'installation ou d'utilisateur ;
- aucune URL ;
- aucun horodatage plus précis que le jour (une heure précise est une donnée d'usage) ;
- aucun élément de collection au-delà des albums listés.

**QR** : le texte simple, jusqu'à 12 albums (limite de densité lisible). Au-delà,
l'application propose le fichier et l'explique.

### 4.3 Écran « Mes envies »

Réutilise `buildShoppingList()` avec une source supplémentaire, sans altérer sa
sémantique actuelle :

```kotlin
fun buildShoppingList(
    series: List<Series>,
    albums: List<Album>,
    releasedUnowned: List<ReleaseWithOwnership>,
    wishlisted: List<Album> = emptyList(),   // nouveau, par défaut vide
): List<ShoppingGroup>
```

Le paramètre par défaut garantit la non-régression : les tests existants
(`ShoppingListTest`, 123 lignes) passent sans modification.

### 4.4 Comportements en erreur

| Situation | Comportement |
|---|---|
| Fichier de liste corrompu | Message clair, aucun effet sur la collection |
| Liste d'une version de format plus récente | Refus explicite, pas d'interprétation partielle |
| Partage sans application de messagerie | Sélecteur système vide géré, copie dans le presse-papiers proposée |
| Liste de 300 albums | QR refusé avec explication, texte et fichier proposés |
| Prénom d'emprunteur très long | Tronqué à l'affichage, jamais en base |

---

## 5. Interface & accessibilité

### 5.1 Écran des prêts

```
┌──────────────────────────────────┐
│  Mes albums prêtés          (3)  │
│                                  │
│  Thorgal — Tome 12               │
│  Prêté à Michel, il y a 4 mois   │
│  [ Il me l'a rendu ]             │
│                                  │
│  XIII — Tome 5                   │
│  Prêté, il y a 3 semaines        │
│  [ Il me l'a rendu ]             │
└──────────────────────────────────┘
```

- Durées **relatives et en toutes lettres** : « il y a 4 mois », jamais « 2026-07-14 ».
- « Il me l'a rendu » plutôt que « Marquer comme retourné » : on parle comme les gens.
- Aucune notification, aucun rappel automatique : un aide-mémoire consulté, pas subi.

### 5.2 Partage — le choix du format

```
┌──────────────────────────────────┐
│  Partager ma liste               │
│                                  │
│  ┌────────────────────────────┐  │
│  │ 💬  Par message            │  │
│  │     Texte lisible partout  │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ 📱  Montrer un QR          │  │
│  │     Au libraire, en direct │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ 📄  Fichier                │  │
│  │     Pour une longue liste  │  │
│  └────────────────────────────┘  │
│                                  │
│  Rien n'est envoyé sur internet. │
│  Tu partages un texte, c'est tout│
└──────────────────────────────────┘
```

La dernière phrase est spécifiée telle quelle : c'est la garantie ADR-008 rendue
lisible par la personne concernée.

**Exigences d'accessibilité**
- Le QR affiché a un équivalent textuel accessible (le texte est lisible sous le code).
- Luminosité maximale forcée pendant l'affichage du QR (lisibilité en librairie).
- Cartes de format ≥ 88 dp, description vocale complète.

---

## 6. Plan de test

### Tests unitaires
- `ShoppingListTest` **étendu** — souhaits inclus, appel sans le paramètre inchangé
  (non-régression stricte).
- `ShareTextTest` — en-têtes selon le destinataire, groupement, tomes nuls, liste vide.
- `ShareFileTest` — **absence d'identifiant, d'URL et d'horodatage précis** dans la
  sortie. Test exécutable par la CI : c'est la garantie ADR-008 rendue vérifiable.
- `LoanDurationTest` — formulations relatives, bornes (aujourd'hui, 1 jour, 1 mois, 13 mois).

### Tests instrumentés
- `LoanFlowTest` — prêter, nommer, retrouver, rendre.
- `ShareFlowTest` — les trois formats, sélecteur système.
- `ReceiveListTest` — ouverture d'un `.bdliste`, **aucune fusion avec la collection**.

### Test de terrain
- Deux libraires du panel reçoivent une liste par message et par QR : lisible et
  exploitable sans explication.

---

## 7. Risques

| # | Risque | Titulaire | Parade |
|---|---|---|---|
| Q1 | Le partage appelle naturellement « et si le destinataire pouvait cocher et me le renvoyer ? » — pente vers un service. | Moi | [ADR-008](../DECISIONS.md#adr-008) opposable. Toute réouverture exige un ADR contresigné par la direction. |
| Q2 | Le format `.bdliste` peut devenir un vecteur d'import non contrôlé. | CHARPENTE | Lecture seule, jamais de fusion automatique, validation stricte du format. |
| Q3 | Les prêts sans emprunteur nommé rendent l'écran peu utile. | LISIBILITÉ | Nommer est proposé mais jamais obligatoire ; test d'usage pour arbitrer la formulation. |

---

## 8. Ce qui prouve que c'est terminé

- [ ] Un album prêté à Michel il y a 4 mois est retrouvable en deux taps.
- [ ] Une liste partagée est lisible telle quelle dans une messagerie, sans lien.
- [ ] Le fichier partagé ne contient aucun identifiant, vérifié par test automatisé.
- [ ] Une liste reçue ne modifie jamais la collection de celui qui la reçoit.
- [ ] Les envies ne sont comptées ni dans les albums, ni dans les trous.
- [ ] Un libraire exploite la liste sans explication préalable.
</content>

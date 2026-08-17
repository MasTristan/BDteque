# E8 — DISTRIBUTION & MARQUE

> Faire sortir le produit d'un artefact de CI.
> **6 sprints-personne · H1 (sprint 6) → H2 (sprint 12) · pilote : FILET, avec BOUSSOLE et GARDE-FOU**
> Décisions applicables : [ADR-011](../DECISIONS.md#adr-011), [ADR-012](../DECISIONS.md#adr-012)

---

## 1. Pourquoi

L'application n'a **aucun canal de distribution**. Le seul artefact produit est un APK
de debug déposé par GitHub Actions. `versionCode` vaut 1. Il n'existe ni compte
développeur, ni signature de release, ni fiche.

Trois sujets s'y rattachent, dont deux peuvent bloquer une publication :

- **Le nom.** Le wordmark affiché emploie un terme étroitement associé à la marque du
  principal concurrent du secteur (dette D9, [ADR-011](../DECISIONS.md#adr-011)).
- **La déclaration Data Safety.** Une déclaration inexacte entraîne un retrait de Play.
  La nôtre sera la plus simple possible — *aucune donnée collectée* — mais il faut la
  **prouver**, y compris pour les téléchargements de couvertures et de catalogue.
- **L'accessibilité comme argument**, pas seulement comme conformité. Nous serons
  probablement la seule application de collection du store à pouvoir l'écrire sans mentir.

---

## 2. Périmètre

### Dans le périmètre
- Compte développeur, signature, pistes de publication.
- Fiche store française : textes, captures, vidéo courte.
- Déclaration Data Safety, politique de confidentialité, mentions de licence.
- Publication F-Droid (build reproductible).
- Décision de nom et renommage éventuel.

### Hors périmètre
- Publicité payante (aucun budget, et contraire au positionnement).
- Achats intégrés ([ADR-012](../DECISIONS.md#adr-012) : gratuit en v2).
- Boutiques alternatives au-delà de F-Droid.
- Autres langues ([ROADMAP §6](../ROADMAP.md)).

---

## 3. Récits utilisateur

### US-8.1 — Trouver et comprendre l'application
```
Étant donné  quelqu'un cherchant « collection BD » sur le store
Quand        il tombe sur notre fiche
Alors        la première phrase dit ce que fait l'application, pas ce qu'elle est
Et           les trois premières captures montrent : l'étagère, le verdict, la liste d'achats
Et           « fonctionne sans internet » et « aucun compte » sont visibles sans dérouler
```

### US-8.2 — Une promesse vérifiable
```
Étant donné  la section Sécurité des données de la fiche
Quand        un utilisateur la consulte
Alors        elle indique qu'aucune donnée n'est collectée ni partagée
Et           cette déclaration est exacte, y compris pour le catalogue et les couvertures
Et           la politique de confidentialité tient en une page lisible
```

### US-8.3 — Installer sans compte Google
```
Étant donné  un utilisateur refusant les services Google
Quand        il cherche l'application sur F-Droid
Alors        il la trouve, construite de façon reproductible depuis les sources
Et           elle fonctionne à l'identique
```

---

## 4. Conception technique

### 4.1 Publication

| Élément | Décision |
|---|---|
| Identifiant | `com.bdshelf.app` — **inchangé** : le modifier casserait les installations existantes pour un bénéfice nul |
| Format | AAB pour Play, APK universel pour F-Droid |
| Signature | Clé de release en secret de CI, sauvegarde hors ligne, procédure de perte écrite |
| `versionCode` | Dérivé du numéro de build CI, jamais saisi à la main |
| Pistes | interne (12 testeurs) → fermée (50) → ouverte |
| minSdk | 26 — conservé : notre cible garde ses téléphones longtemps |

### 4.2 Data Safety — l'inventaire des flux

Chaque flux réseau doit être justifié dans le formulaire. Inventaire exhaustif :

| Flux | Données émises | Déclaration |
|---|---|---|
| Téléchargement du catalogue | Aucune donnée utilisateur — requête de fichier statique | Non collectée |
| Téléchargement des sorties | Idem | Non collectée |
| Couverture d'album (opt-in) | L'ISBN scanné, vers Open Library | **Signalé dans la fiche**, opt-in, désactivé par défaut |
| Recherche en ligne (opt-in, [E2](E2-scan.md)) | L'ISBN scanné, vers BnF / Open Library | **Signalé**, opt-in, désactivé par défaut |
| Rapport d'incident | Rien d'automatique ; envoi manuel par l'utilisateur | Non collectée |

**Conséquence majeure de [E1](E1-catalogue.md)** : une fois le catalogue local en place,
les deux seuls flux portant un ISBN deviennent optionnels et désactivés par défaut.
La déclaration « aucune donnée collectée » devient **vraie sans réserve** dans la
configuration par défaut. C'est un argument de fiche store, pas seulement une case à cocher.

### 4.3 Licences et mentions

Écran À propos, obligatoire ([ADR-004](../DECISIONS.md#adr-004)) :

```
Les données du catalogue proviennent du Catalogue général de la
Bibliothèque nationale de France, sous Licence Ouverte Etalab 2.0.
Dernière mise à jour de la source : 28 octobre 2026.

Polices : Atkinson Hyperlegible (Braille Institute, SIL OFL),
Fraunces (SIL OFL).

Application publiée sous licence MIT. Code source disponible.
```

La dédicace historique — *« Développé avec soin par {fils}, pour {Prénom} »* — est
**conservée**. Elle dit d'où vient ce produit, et c'est vrai.

### 4.4 Décision de nom

État : [ADR-011](../DECISIONS.md#adr-011), **proposée**, décision au sprint 6.

| Élément | Aujourd'hui | Après décision |
|---|---|---|
| Dépôt | `BDteque` | inchangé |
| Identifiant | `com.bdshelf.app` | inchangé |
| Nom store | *(inexistant)* | à décider |
| Wordmark | « La Bédéthèque de {Prénom} » | recommandé : « L'Étagère de {Prénom} » |

**Recommandation** : « L'Étagère de {Prénom} ». Cohérent avec la métaphore centrale du
produit, plus court, plus chaleureux, sans homonymie sectorielle. Conditionné à la
recherche d'antériorité INPI (GARDE-FOU, rendue avant le sprint 6).

Un changement de wordmark touche `strings.xml` et l'écran d'accueil. Coût technique
négligeable **avant** publication, coûteux après — d'où l'échéance.

---

## 5. Fiche store

### Titre et accroche

```
Titre        : L'Étagère — ma collection de BD
Accroche     : Sais si tu l'as déjà, même sans réseau.
```

L'accroche décrit **l'usage**, pas le produit. C'est la phrase que les gens du panel
ont employée pour décrire ce qu'ils cherchaient.

### Description (structure imposée)

1. Le problème, en une phrase, avec les mots des utilisateurs.
2. Ce que fait l'application, en cinq points concrets.
3. Ce qu'elle **ne fait pas** : pas de compte, pas de publicité, pas de collecte, pas
   d'abonnement. Cette section est un argument de vente, placée haut, pas en bas de page.
4. L'accessibilité, nommée : grands caractères, lecteur d'écran, langage simple.
5. Origine du projet : une application écrite par un fils pour son père.

Le point 5 n'est pas un ornement. C'est l'élément que les utilisateurs du panel ont
retenu et répété quand on leur a décrit le produit.

### Captures (8, dans cet ordre)

1. L'étagère d'une série, avec ses trous
2. Le verdict « Tu l'as déjà »
3. Le verdict « Il te manque »
4. Le scanner en usage
5. La liste d'achats prête à partager
6. Le mur d'étagères
7. Les réglages de sauvegarde
8. L'application en grands caractères (`fontScale 2.0`) — **volontairement présente** :
   elle s'adresse directement à ceux qui en ont besoin

---

## 6. Plan de test

- **Contrôle Data Safety** : capture réseau complète sur un parcours de 30 minutes,
  réglages par défaut. **Attendu : uniquement le téléchargement du catalogue.** Toute
  autre requête est un défaut bloquant.
- **Build reproductible F-Droid** : deux constructions indépendantes produisent des
  artefacts identiques.
- **Test de la fiche** : 5 personnes hors panel lisent la fiche et décrivent ce que fait
  l'application. Réussite si 4 sur 5 mentionnent le scan et l'absence de compte.
- **Test d'installation** : parcours complet depuis le store sur les deux appareils de
  référence, sans compte Google actif pour la variante F-Droid.

---

## 7. Risques

| # | Risque | Titulaire | Parade |
|---|---|---|---|
| Q1 | Recherche d'antériorité défavorable → renommage tardif. | GARDE-FOU | Échéance sprint 6, avant toute publication. Le nom de repli est déjà choisi. |
| Q2 | Refus de Play pour déclaration Data Safety inexacte. | FILET | Inventaire des flux vérifié par capture réseau, pas par relecture de code. |
| Q3 | Build reproductible F-Droid difficile à obtenir (dépendances non déterministes). | FILET | Versions épinglées — déjà le cas dans `libs.versions.toml`. Vérification tôt, dès H1. |
| Q4 | Fiche noyée dans une catégorie saturée. | BOUSSOLE | Positionnement sur des requêtes précises (« collection BD hors ligne », « scanner ISBN BD ») plutôt que sur des termes génériques. |

---

## 8. Ce qui prouve que c'est terminé

- [ ] Une release signée s'obtient depuis un tag, en une commande.
- [ ] L'application est installable depuis Play et depuis F-Droid.
- [ ] Une capture réseau de 30 minutes ne montre que le téléchargement du catalogue.
- [ ] La déclaration « aucune donnée collectée » est exacte et vérifiée.
- [ ] La recherche d'antériorité est rendue et la décision de nom prise.
- [ ] 4 personnes sur 5 comprennent le produit à la seule lecture de la fiche.
- [ ] La dédicace d'origine figure toujours dans l'écran À propos.
</content>

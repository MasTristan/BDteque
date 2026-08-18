# DÉCISIONS D'ARCHITECTURE (ADR)

> Journal des décisions structurantes. Une décision absente de ce fichier n'existe pas.
> Format : contexte → décision → conséquences → alternatives écartées.
> Statuts : `Acceptée` · `Proposée` · `Remplacée par ADR-nnn` · `Obsolète`.

| # | Titre | Statut | Date | Réunion |
|---|---|---|---|---|
| [001](#adr-001) | Faire évoluer l'existant, ne pas réécrire | Acceptée | 2026-09-01 | R0 |
| [002](#adr-002) | Extraire `:core:domain` en Kotlin pur, et rien d'autre | Acceptée | 2026-09-25 | R3 |
| [003](#adr-003) | Le catalogue vit dans une base Room séparée et jetable | Acceptée | 2026-09-25 | R3 |
| [004](#adr-004) | Le référentiel distribué ne contient que des données BnF | Acceptée | 2026-10-02 | R4 |
| [005](#adr-005) | Cinq portes de qualité bloquantes | Acceptée | 2026-10-09 | R3/R6 |
| [006](#adr-006) | Les couvertures restent locales et opt-in | Acceptée | 2026-10-06 | R5 |
| [007](#adr-007) | Toute donnée téléchargée est signée et vérifiée | Acceptée | 2026-10-02 | R4 |
| [008](#adr-008) | Le partage sort du téléphone sous forme inerte | Acceptée | 2026-09-18 | R2 |
| [009](#adr-009) | L'étagère a une vue liste équivalente pour tous | Acceptée | 2026-10-06 | R5 |
| [010](#adr-010) | Rapports d'incident locaux et manuels, aucun SDK | Acceptée | 2026-10-09 | R6 |
| [011](#adr-011) | Nom du produit : décision suspendue à l'antériorité | Proposée | 2026-10-02 | R4 |
| [012](#adr-012) | Gratuit sans publicité en v2 ; payant une fois réexaminé en v3 | Acceptée | 2026-10-13 | R7 |

---

## ADR-001
### Faire évoluer l'existant, ne pas réécrire

**Statut** : Acceptée · 2026-09-01 · R0

**Contexte.** Le projet arrive avec 8 095 lignes de Kotlin applicatif, 13 classes de
tests sur le domaine pur, 12 écrans fonctionnels et une architecture MVVM + Repository
tenue sans exception. L'arrivée d'un budget et d'une équipe pose classiquement la
question de la remise à plat.

**Décision.** Aucune réécriture. Le socle est conservé, étendu et solidé.

**Justification.** Ce qui manque au projet n'est pas de la qualité de code : c'est de
la **donnée** (catalogue), une **chaîne de livraison** (release) et des **preuves**
(tests instrumentés, migrations testées). Une réécriture ne produit aucun des trois et
détruit un actif rare : la gestion systématique des cas dégradés (hors-ligne, disque
plein, permission refusée, JSON malformé), présente partout dans le code actuel.

**Conséquences.**
- Les conventions existantes deviennent la référence : chaînes de caractères
  exclusivement dans `strings.xml`, un `StateFlow<UiState>` par ViewModel, jamais de
  DAO dans un écran, jamais de logique métier dans un composable.
- La règle « la collection et les sorties ne se mélangent jamais » est étendue au
  catalogue (ADR-003).
- Toute nouvelle dépendance doit être justifiée en comité d'architecture.

**Alternatives écartées.** Réécriture complète (perte de trois ans d'attention aux
détails) · réécriture progressive par module (complexité sans bénéfice à cette taille).

---

## ADR-002
### Extraire `:core:domain` en Kotlin pur, et rien d'autre

**Statut** : Acceptée · 2026-09-25 · R3

**Contexte.** Le module unique atteindra ses limites vers 15 000 lignes. La logique
métier (`GapDetector`, `ShoppingList`, `Ean`, `TextNormalization`, `BnfUnimarcParser`,
`CollectionExport`, `SnapshotValidation`…) est déjà pure et testée en JVM, mais vit dans
le même module que l'interface, et importe les entités Room.

**Décision.** Un seul découpage : un module `:core:domain` en Kotlin/JVM pur, sans
aucune dépendance Android ni Room. Aucun autre module.

**Conséquences.**
- Les entités Room ne peuvent plus être des paramètres de fonctions de domaine : le
  domaine définit ses propres modèles, la couche données convertit. Coût réel de
  refonte, assumé, chiffré à 2 personne-sprints.
- Les tests de domaine s'exécutent sans SDK Android — quelques secondes.
- La logique métier devient **structurellement impossible** à polluer d'appels
  Android : la contrainte est appliquée par le compilateur, pas par la revue de code.
- Une éventuelle version iOS (KMP) reste ouverte sans coût aujourd'hui.

**Alternatives écartées.** Cinq modules (`data`, `domain`, `ui`, `catalog`, `scan`) :
temps de build et complexité disproportionnés pour six personnes · statu quo : ne
résout pas le couplage domaine/Room.

---

## ADR-003
### Le catalogue vit dans une base Room séparée et jetable

**Statut** : Acceptée · 2026-09-25 · R3

**Contexte.** Le pari n°1 introduit un référentiel de l'ordre de 35 000 albums. Le
projet applique jusqu'ici une règle stricte : la collection (Room) et les sorties
(fichier JSON en cache) ne se mélangent jamais.

**Décision.** Le catalogue est une **troisième moitié** : une base Room distincte
(`catalog.db`), en lecture seule pour l'application, remplaçable en bloc, jamais
sauvegardée, jamais exportée.

```
Moitié A — collection   Room bdshelf.db   propriété de l'utilisateur   irremplaçable   sauvegardée
Moitié B — sorties      JSON filesDir     donnée curée                 jetable         non sauvegardée
Moitié C — catalogue    Room catalog.db   donnée publique              jetable         non sauvegardée
```

**Règle absolue.** Aucune clé étrangère, aucune jointure SQL et aucune transaction ne
traverse la frontière A/C. Les liens se font en mémoire, dans le domaine, par
identifiant — exactement comme la liste d'achats croise déjà A et B aujourd'hui.

**Conséquences.**
- Supprimer ou remplacer le catalogue ne peut, par construction, pas toucher un album
  de l'utilisateur.
- La sauvegarde de la collection reste petite (quelques centaines de Ko), inchangée.
- `Series` gagne une colonne `catalogSeriesId` (nullable) : un simple lien, non
  contraint, qui peut pointer dans le vide sans rien casser.
- Deux instances Room coexistent dans `BdShelfApplication`.

**Alternatives écartées.** Catalogue dans la base existante (une CASCADE mal placée
détruit la collection ; sauvegardes énormes) · catalogue en JSON dans `filesDir`
(plusieurs secondes de chargement, incompatible avec le pilier P1).

---

## ADR-004
### Le référentiel distribué ne contient que des données BnF

**Statut** : Acceptée · 2026-10-02 · R4

**Contexte.** Deux sources de données bibliographiques sont accessibles librement :
le catalogue BnF (Licence Ouverte **Etalab 2.0**) et Open Library (**ODbL**). L'ODbL
impose la réciprocité : toute base dérivée publiée doit l'être sous la même licence.

**Décision.**
1. Le bundle catalogue distribué est construit **exclusivement** à partir de données BnF.
2. Open Library reste utilisable **à l'exécution, sur l'appareil de l'utilisateur**,
   en repli opt-in — usage individuel qui ne produit aucune base dérivée distribuée.
3. La mention de source et la date de dernière mise à jour figurent dans l'écran
   À propos, comme l'exige Etalab 2.0.

**Conséquences.**
- Notre référentiel reste sous notre maîtrise, sans obligation de réciprocité.
- Le taux de couverture dépendra de la qualité du dépôt légal — mesuré, pas supposé :
  corpus de 500 EAN réels, seuil d'acceptation 90 %.
- Le pipeline de construction interdit techniquement l'ingestion d'une source non
  autorisée : la liste des sources est déclarative et vérifiée en CI du pipeline.

**Alternatives écartées.** Bundle mixte BnF + Open Library (contamination ODbL pour un
apport marginal) · publication du référentiel en ODbL (engagement perpétuel non
nécessaire) · moissonnage de sites tiers (interdit par la spec fondatrice, contraire
aux CGU, techniquement fragile).

---

## ADR-005
### Cinq portes de qualité bloquantes

**Statut** : Acceptée · 2026-10-09 · R3 + R6

**Contexte.** Le projet n'a aucun test instrumenté, ses schémas Room ne sont pas
versionnés, et une migration déjà livrée n'a jamais été vérifiée contre son schéma de
référence. La cible d'utilisateurs supporte mal l'incident et ne sait pas le signaler.

**Décision.** Cinq portes bloquent toute publication. Aucune n'est consultative.

| Porte | Critère |
|---|---|
| G1 — Tests unitaires | Suite JVM verte |
| G2 — Parcours critiques | 5 tests instrumentés verts : premier lancement · scan → verdict → ajout · trous d'une série · export puis import · mise à jour du catalogue |
| G3 — Accessibilité | Aucune anomalie bloquante détectée automatiquement ; rendus capturés à `fontScale` 1,0 / 1,5 / 2,0 |
| G4 — Performance | Démarrage à froid < 1,2 s · verdict p95 < 1,5 s hors-ligne · APK < 30 Mo hors catalogue |
| G5 — Migration | Test de migration vert pour chaque version de base, de 1 à N, sur schémas versionnés |

**Conséquences.**
- `app/schemas/` entre au dépôt, rétroactivement pour les versions 1 et 2.
- La source `androidTest` est créée au sprint 1.
- Le contournement d'une porte exige un ADR nominatif, daté et signé. La trace publique
  est le garde-fou ; l'interdiction absolue serait contournée en silence.

---

## ADR-006
### Les couvertures restent locales et opt-in

**Statut** : Acceptée · 2026-10-06 · R5

**Contexte.** La spécification fondatrice interdisait les couvertures (« zéro image
réseau »). L'équipe précédente a levé cette interdiction avec un dispositif prudent :
téléchargement opt-in au moment du scan uniquement, stockage local, écriture atomique,
aucune image réseau à l'affichage.

**Décision.** L'assouplissement est **confirmé** et encadré :
- Réglage désactivé par défaut, formulé en langage clair.
- Téléchargement uniquement au moment d'une identification, jamais au parcours.
- Servi exclusivement depuis le stockage local ; aucun chargeur d'images réseau
  (pas de Coil, pas de Glide).
- Les couvertures ne sont **jamais** incluses dans le bundle catalogue (poids, et
  droits patrimoniaux distincts des données bibliographiques).
- Purge possible depuis les réglages, avec la taille occupée affichée.

**Justification.** La règle réelle n'était pas « pas d'images » mais « pas de
dépendance réseau à l'affichage, pas de collecte ». Le dispositif actuel respecte les
deux. Une couverture améliore mesurablement la confiance dans le verdict — l'utilisateur
vérifie d'un coup d'œil que c'est le bon livre.

---

## ADR-007
### Toute donnée téléchargée est signée et vérifiée

**Statut** : Acceptée · 2026-10-02 · R4

**Contexte.** L'application télécharge déjà un `releases.json` depuis une URL
**surchargeable dans les réglages**, sans aucune vérification d'intégrité. Le catalogue
ajoutera un artefact bien plus gros, qui remplit une base locale.

**Décision.** Tout artefact de données téléchargé est signé **Ed25519**. La clé
publique est intégrée à l'application. La vérification précède l'import.

- Signature invalide, absente ou clé inconnue → **rejet silencieux**, conservation de
  l'artefact précédent, information non bloquante dans l'écran concerné.
- Rétroactif sur `releases.json`, avec période de tolérance d'une version pour ne pas
  casser les installations existantes.
- L'URL reste surchargeable (réglage avancé), mais la signature n'est jamais
  contournable : changer d'URL ne permet pas d'injecter des données arbitraires.
- Rotation de clé prévue : deux clés publiques acceptées simultanément pendant la
  transition.

**Conséquences.** Le pipeline de publication détient une clé privée, gérée comme un
secret de release au même titre que la clé de signature de l'APK.

---

## ADR-008
### Le partage sort du téléphone sous forme inerte

**Statut** : Acceptée · 2026-09-18 · R2

**Contexte.** La recherche utilisateur montre que le besoin est en partie relationnel :
11 personnes sur 15 rapportent qu'un proche veut savoir quoi leur offrir. La tentation
naturelle est un lien partagé, donc un service, donc à terme un compte.

**Décision.** Ce qui sort du téléphone est **inerte** : du texte lisible, ou un fichier
autonome. Jamais un lien vers un service, jamais un identifiant, jamais un canal de
retour.

**Règles.**
- Aucun identifiant d'appareil, d'utilisateur ou d'installation dans un artefact partagé.
- Aucune URL pointant vers une infrastructure que nous contrôlons.
- Le destinataire n'a besoin de rien installer : le texte partagé se lit dans n'importe
  quelle messagerie.
- Un artefact partagé est une **photographie**, pas un abonnement : il ne se met pas à
  jour, et c'est voulu.

**Conséquences.** Pas de synchronisation familiale, pas de liste vivante partagée.
Assumé : ces fonctionnalités exigeraient un service et un compte, ce qui contredit le
pilier P3.

---

## ADR-009
### L'étagère a une vue liste équivalente pour tous

**Statut** : Acceptée · 2026-10-06 · R5

**Contexte.** L'étagère de tranches en défilement horizontal est la signature visuelle
du produit. C'est aussi le pire schéma d'interaction pour un lecteur d'écran : une série
de 36 tomes devient un tunnel sans repère. Les états (possédé, manquant, lu, prêté) sont
en outre codés uniquement par la couleur et la forme.

**Décision.** Une **vue liste équivalente**, offrant strictement les mêmes actions,
accessible à tous les utilisateurs via un basculement visible et persistant — pas un
mode caché réservé à l'accessibilité.

- La liste affiche numéro, titre et état **en toutes lettres**.
- L'étagère gagne des descriptions d'état textuelles et un accès direct au tome N.
- La préférence est mémorisée.
- Le basculement est offert dans l'interface principale, sans stigmatisation.

**Justification** (BOUSSOLE, R2) : *« les personnes qui voient mal ne veulent pas un
mode handicap, elles veulent le mode qui marche pour elles. »* Une vue liste sert aussi
les collections de 60 tomes, où l'étagère devient de toute façon peu maniable.

**Alternatives écartées.** Supprimer l'étagère (détruit l'identité du produit) ·
rustine TalkBack sur l'étagère (conformité de façade, expérience réelle mauvaise).

---

## ADR-010
### Rapports d'incident locaux et manuels, aucun SDK

**Statut** : Acceptée · 2026-10-09 · R6

**Contexte.** Sans télémétrie, un plantage chez un utilisateur est invisible. Les
solutions du marché (Crashlytics, Sentry) embarquent un identifiant d'installation et
un envoi automatique.

**Décision.** Aucun SDK de collecte, y compris « anonymisé ».
L'application écrit l'exception dans un fichier local, sans identifiant. Au démarrage
suivant : proposition explicite d'envoyer le rapport, **contenu visible avant envoi**,
partage manuel via le sélecteur système.

**Conséquences.**
- Taux de retour faible, assumé et compensé par le panel utilisateur (retour sous 48 h).
- Le formulaire Data Safety de Play peut déclarer **aucune donnée collectée** — et ce
  sera vrai, ce qui est un argument de fiche store, pas seulement une conformité.
- La qualité repose davantage sur la prévention (portes G1–G5) que sur la détection.
  C'est plus coûteux en amont et c'est le bon compromis pour ce produit.

---

## ADR-011
### Nom du produit : décision suspendue à la recherche d'antériorité

**Statut** : **Proposée** — décision attendue au sprint 6 · 2026-10-02 · R4

**Contexte.** Trois noms coexistent : le dépôt (`BDteque`), l'application (`BDShelf`),
et le wordmark affiché (« La Bédéthèque de {Prénom} »). Ce dernier emploie un terme
étroitement associé à `bedetheque.com`, propriété de l'éditeur de BDGest, acteur majeur
du secteur et concurrent direct.

**Analyse préliminaire (GARDE-FOU).** « Bédéthèque » est un nom commun attesté en
français, ce qui affaiblit toute revendication exclusive. Mais l'usage en tant que
**nom de produit dans le même secteur d'activité** que le titulaire est précisément le
cas où le risque se matérialise. Le risque est faible en probabilité, élevé en impact :
un retrait du store après publication coûte bien plus qu'un renommage avant.

**Options.**
1. Conserver le wordmark actuel après recherche d'antériorité favorable.
2. Renommer le wordmark en « L'Étagère de {Prénom} » — cohérent avec la métaphore
   centrale du produit, plus court, plus chaleureux, sans homonymie sectorielle.
3. Renommer intégralement le produit.

**Recommandation.** Option 2, sous réserve de la recherche INPI. Le mot « étagère » est
déjà l'idée directrice du produit ; le wordmark y gagne en justesse. Le dépôt garde son
nom (`BDteque`), l'identifiant d'application reste `com.bdshelf.app` — le changer
casserait les installations existantes pour un bénéfice nul.

**Échéance.** Recherche d'antériorité rendue avant le sprint 6, décision prise avant
toute publication.

---

## ADR-012
### Gratuit sans publicité en v2 ; payant une fois réexaminé en v3

**Statut** : Acceptée · 2026-10-13 · R7

**Contexte.** La direction attend une trajectoire économique. Trois modèles sont
possibles : publicité, abonnement, achat unique.

**Décision.**
- **v2 (2027) : gratuit, sans publicité, sans achat intégré.** Objectif : constituer une
  base d'usage et une preuve de qualité.
- **v3 : réexamen d'un achat unique**, aux alentours de 4,99 €, avec version d'essai.
- **Jamais** de publicité ni d'abonnement.

**Justification.** La publicité exige un SDK traceur : incompatible avec P3, et
destructrice de l'argument différenciant. L'abonnement exige un compte et une
infrastructure : incompatible avec P2 et P5. L'achat unique est le seul modèle
cohérent avec le produit — on paie l'objet, pas l'accès.

**Conséquences.** La bibliothèque de facturation n'est pas intégrée en v2 : aucune
dépendance de paiement, aucun compte, rien à maintenir. La décision v3 sera prise sur
des données d'usage réelles, pas sur une projection.
</content>

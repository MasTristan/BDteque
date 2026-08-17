# COMITÉS — comptes rendus de la phase de cadrage

> Séquence de 9 réunions, du 2026-09-01 au 2026-10-15, aboutissant au gel des
> spécifications de développement.
> Chaque décision porte une référence `ADR-nnn` documentée dans [DECISIONS.md](DECISIONS.md).
> Format : ordre du jour · **débat** (les arbitrages réels, pas le consensus reconstitué) ·
> décisions · actions.

**Participants** (indicatifs, voir [TEAM.md](TEAM.md)) : CHARPENTE, SOURCE, LISIBILITÉ,
ŒIL, FILET, MAIN, BOUSSOLE, GARDE-FOU. Animation : responsable produit & technique.

---

## R0 — Passation & état des lieux
**2026-09-01 · 3 h · tous (hors ŒIL, LISIBILITÉ, MAIN — pas encore en poste)**

### Objet
Partager l'audit du code existant. Établir ce qui est acquis, ce qui est dette, ce qui
est sacré.

### Débat

**« On repart de zéro sur une base propre ? »** — posé d'entrée par CHARPENTE, après
lecture du code. Réponse argumentée en séance, et refusée : 8 095 lignes, 13 classes de
tests sur le domaine pur, une séparation architecturale tenue sans exception, une
gestion des cas dégradés meilleure que la moyenne de l'industrie. Une réécriture
détruirait trois ans d'attention aux détails pour reconstruire les mêmes bugs. **Ce
qu'il manque n'est pas de la qualité de code, c'est de la donnée et une chaîne de
livraison.**

CHARPENTE maintient une réserve, actée : *« le module unique tiendra jusqu'à ~15 000
lignes, pas au-delà. »* Traité en R3.

**Le point qui a fait taire la salle.** FILET ouvre `BackupManager.kt` en séance :
les sauvegardes automatiques quotidiennes vivent dans `filesDir`. Désinstaller
l'application efface les sept sauvegardes en même temps que la collection. Le filet de
sécurité ne couvre pas le cas de perte le plus fréquent. La documentation du fichier
est honnête à ce sujet, mais l'utilisateur, lui, croit être protégé.

Décidé sur-le-champ : **premier ticket du projet**, avant tout travail de vision.

### Décisions
- **D-R0-1** — Pas de réécriture. On solde, on étend. *(→ [ADR-001](DECISIONS.md))*
- **D-R0-2** — Dette D1 (sauvegardes) traitée au sprint 1, en tête de backlog.
- **D-R0-3** — Les cinq piliers de la vision sont **opposables** : toute violation exige
  un ADR contresigné par la direction.
- **D-R0-4** — `SPEC.md` et `CLAUDE.md`, retirés du dépôt en juillet, sont restaurés
  comme documents publics sous `docs/`. Un projet financé ne peut pas avoir sa
  spécification dans l'historique git d'un fichier supprimé.

### Actions
| # | Action | Qui | Échéance |
|---|---|---|---|
| A1 | Ticket sauvegardes SAF (D1) rédigé et estimé | MAIN | 2026-09-15 |
| A2 | Audit de dette chiffré, D1→D9 | CHARPENTE + FILET | 2026-09-08 |
| A3 | Corpus de 500 EAN réels à collecter en librairie | BOUSSOLE | 2026-09-30 |

---

## R1 — Vision & positionnement
**2026-09-04 · 2 h · CHARPENTE, SOURCE, FILET, BOUSSOLE, GARDE-FOU**

### Objet
Arrêter la thèse produit avant d'écrire la moindre spécification.

### Débat

**Le désaccord fondateur.** BOUSSOLE défend un élargissement : « manga et comics
représentent plus de la moitié du marché BD français en volume ; se limiter au
franco-belge, c'est se priver des moins de 40 ans. » SOURCE tempère : le dépôt légal
BnF couvre les trois, mais la normalisation des séries manga (numérotation par langue,
éditions VF/VO, rythme de parution mensuel) est un problème d'une autre nature, qui
doublerait le coût du pari n°1.

Arbitrage rendu : **franco-belge d'abord, manga au T3 2027 si le taux de résolution
franco-belge dépasse 90 %.** Motif : notre utilisateur de référence a 70 ans et lit
Blake et Mortimer ; un catalogue à moitié bon sur deux segments vaut moins qu'un
catalogue excellent sur un seul. Le marché manga est mieux servi par les acteurs
existants ; nous n'avons pas d'avantage là-bas.

**La fuite de vie privée.** GARDE-FOU relève que chaque scan envoie l'ISBN à la BnF et
à Open Library. Aucune donnée personnelle au sens du RGPD, mais l'adresse IP corrélée
au flux des requêtes révèle ce qu'une personne achète, où et quand. Face à une promesse
« aucune collecte » affichée dans le README, c'est une contradiction publiable.

Constat clé de la séance, formulé par SOURCE : **le catalogue local ne corrige pas
seulement la complétude, il supprime la fuite.** Télécharger le catalogue entier au
lieu d'interroger un titre à la fois, c'est du *private information retrieval* de
pauvre — et ça marche. Le pari n°1 passe du statut de « meilleure donnée » à celui de
« meilleure donnée **et** meilleure vie privée ».

### Décisions
- **D-R1-1** — Périmètre catalogue v1 : **BD franco-belge**. Manga/comics : réexamen T3 2027.
- **D-R1-2** — Le catalogue local devient la **source primaire** d'identification ; le
  réseau passe en repli explicitement opt-in. *(→ [ADR-003](DECISIONS.md))*
- **D-R1-3** — Les cinq piliers et les trois paris sont adoptés tels que rédigés dans
  [VISION.md](VISION.md).
- **D-R1-4** — Mesure sans télémétrie : panel de 12 personnes + corpus EAN + banc CI.
  Aucun SDK analytique, y compris « anonymisé ».

### Actions
| # | Action | Qui | Échéance |
|---|---|---|---|
| A4 | Note juridique Etalab 2.0 vs ODbL | GARDE-FOU | 2026-09-18 |
| A5 | Mesure du taux de résolution BnF sur 200 séries pilotes | SOURCE | 2026-09-25 |

---

## R2 — Terrain : ce que veulent vraiment les gens
**2026-09-18 · 2 h · tous**

### Objet
Restitution de 15 entretiens (9 collectionneurs de 34 à 78 ans, 2 libraires,
4 proches offrant des BD).

### Ce qui est remonté

1. **« Je ne sais jamais où j'en suis dans une série. »** — 13 mentions sur 15. Attendu.
2. **« J'achète en double une fois par an environ. »** — 9/15. Coût moyen déclaré :
   40–60 € par an. C'est notre proposition de valeur chiffrée.
3. **Inattendu, et le plus important de la séance :** *« le problème, ce n'est pas moi,
   c'est ma femme / mes enfants qui veulent m'offrir un album. »* — 11/15. Le besoin
   n'est pas seulement personnel, il est **relationnel**. Quelqu'un d'autre a besoin de
   savoir ce qu'il me manque, sans installer d'application ni créer de compte.
4. **Les deux libraires** disent la même chose : les clients arrivent avec une capture
   d'écran ou un papier. Ils ne veulent pas d'un système, ils veulent **une liste
   lisible**. La fonction de partage texte existante est déjà la bonne réponse — elle
   est juste enfouie.
5. **Douche froide :** 4 personnes sur 15 ont abandonné une application concurrente
   *« parce qu'il fallait ressaisir 400 albums »*. L'amorçage n'est pas un confort,
   c'est la condition de survie. Confirme le pari n°3.
6. **Le repli sans code-barres** : les 3 personnes de plus de 65 ans possèdent
   majoritairement des éditions d'avant 1990. Une application qui exige un code-barres
   est inutile sur la moitié de leur étagère. Confirme le pari n°2.

### Débat

MAIN propose de faire de la liste d'achats partagée une épopée de premier rang
(constat n°3). LISIBILITÉ objecte : « on va reconstruire un réseau social par la bande —
partage, destinataire, état de lecture croisé, et dans six mois on aura besoin d'un
compte. » Objection retenue, et cadrée : **le partage se fait par fichier ou par texte,
jamais par service.** Ce qui sort du téléphone est inerte : pas de retour, pas d'état
partagé, pas d'identité.

### Décisions
- **D-R2-1** — Le partage de liste passe de « fonctionnalité enfouie » à **objectif de
  premier rang** de E5, sous contrainte de format inerte. *(→ [ADR-008](DECISIONS.md))*
- **D-R2-2** — L'amorçage (E3) est requalifié : critique pour l'acquisition, pas confort.
- **D-R2-3** — Le repli sans code-barres (E2) est confirmé comme condition d'usage
  chez les plus de 65 ans, pas comme raffinement.
- **D-R2-4** — Métrique de valeur affichée en interne : **« doublons évités »**, mesurée
  par déclaratif du panel. Pas de suivi individuel.

---

## R3 — Comité d'architecture
**2026-09-25 · 3 h · CHARPENTE, SOURCE, FILET, MAIN, moi**

### Objet
Trancher la structure du projet, l'emplacement du catalogue, la stratégie de migration.

### Débat

**Point dur n°1 — Où vit le catalogue ?**
Trois options mises sur la table par CHARPENTE :

| Option | Pour | Contre | Verdict |
|---|---|---|---|
| Dans la base Room existante | Simple, une seule base, jointures gratuites | **Viole la règle des moitiés séparées.** Un `DELETE` de catalogue à la CASCADE peut emporter des albums de l'utilisateur. Sauvegarde et restauration deviennent énormes. | **Rejeté** |
| Fichiers JSON dans `filesDir` (comme les sorties) | Cohérent avec l'existant | 35 000 albums en JSON = plusieurs secondes de chargement, incompatible avec le pilier P1 | **Rejeté** |
| **Base Room séparée `catalog.db`, en lecture seule** | Requêtes indexées instantanées, FTS pour la recherche, remplaçable en bloc, jamais mêlée à la collection | Une deuxième instance Room à gérer | **Retenu** |

La règle historique « deux moitiés » devient **trois moitiés** : A la collection
(Room, propriété de l'utilisateur, sauvegardée), B les sorties (fichier JSON en cache),
C le catalogue (Room séparée, jetable, remplaçable, jamais sauvegardée).

Le critère qui a tranché : **ce qui est jetable ne doit jamais toucher ce qui est
irremplaçable.** Une collection est irremplaçable ; un catalogue se retélécharge.

**Point dur n°2 — Modulariser ou pas ?**
CHARPENTE veut découper en cinq modules. FILET s'y oppose au motif du temps de build
et de la complexité pour une équipe de six personnes. Compromis adopté : **un seul
découpage**, l'extraction de `:core:domain` en Kotlin pur sans dépendance Android.
Trois bénéfices concrets, pas théoriques : les tests de domaine tournent en quelques
secondes, la logique métier devient impossible à polluer d'appels Android, et une
éventuelle version iOS reste ouverte sans travail supplémentaire aujourd'hui.

**Point dur n°3 — Les migrations.**
`exportSchema = true` est configuré, mais `app/schemas/` n'est pas dans le dépôt.
Autrement dit, la migration 1→2 déjà livrée **n'a jamais été testée contre son schéma
de référence**. Elle est probablement correcte — écrite à la main, lisible — mais nous
n'en avons aucune preuve. FILET qualifie le risque : *« une migration ratée sur cette
application, ce n'est pas un crash, c'est la collection d'un homme de 70 ans. »*

### Décisions
- **D-R3-1** — Catalogue en base Room **séparée**, lecture seule, jetable. *(→ [ADR-003](DECISIONS.md))*
- **D-R3-2** — Extraction de `:core:domain` en Kotlin pur. Aucun autre découpage. *(→ [ADR-002](DECISIONS.md))*
- **D-R3-3** — `app/schemas/` versionné + test de migration obligatoire pour toute
  version de base, rétroactivement sur 1→2. Porte de qualité bloquante en CI. *(→ [ADR-005](DECISIONS.md))*
- **D-R3-4** — Injection manuelle conservée. Hilt réexaminé au-delà de 15 000 lignes.
- **D-R3-5** — Budgets de performance inscrits dans la CI, échec = build rouge :
  démarrage à froid < 1,2 s, verdict p95 < 1,5 s hors-ligne, APK < 30 Mo hors catalogue.

---

## R4 — Données, licences & signature
**2026-10-02 · 2 h · SOURCE, GARDE-FOU, CHARPENTE, FILET, moi**

### Objet
Sécuriser juridiquement et techniquement le pari n°1.

### Débat

**L'avis de GARDE-FOU (note A4) est sans ambiguïté.**

- Les données du catalogue général de la BnF sont diffusées sous **Licence Ouverte
  Etalab 2.0** : réutilisation libre, y compris commerciale, y compris en base dérivée,
  sous réserve de **mentionner la source et la date de dernière mise à jour**. Aucune
  obligation de réciprocité.
- Open Library est sous **ODbL**. Toute base dérivée incorporant ses données doit être
  publiée sous la même licence. Ce n'est pas rédhibitoire, mais cela nous engagerait à
  publier le référentiel entier en ODbL, à perpétuité, pour un apport marginal.

Décision immédiate et nette : **le bundle est BnF pur.** Open Library reste utilisable
à l'exécution, sur le téléphone de l'utilisateur, en repli opt-in — un usage individuel
qui ne produit pas de base dérivée distribuée.

**Point technique — l'intégrité du bundle.**
CHARPENTE pose la question qui fâche : un fichier téléchargé qui remplit une base
locale, c'est un vecteur d'exécution. Si l'URL est détournée (DNS, dépôt compromis,
réglage « avancé » modifié par un tiers), on injecte des données arbitraires dans
l'application d'un utilisateur qui nous fait confiance. À noter : **l'URL des sorties
est déjà surchargeable dans les réglages** de la version actuelle, sans aucune
vérification d'intégrité.

Retenu : **signature Ed25519**, clé publique intégrée à l'application, vérification
avant tout import, refus silencieux et conservation de l'ancien catalogue en cas
d'échec. Même traitement rétroactif pour `releases.json`.

**Débat sur la fraîcheur.** SOURCE veut des mises à jour hebdomadaires. FILET rappelle
que chaque publication de bundle est une release de données à valider. Compromis :
**mensuel** pour le catalogue complet, **hebdomadaire** pour le petit fichier des
sorties, qui est un delta léger.

### Décisions
- **D-R4-1** — Bundle catalogue : **sources BnF uniquement**, Etalab 2.0, avec mention
  de source et date affichée dans l'écran À propos. *(→ [ADR-004](DECISIONS.md))*
- **D-R4-2** — Open Library : usage à l'exécution seulement, opt-in, jamais dans un
  artefact distribué.
- **D-R4-3** — Signature Ed25519 obligatoire sur le bundle **et** sur `releases.json`.
  Bundle non signé ou signature invalide = rejet silencieux, ancien catalogue conservé. *(→ [ADR-007](DECISIONS.md))*
- **D-R4-4** — Cadence : catalogue mensuel, sorties hebdomadaires.
- **D-R4-5** — Le pipeline de construction du catalogue vit dans un dépôt séparé
  (`bdteque-catalog`), avec ses propres tests et sa propre CI. Le dépôt applicatif ne
  contient que le consommateur.

---

## R5 — Design system & accessibilité
**2026-10-06 · 2 h · LISIBILITÉ, MAIN, BOUSSOLE, GARDE-FOU, moi**

### Objet
Restitution de l'audit d'accessibilité. Cadrer le design system.

### Restitution de l'audit (existant)

**Ce qui est déjà bon** — et il faut le dire, c'est au-dessus de la moyenne du marché :
police Atkinson Hyperlegible embarquée, cibles tactiles à 56 dp appliquées, `fontScale`
système respecté, descriptions de contenu présentes, réduction des animations gérée,
langage sans jargon tenu jusque dans le code.

**Les six anomalies relevées**

| # | Anomalie | Sévérité |
|---|---|---|
| A-01 | Contrastes non revérifiés depuis l'ajout du thème sombre : les jetons ont été dupliqués, jamais mesurés en mode nuit | Bloquante |
| A-02 | L'étagère est un défilement horizontal sans équivalent d'accès séquentiel : en TalkBack, une série de 36 tomes est un tunnel | Bloquante |
| A-03 | Aucune vue testée au-delà de `fontScale 1.5` ; à 2,0 le verdict et les formulaires n'ont jamais été observés | Majeure |
| A-04 | Les états de la tranche (possédé/manquant/lu/prêté) sont **uniquement** codés par la couleur et la forme, sans texte alternatif d'état | Majeure |
| A-05 | Aucun ordre de focus déclaré ; l'ordre par défaut sur le verdict place l'action principale en dernier | Modérée |
| A-06 | Les messages d'erreur ne sont pas annoncés (`liveRegion` absent) | Modérée |

### Débat

**L'étagère contre TalkBack.** L'étagère horizontale est la signature du produit ;
c'est aussi le pire schéma d'interaction possible pour un lecteur d'écran. LISIBILITÉ
refuse les deux fausses solutions : supprimer l'étagère (on tue l'âme du produit) et
poser une rustine (on ment sur la conformité).

Solution retenue, qui a mis tout le monde d'accord : **une vue en liste équivalente,
accessible à tous, pas seulement à TalkBack**. Un tomes-en-liste avec numéro, titre et
état en toutes lettres. Basculement persistant. Et l'observation de BOUSSOLE qui a
emporté la décision : *« les personnes du panel qui ne voient pas bien ne veulent pas
un mode handicap, elles veulent le mode qui marche pour elles. »*

**L'European Accessibility Act.** GARDE-FOU rappelle que le texte s'applique depuis
juin 2025 aux services numériques grand public dans l'UE. Une application personnelle
n'entre pas dans son champ ; une application publiée sur un store, potentiellement.
Conséquence pratique : le veto de LISIBILITÉ cesse d'être une politique interne pour
devenir une exigence de conformité — et donc non arbitrable en fin de sprint.

### Décisions
- **D-R5-1** — Les six anomalies sont traitées **avant** la publication publique, A-01
  et A-02 dès le sprint 3.
- **D-R5-2** — Vue liste équivalente à l'étagère, accessible à tous, préférence
  persistante. *(→ [ADR-009](DECISIONS.md))*
- **D-R5-3** — Cible officielle : **WCAG 2.2 AA + EN 301 549**, vérifiée automatiquement
  en CI et manuellement à chaque revue de sprint.
- **D-R5-4** — Design system documenté dans `docs/specs/DESIGN-SYSTEM.md`, source
  unique des jetons ; toute couleur non issue d'un jeton est refusée en revue.
- **D-R5-5** — Tests obligatoires à `fontScale` 1,0 / 1,5 / **2,0** sur les 8 écrans
  principaux, capturés en CI.

---

## R6 — Qualité, livraison & incidents
**2026-10-09 · 2 h · FILET, CHARPENTE, MAIN, moi**

### Objet
Construire la chaîne du commit au téléphone.

### Débat

**Comment savoir qu'on a planté, sans télémétrie ?**
Le sujet le plus épineux du cadrage. Sans Crashlytics, un crash chez un utilisateur est
invisible. FILET propose un compromis « anonymisé ». Refusé : un SDK de crash embarque
un identifiant d'installation et un rappel réseau automatique — c'est de la collecte,
quel que soit le nom qu'on lui donne.

Retenu : **journal d'incident local**. L'application capture l'exception dans un fichier
local, sans identifiant, sans envoi. Au redémarrage suivant : « L'application s'est
arrêtée la dernière fois. Voulez-vous envoyer le rapport ? » → partage manuel, contenu
visible avant envoi. Taux de retour faible et assumé : nous préférons dix rapports
consentis à dix mille rapports subis. Le panel compense (BOUSSOLE remonte les incidents
sous 48 h).

**Émulateur ou appareil réel ?** CHARPENTE veut une ferme d'appareils. Coût
disproportionné pour l'équipe. Compromis : émulateurs en CI pour la régression,
**deux appareils physiques de référence** — Pixel 6a (cible haute) et Galaxy A14
(cible basse, 4 Go de RAM, écran 720p) — pour la démonstration de fin de sprint et le
banc de performance. Aucune démonstration sur émulateur n'est recevable en revue.

**Les portes de qualité.** Débat vif sur leur caractère bloquant. Position tenue :
une porte non bloquante n'est pas une porte, c'est un avis. Cinq portes bloquent la
publication, listées ci-dessous. FILET obtient en contrepartie un droit de veto
explicite, et la possibilité de les court-circuiter par un ADR nominatif signé — la
trace publique étant le vrai garde-fou.

### Décisions
- **D-R6-1** — Cinq portes de qualité bloquantes : tests JVM verts · tests instrumentés
  des 5 parcours critiques verts · contrôles d'accessibilité sans anomalie bloquante ·
  budgets de performance tenus · test de migration de base vert. *(→ [ADR-005](DECISIONS.md))*
- **D-R6-2** — Rapport d'incident **local et manuel**. Aucun SDK de collecte. *(→ [ADR-010](DECISIONS.md))*
- **D-R6-3** — Deux appareils physiques de référence : Pixel 6a, Galaxy A14.
- **D-R6-4** — Publication : AAB signé, `versionCode` dérivé de la CI, pistes
  interne → fermée → ouverte. F-Droid en parallèle dès la publication publique.
- **D-R6-5** — Toute release s'accompagne d'un export de sauvegarde vérifié par
  restauration sur installation neuve. Rituel non négociable.

---

## R7 — Arbitrage de la feuille de route
**2026-10-13 · 3 h · tous**

### Objet
Passer de 14 épopées candidates à 8 financées. Affecter 110 sprints-personne.

### Débat

**La séance a duré une heure de plus que prévu**, sur un seul point : la R&D
« photographie d'étagère ».

ŒIL l'estime à 8 personne-sprints pour un résultat incertain (précision attendue
60–75 % sur des dos de livres en conditions d'éclairage domestique). BOUSSOLE la
défend : c'est le seul élément du plan qui produit un effet de démonstration immédiat,
et l'amorçage est vital (constat R2-5). CHARPENTE s'y oppose frontalement : *« huit
sprints de R&D quand on n'a pas encore un seul test instrumenté, c'est de la
gourmandise. »*

Arbitrage rendu, et il tranche dans le sens de la discipline :

> La R&D est **financée mais séquencée en dernier** (H3), **plafonnée à 6 personne-sprints**,
> avec un critère go/no-go chiffré évalué au sprint 14 : ≥ 60 % de séries correctement
> identifiées au premier choix sur un corpus de 30 photos d'étagères réelles. En dessous,
> arrêt immédiat, capacité rebasculée sur E5. Aucune prolongation, aucun « on y est
> presque ».

Motif assumé en séance : *on ne finance pas un rêve avant d'avoir livré une promesse.*
L'effet de démonstration recherché par la direction sera produit par le catalogue
(scanner un album inconnu en mode avion et obtenir son titre est déjà spectaculaire),
pas par une reconnaissance d'image à moitié fiable.

**Deuxième arbitrage.** Le widget d'écran d'accueil, jugé « gadget » par trois personnes,
est maintenu — il économise deux taps sur le geste central du produit, en librairie,
sur un téléphone tenu d'une main. C'est P1, pas de la décoration.

**Ce qui a été coupé** : synchronisation multi-appareils par cloud (viole P3),
recommandations (viole P3), estimation de la valeur d'une collection (données
commerciales non libres, contredit ADR-004), mode multi-utilisateur, notation et avis
(hors sujet), thème personnalisable (coût d'accessibilité disproportionné).

### Décisions
- **D-R7-1** — 8 épopées financées : E1 → E8. Priorisation RICE dans [ROADMAP.md](ROADMAP.md).
- **D-R7-2** — R&D photo d'étagère : plafond 6 personne-sprints, go/no-go sprint 14.
- **D-R7-3** — 6 fonctionnalités écartées explicitement, avec motif, pour qu'on cesse
  d'y revenir.
- **D-R7-4** — Réserve de capacité : **15 %** non affectés, pour les retours du panel.
  Une feuille de route pleine à 100 % est une feuille de route fausse.

---

## R8 — Revue de spécifications & gel
**2026-10-15 · 3 h · tous**

### Objet
Passage en revue des 8 spécifications. Critère : « prêt à développer ».

### Contrôle appliqué à chaque épopée

Chaque spécification doit satisfaire les 8 critères de [specs/README.md](specs/README.md).
Trois épopées ont été renvoyées en correction et corrigées en séance :

- **E1** — le comportement en cas de bundle **partiellement** téléchargé n'était pas
  spécifié (coupure réseau à 80 %). Ajouté : téléchargement dans un fichier temporaire,
  bascule atomique, reprise impossible = redémarrage propre. *(même exigence que
  l'écriture atomique déjà appliquée aux couvertures et aux sauvegardes dans le code
  existant — on aligne, on n'invente pas.)*
- **E2** — le seuil de confiance de l'OCR n'était pas chiffré. Ajouté : proposition
  affichée seulement au-delà de 0,80, jamais de sélection automatique, l'utilisateur
  confirme toujours.
- **E5** — le format de partage de liste laissait la porte ouverte à un identifiant.
  Corrigé : format inerte, aucun identifiant, aucune URL de retour.

### Décision finale

- **D-R8-1** — Les 8 spécifications passent en **prêt à développer**. Gel au 2026-10-15.
- **D-R8-2** — Toute modification ultérieure d'une spécification gelée passe par une
  entrée dans [DECISIONS.md](DECISIONS.md) et une revue de 30 minutes. Pas de
  modification par message.
- **D-R8-3** — Le sprint 1 démarre le **2026-10-20**. Contenu figé : D1 (sauvegardes),
  D3 (schémas et migrations), source `androidTest`, moissonnage BnF pilote.

### Mot de clôture porté au compte rendu

> Ce projet a été écrit par une personne pour son père. Le budget ne change pas ce
> qu'il est ; il change combien de pères peuvent l'utiliser. La première fonctionnalité
> que nous livrerons est la réparation d'une sauvegarde qui ne protège rien — parce que
> c'est ce qu'on doit à l'utilisateur qui l'a déjà installée.
</content>

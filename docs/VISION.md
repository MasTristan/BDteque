# VISION — BDteque

> Document de reprise de projet. Rédigé par le nouveau responsable produit & technique.
> Statut : **validé en comité du 2026-08-17**. Horizon : 12 mois (T4 2026 → T3 2027).
> Documents liés : [TEAM.md](TEAM.md) · [COMITES.md](COMITES.md) · [DECISIONS.md](DECISIONS.md) · [ROADMAP.md](ROADMAP.md) · [specs/](specs/)

---

## 0. Ce que j'ai trouvé en arrivant

Je ne reprends pas une friche. Je reprends un objet **fini, cohérent et rare**, construit
autour d'une intention forte : une application Android offerte par un fils à son père,
lecteur âgé et non technicien, pour gérer sa collection de bandes dessinées.

### L'inventaire factuel

| | |
|---|---|
| Code applicatif | **8 095 lignes Kotlin** (`app/src/main`), module unique |
| Tests | **1 092 lignes**, 13 classes de tests JVM sur le domaine pur |
| Écrans | 12 (onboarding, accueil, scanner, inventaire, verdict, séries, étagère, fiche album, fiche série, à paraître, liste d'achats, réglages) |
| Base | Room v2 (`series`, `albums`, `isbn_lookup_cache`), 1 migration écrite |
| Réseau | 3 sources : BnF SRU/UNIMARC, Open Library, `releases.json` distant |
| CI | GitHub Actions — tests JVM + APK debug, à chaque push et PR |
| Distribution | **aucune**. Artefact CI uniquement. `versionCode = 1`. |
| Utilisateurs | **1** |

### Les forces (à ne pas casser)

1. **Une intention produit lisible en trois secondes.** « En librairie : est-ce que je
   l'ai déjà ? » Tout le reste en découle. Peu de produits ont une boucle aussi nette.
2. **Une architecture disciplinée.** La séparation *Moitié A (collection locale)* /
   *Moitié B (sorties distantes)* est tenue partout, sans exception, y compris dans la
   liste d'achats qui les croise en mémoire sans jamais les mélanger en base.
3. **Une culture de la dégradation silencieuse.** Hors-ligne, disque plein, ISBN
   inconnu, JSON dupliqué, permission refusée : chaque cas est traité, commenté, et
   ne casse jamais l'usage principal. C'est de l'ingénierie de qualité rare.
4. **L'accessibilité comme fondation, pas comme rattrapage.** Atkinson Hyperlegible,
   cibles 56 dp, `fontScale` respecté, langage sans jargon (« code-barres », jamais
   « EAN »).
5. **Une esthétique.** Papier chaud, encre, tranches de livres, tampon encreur.
   Le produit a une âme, ce qui ne s'achète pas avec du budget.

### Les dettes (à solder avant d'accélérer)

| # | Dette | Gravité | Traitée dans |
|---|---|---|---|
| D1 | **Les sauvegardes automatiques vivent dans `filesDir`** : désinstaller l'app détruit les 7 sauvegardes. Le filet de sécurité ne protège pas du cas le plus courant. | 🔴 Critique | [E4](specs/E4-coffre.md) |
| D2 | **Aucun test instrumenté.** Le dossier `androidTest` n'existe pas alors que les dépendances sont déclarées. Aucun parcours n'est vérifié de bout en bout. | 🔴 Critique | [E6](specs/E6-qualite.md) |
| D3 | **Les schémas Room ne sont pas versionnés** (`schemaDirectory` configuré, `app/schemas/` absent du dépôt). Aucun test de migration possible : une migration ratée = collection perdue. | 🔴 Critique | [E6](specs/E6-qualite.md) |
| D4 | **Chaque scan fuite un ISBN** vers la BnF et Open Library. Sans compte, mais l'adresse IP + le flux des requêtes révèle ce que la personne achète et quand. Contradiction avec la promesse « aucune collecte ». | 🟠 Majeure | [E1](specs/E1-catalogue.md) |
| D5 | **Les trous ne sont connus qu'en deçà du dernier tome possédé.** `GapDetector` ne peut rien inventer : si on possède T1–T13 d'une série qui en compte 22, l'app affiche « collection complète ». C'est faux, et c'est précisément la question posée en librairie. | 🟠 Majeure | [E1](specs/E1-catalogue.md) |
| D6 | **La donnée des sorties est maintenue à la main par une seule personne** (`releases.json`). `releases.json` est aujourd'hui **vide**. La fonctionnalité « À paraître » existe mais n'a rien à afficher. | 🟠 Majeure | [E1](specs/E1-catalogue.md) |
| D7 | **Aucune chaîne de release** : pas de signature, pas de `minify`, pas d'AAB, pas de compte store. Le produit ne peut pas atteindre un deuxième utilisateur. | 🟠 Majeure | [E8](specs/E8-distribution.md) |
| D8 | Pas de `lint` ni d'analyse statique en CI ; `versionCode` figé à 1 ; pas de budget de taille d'APK. | 🟡 Modérée | [E6](specs/E6-qualite.md) |
| D9 | Le nom **« Bédéthèque »** affiché dans le wordmark est aussi la marque du principal acteur du secteur (bedetheque.com / BDGest). Risque à lever avant toute publication. | 🟡 Modérée | [ADR-011](DECISIONS.md) |

---

## 1. La thèse

> **Le marché de la BD a des bases de données. Il n'a pas d'objet.**

Les applications existantes (Bubble, BDGest/Bédéthèque, Sanctuary) sont des **catalogues
avec une interface** : compte obligatoire, densité d'information maximale, dépendance
totale au réseau, monétisation par abonnement ou publicité, ergonomie pensée pour un
collectionneur expert de 35 ans qui saisit ses albums le soir.

BDteque prend le problème par l'autre bout : **une étagère, avec des trous.** L'objet
avant la donnée. Le geste avant le formulaire. Trois secondes en librairie, sans réseau,
sans compte, sans se souvenir d'un mot de passe.

Ce positionnement n'est pas un compromis « petit produit ». C'est le seul segment que
les acteurs installés **ne peuvent pas** attaquer sans se renier : leur modèle
économique repose sur le compte et la donnée d'usage. Le nôtre repose sur leur absence.

### La formulation en une phrase

> **BDteque est l'application de collection qu'on peut offrir.**
> Elle marche le premier jour, sans compte. Elle marche dans dix ans, sans serveur.
> Elle marche pour quelqu'un qui n'a jamais installé une application.

---

## 2. Positionnement

|  | Bubble / BDGest | Tableur / carnet | **BDteque** |
|---|---|---|---|
| Compte obligatoire | Oui | Non | **Non** |
| Fonctionne hors-ligne | Partiellement | Oui | **Totalement** |
| Réponse « je l'ai déjà ? » | 10–20 s, avec réseau | ~1 min | **< 3 s, sans réseau** |
| Utilisable à 75 ans | Difficilement | Oui | **Conçu pour** |
| Collecte de données | Oui | Non | **Aucune** |
| Complétude du catalogue | Excellente | Nulle | *Faiblesse actuelle → cible : bonne* |
| Modèle | Abonnement / pub | — | **Sans monétisation de l'usager** |

**Notre seule faiblesse structurelle face à eux est la complétude du catalogue.**
C'est exactement là que va la moitié du budget (voir §4, pari n°1).

---

## 3. Les cinq piliers (invariants de conception)

Toute décision produit se juge contre ces cinq lignes. Une fonctionnalité qui en viole
une est refusée, quel que soit son intérêt commercial.

**P1 — Le verdict en trois secondes.**
La boucle « scanner → savoir » est le produit. Tout ce qui la ralentit d'un tap ou d'une
seconde est un régression majeure, pas un détail. Budget non négociable : **p95 < 1,5 s
en mode avion**, du déclenchement du scan à l'affichage du verdict.

**P2 — Hors-ligne d'abord, absolument.**
Le réseau est un bonus qui améliore des données, jamais une condition d'usage. Toute
fonctionnalité réseau doit avoir un comportement défini, silencieux et non bloquant en
mode avion. Corollaire nouveau : **les données descendent, elles ne remontent jamais.**

**P3 — Aucun compte, aucune collecte, jamais.**
Pas d'authentification, pas de télémétrie, pas de SDK analytique, pas de publicité.
Nous mesurons par la recherche utilisateur, pas par la surveillance (voir §6).

**P4 — L'accessibilité est une fonctionnalité, pas une conformité.**
Cible : **WCAG 2.2 AA + EN 301 549**, vérifiée en CI. Taille de police système jusqu'à
2,0×. TalkBack complet. Cibles ≥ 56 dp. Langage sans jargon. Notre utilisateur type a
70 ans et une presbytie — c'est notre avantage concurrentiel, pas notre contrainte.

**P5 — Durabilité à dix ans.**
Aucun service payant, aucune dépendance à une API propriétaire, aucun format fermé.
Si toute l'équipe disparaît demain, l'application installée continue de fonctionner
indéfiniment, et la collection reste exportable en JSON et en CSV lisibles.

---

## 4. Les trois paris

Le budget sert à financer trois paris, dans cet ordre. Le reste est de l'intendance.

### Pari n°1 — **Le Catalogue** *(cœur du budget, T4 2026)*

> Faire passer l'application de « ce que tu lui as dit » à « ce qui existe ».

Aujourd'hui l'app ne connaît que ce que l'utilisateur a saisi. Elle ne peut donc pas
répondre correctement à la question qui la justifie : *qu'est-ce qu'il me manque ?*

Nous construisons un **référentiel BD franco-belge embarqué** : un fichier de données
versionné, signé et téléchargé en bloc, construit à partir du catalogue de la **BnF**
(dépôt légal, données sous Licence Ouverte Etalab 2.0 — usage libre avec attribution,
sans réciprocité). L'application interroge ce référentiel **localement**.

Ce que ça change, concrètement :

- Scanner un album jamais vu, en mode avion, donne son titre, sa série et son tome.
- « Il te manque les tomes 7, 12 et 19 » devient vrai, même au-delà du dernier tome
  possédé.
- « À paraître » s'alimente tout seul : plus de fichier maintenu à la main (D6).
- **Plus aucun ISBN ne quitte le téléphone** : on télécharge le catalogue entier, pas
  des réponses à des questions. La fuite de vie privée D4 disparaît par construction.

C'est le pari structurant : il transforme une faiblesse en argument.
Spécification : [E1 — Le Catalogue](specs/E1-catalogue.md).

### Pari n°2 — **Le Scan qui ne renonce jamais** *(T1 2027)*

Un tiers des albums d'une collection ancienne **n'a pas de code-barres lisible** :
éditions d'avant 1990, sur-couvertures plastifiées, dos abîmés. Aujourd'hui, l'app
s'arrête là et renvoie vers un formulaire — l'échec exact que le produit prétend éviter.

Chaîne de résolution complète : collection locale → catalogue local → cache ISBN →
réseau (opt-in) → **reconnaissance de texte sur la couverture** (ML Kit, embarqué) →
saisie assistée. À chaque échec, une marche de repli, jamais un cul-de-sac.

Spécification : [E2 — Le Verdict](specs/E2-scan.md).

### Pari n°3 — **De zéro à deux mille albums en une soirée** *(T1–T2 2027)*

Le produit ne survit pas au premier lancement d'un nouvel utilisateur : saisir 600
albums à la main, personne ne le fait. Import des exports Bubble/BDGest/CSV, ajout
d'une série entière en un geste depuis le catalogue, et — en R&D encadrée — la
**photographie d'une étagère** qui reconnaît les tranches.

Spécification : [E3 — L'Amorçage](specs/E3-amorcage.md).

---

## 5. Ce qui ne change pas

Décisions héritées, réexaminées le 2026-08-17, **confirmées** :

- Kotlin, Compose, Material 3, Room, DataStore, WorkManager, CameraX, ML Kit.
- Module unique côté application (une exception : extraction du domaine pur, [ADR-002](DECISIONS.md)).
- Injection manuelle, pas de Hilt.
- Thèmes clair et sombre à jetons fixes, pas de couleurs dynamiques.
- Français comme unique langue d'interface en 2026 (internationalisation : voir §7).
- Aucun SDK Google hors ML Kit et CameraX. Pas de Firebase, pas de Crashlytics.

Une décision héritée est **assouplie** : le « pas de couvertures » de la spec v1 a déjà
été levé par l'équipe précédente (téléchargement opt-in, service local ensuite). Nous
confirmons cet assouplissement et l'encadrons ([ADR-006](DECISIONS.md)).

---

## 6. Comment on mesure sans surveiller

La direction demandera des chiffres. Nous n'en fabriquerons pas au prix du pilier P3.
Notre instrumentation est **humaine et volontaire** :

| Question | Instrument | Fréquence |
|---|---|---|
| Le verdict est-il assez rapide ? | Banc de mesure automatisé en CI, sur appareil réel (Pixel 6a + Galaxy A14, notre cible basse) | Chaque release |
| Le catalogue est-il assez complet ? | **Taux de résolution** mesuré sur un corpus de 500 EAN réels collectés en librairie partenaire | Mensuel |
| Les gens comprennent-ils ? | **Panel de 12 collectionneurs** (dont 5 de plus de 65 ans), tests d'usage filmés | Chaque fin de sprint pair |
| L'app est-elle stable ? | Rapports d'incident **envoyés manuellement** par l'utilisateur (bouton « Signaler un souci », partage d'un fichier texte local) + avis store | Continu |
| Qui l'utilise ? | Volume de téléchargements store, avis, retours du panel. **Rien d'autre.** | Mensuel |

**Ce que nous refusons de savoir** : combien d'albums possède un utilisateur donné,
quelles séries il suit, quand il ouvre l'app. Cette ignorance est le produit.

---

## 7. Horizon 12 mois

| Horizon | Période | Thème | Jalon public |
|---|---|---|---|
| **H1 — Solder & fonder** | T4 2026 (sprints 1–6) | Dettes critiques D1–D3, D7 · Catalogue v1 · chaîne de release | **v2.0 « Le Catalogue »** — piste interne Play, 12 testeurs |
| **H2 — Ouvrir** | T1 2027 (sprints 7–12) | Scan complet · Amorçage · Le Coffre | **v2.1** — publication publique Play + F-Droid |
| **H3 — Incarner** | T2–T3 2027 (sprints 13–24) | Prêts & famille · Étagère vivante · widget · R&D photo d'étagère | **v3.0** — arbitrage modèle économique |

Détail, priorisation RICE et affectation du budget : [ROADMAP.md](ROADMAP.md).

**Internationalisation** : hors périmètre 12 mois. Le catalogue BnF est franco-belge ;
étendre la langue avant le catalogue produirait une coquille vide. Réexamen au T3 2027.

---

## 8. Les cinq risques que je surveille personnellement

| Risque | Probabilité | Impact | Parade |
|---|---|---|---|
| **R1 — La licence des données du catalogue.** Une contamination ODbL (Open Library) rendrait notre référentiel dérivé partageable de force, ou nous mettrait en faute. | Moyenne | Élevé | Bundle **BnF/Etalab uniquement**. Open Library reste un appoint d'exécution, jamais une source du bundle. Revue juridique avant chaque build de catalogue ([ADR-004](DECISIONS.md)). |
| **R2 — Le poids du catalogue.** Un référentiel trop lourd casse l'installation sur les téléphones d'entrée de gamme de notre cible. | Élevée | Moyen | Budget dur : **< 15 Mo compressé, < 60 Mo installé**, vérifié en CI, sinon la release est bloquée. Sous-ensemble embarqué + reste téléchargé sur Wi-Fi. |
| **R3 — Trahir l'âme du produit sous pression de croissance.** « Et si on ajoutait un compte pour la synchro ? » | Moyenne | **Fatal** | Les cinq piliers §3 sont opposables en comité. Toute violation exige un ADR signé par le responsable produit **et** la direction. |
| **R4 — Dépendance à une personne** (le fichier des sorties, la connaissance du domaine). | Élevée aujourd'hui | Élevé | Le pari n°1 supprime le maintien manuel. Documentation d'exploitation obligatoire par binôme. |
| **R5 — Marque.** Le wordmark actuel emploie un terme adossé à la marque d'un concurrent direct (D9). | Faible | Élevé | Recherche d'antériorité INPI avant toute publication ; renommage prévu et budgété ([ADR-011](DECISIONS.md)). |

---

## 9. Ce à quoi ressemble la réussite, en août 2027

- Un utilisateur de 72 ans installe l'app, importe sa collection en 20 minutes, et
  répond en librairie sans jamais avoir créé de compte.
- Le scan d'un album quelconque de BD franco-belge aboutit **dans plus de 9 cas sur 10,
  en mode avion**.
- L'application est publiée sur Play et F-Droid, note ≥ 4,5, **zéro donnée collectée**
  déclarée au formulaire Data Safety — et c'est vrai.
- Le père pour qui elle a été écrite s'en sert toujours, et le code de la v1 qu'il
  utilise tourne encore à l'identique sous les nouvelles couches.

Ce dernier point n'est pas de la sentimentalité. C'est le test de non-régression le
plus exigeant que je connaisse : **un produit qui reste bon pour une personne quand il
devient bon pour cent mille.**
</content>

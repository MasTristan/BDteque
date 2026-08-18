# L'ÉQUIPE — 8 recrutements

> Composition arrêtée le 2026-08-17. Prise de poste échelonnée du 2026-09-01 au 2026-10-01.
> Les personnes sont désignées par leur **rôle** et un **indicatif** ; les prénoms sont
> des personas de travail servant à rendre les comptes rendus lisibles.

---

## Principe de composition

Huit postes, pas un de plus. Trois règles ont guidé le choix :

1. **Chaque recrutement lève un risque nommé de la vision**, pas une envie. Si je ne
   peux pas rattacher un poste à un risque R1–R5 ou à une dette D1–D9, je ne le crée pas.
2. **Deux personnes minimum sur tout sujet critique.** Le projet arrive avec une
   dépendance à une seule personne (risque R4) ; on ne la reproduit pas.
3. **Le produit a une âme et une contrainte d'accessibilité.** Ces deux sujets ont un
   titulaire dédié et un droit de veto, sinon ils sont les premiers sacrifiés sous
   pression de calendrier.

Répartition : **5 construction · 1 produit · 1 design · 1 conformité** (temps partiel).

---

## 1 — Architecte Android · « CHARPENTE »

**Persona** : Naïm · **Niveau** : Staff Engineer · **Temps** : plein · **Arrivée** : 2026-09-01

**Mandat.** Garantir que le socle tienne dix ans et cent fois la charge de données
actuelle. Propriétaire du modèle de données, des migrations Room, des budgets de
performance et de la cohérence architecturale.

**Ce qu'on attend dans les 30 premiers jours**
- `app/schemas/` versionné et migrations testées (dette D3) — **avant toute autre chose**.
- Extraction du domaine pur en module `:core:domain` sans dépendance Android ([ADR-002](DECISIONS.md)).
- Banc de performance automatisé : démarrage à froid, temps jusqu'au verdict, fluidité
  de l'étagère à 2 000 albums.

**Indicateurs** : 0 migration non testée · démarrage à froid < 1,2 s sur Galaxy A14 ·
p95 verdict < 1,5 s hors-ligne.

**Droit de veto** : sur toute modification de schéma de base sans migration ni test.

---

## 2 — Ingénieure Données & Catalogue · « SOURCE »

**Persona** : Fatou · **Niveau** : Senior Data Engineer · **Temps** : plein · **Arrivée** : 2026-09-01

**Mandat.** Construire et exploiter le référentiel BD — le pari n°1. Propriétaire du
pipeline de moissonnage BnF, de la normalisation séries/tomes, du format de bundle,
de sa signature et de sa publication.

C'est le poste le plus structurant du plan : il porte à lui seul les dettes D4, D5 et D6.

**Ce qu'on attend dans les 30 premiers jours**
- Moissonnage BnF SRU d'un périmètre pilote (200 séries), mesure du **taux de
  résolution réel** sur un corpus de 500 EAN.
- Décision documentée sur la normalisation série/tome (le point dur : « Thorgal »,
  « Thorgal — La Jeunesse », intégrales, rééditions, tirages de tête).
- Première version du format de bundle et de sa chaîne de signature.

**Indicateurs** : taux de résolution EAN ≥ 90 % sur corpus · bundle < 15 Mo compressé ·
build de catalogue reproductible et automatisé.

**Note** : l'application sait déjà lire l'UNIMARC de la BnF (`BnfUnimarcParser`,
151 lignes, testé). Le poste part d'un acquis, pas d'une page blanche.

---

## 3 — Designer Produit & Accessibilité · « LISIBILITÉ »

**Persona** : Camille · **Niveau** : Senior, spécialité accessibilité · **Temps** : plein · **Arrivée** : 2026-09-15

**Mandat.** Tenir l'âme visuelle (« papier & encre ») **et** la conformité
WCAG 2.2 AA / EN 301 549. Les deux dans la même main, volontairement : séparer le beau
du lisible produit toujours un compromis raté.

**Ce qu'on attend dans les 30 premiers jours**
- Audit d'accessibilité complet de l'existant, chiffré, avec plan de correction.
- Design system documenté (jetons, composants, états) — aujourd'hui le style vit dans
  le code sans référence externe.
- Protocole de test d'usage avec le panel de 12 personnes, dont 5 de plus de 65 ans.

**Indicateurs** : 0 anomalie bloquante d'accessibilité en CI · toutes les vues tenables
à `fontScale 2.0` · parcours principal réussi par 5 utilisateurs sur 5 sans aide.

**Droit de veto** : sur toute interface enfreignant les critères d'accessibilité, y
compris en fin de sprint. Ce veto n'est pas négociable au motif du calendrier.

---

## 4 — Ingénieur Vision embarquée · « ŒIL »

**Persona** : Youssef · **Niveau** : Senior, CV/ML sur appareil · **Temps** : plein · **Arrivée** : 2026-10-01

**Mandat.** Faire que le scan ne renonce jamais (pari n°2) et explorer la
reconnaissance d'étagère (pari n°3). Tout sur l'appareil, aucune image ne sort du
téléphone — sans quoi le pilier P3 tombe.

**Ce qu'on attend dans les 30 premiers jours**
- Corpus de test : 300 photos réelles (codes-barres abîmés, sous plastique, albums
  d'avant 1990 sans code-barres), constitué avec la librairie partenaire.
- Repli OCR couverture → titre → catalogue, mesuré, avec seuil de confiance explicite.
- **Étude de faisabilité** de la photo d'étagère avec critère go/no-go chiffré.

**Indicateurs** : taux de résolution global du scan ≥ 90 % · 0 faux positif (proposer
le mauvais album est pire que ne rien proposer) · aucune inférence hors appareil.

**Encadrement** : ce poste porte la seule vraie R&D du plan. Il est **arrêtable** :
si les critères go/no-go de la photo d'étagère ne sont pas atteints au sprint 14, le
sujet est abandonné sans discussion et la capacité rebasculée sur E5.

---

## 5 — Ingénieur Qualité & Release · « FILET »

**Persona** : Théo · **Niveau** : Senior QA / Release Engineer · **Temps** : plein · **Arrivée** : 2026-09-01

**Mandat.** Faire exister la chaîne qui va du commit au téléphone d'un inconnu
(dettes D2, D7, D8). Propriétaire de la CI, des tests instrumentés, de la signature,
des pistes de publication et du processus d'incident.

**Ce qu'on attend dans les 30 premiers jours**
- Source `androidTest` créée, 5 parcours critiques couverts en test instrumenté.
- CI complète : lint + analyse statique + tests JVM + tests instrumentés sur émulateur
  + budget de taille d'APK + contrôles automatisés d'accessibilité.
- Chaîne de release signée : AAB, `versionCode` dérivé de la CI, piste interne Play.

**Indicateurs** : couverture des 5 parcours critiques à 100 % · CI verte < 15 min ·
release publiable en une commande · 0 régression atteignant un utilisateur.

**Droit de veto** : sur toute publication ne passant pas les portes de qualité définies
en [E6](specs/E6-qualite.md).

---

## 6 — Ingénieure Android Produit · « MAIN »

**Persona** : Léna · **Niveau** : Senior Android · **Temps** : plein · **Arrivée** : 2026-09-15

**Mandat.** La vélocité sur les fonctionnalités visibles. C'est le poste qui livre
l'essentiel des écrans : Coffre, prêts, partage, étagère vivante, widget.

**Ce qu'on attend dans les 30 premiers jours**
- Correction de la dette D1 (sauvegardes détruites à la désinstallation) — **premier
  ticket du projet, sprint 1**, parce qu'elle peut détruire la collection d'un
  utilisateur réel dès aujourd'hui.
- Assistant de restauration avec prévisualisation.
- Reprise du parcours d'ajout manuel, mesuré au chronomètre avec le panel.

**Indicateurs** : ajout d'un album connu en ≤ 3 taps · aucune tâche livrée sans test ·
aucune chaîne de caractères codée en dur dans le Compose.

---

## 7 — Responsable Produit & Recherche · « BOUSSOLE »

**Persona** : Inès · **Niveau** : Senior PM, profil recherche utilisateur · **Temps** : plein · **Arrivée** : 2026-09-01

**Mandat.** Remplacer la télémétrie que nous refusons par de la connaissance réelle.
Propriétaire du panel, des tests d'usage, du backlog priorisé, de la relation avec les
libraires partenaires, et de la fiche store.

C'est le contrepoids indispensable au pilier P3 : sans instrumentation, une équipe
navigue à l'intuition. Ce poste est notre instrumentation.

**Ce qu'on attend dans les 30 premiers jours**
- Panel de 12 personnes recruté et sous accord, dont 5 de plus de 65 ans et 2 libraires.
- 15 entretiens réalisés, personas et parcours d'usage documentés.
- Corpus de 500 EAN réels collectés en librairie, qui servira de banc de mesure à E1 et E2.

**Indicateurs** : ≥ 2 tests d'usage par mois · chaque épopée validée par au moins
5 utilisateurs avant livraison · délai retour terrain → backlog < 5 jours.

---

## 8 — Conseil Conformité, Données & Marque · « GARDE-FOU »

**Persona** : Maître Ravel · **Niveau** : Conseil externe · **Temps** : **0,2 ETP** (1 jour/semaine) · **Arrivée** : 2026-09-01

**Mandat.** Le seul poste non technique, et le moins discutable du plan : le pari n°1
repose entièrement sur le droit d'exploiter des données publiques, et le produit est
sur le point de prendre un nom déjà occupé.

**Périmètre**
- **Licences de données** : Licence Ouverte Etalab 2.0 (BnF) vs ODbL (Open Library) —
  éviter toute contamination du référentiel dérivé (risque R1).
- **Marque** : recherche d'antériorité INPI sur le nom et le wordmark (risque R5, D9).
- **Conformité européenne** : *European Accessibility Act*, applicable depuis
  juin 2025 aux services numériques grand public — l'accessibilité passe du statut de
  qualité à celui d'obligation, ce qui change la nature du veto de LISIBILITÉ.
- **RGPD** : dossier trivial (aucune donnée collectée), mais il faut le **prouver** et
  le déclarer correctement au formulaire Data Safety de Play, sous peine de retrait.

**Indicateurs** : avis écrit sur la licence du bundle avant le premier build public ·
recherche d'antériorité rendue avant le sprint 6 · dossier Data Safety validé du
premier coup.

---

## Vue d'ensemble

| # | Indicatif | Rôle | ETP | Risques / dettes couverts |
|---|---|---|---|---|
| 1 | CHARPENTE | Architecte Android | 1,0 | D3, D8, R2 |
| 2 | SOURCE | Données & Catalogue | 1,0 | D4, D5, D6, R1, R4 |
| 3 | LISIBILITÉ | Design & Accessibilité | 1,0 | P4, R3 |
| 4 | ŒIL | Vision embarquée | 1,0 | Pari n°2, pari n°3 |
| 5 | FILET | Qualité & Release | 1,0 | D2, D7, D8 |
| 6 | MAIN | Android produit | 1,0 | D1, vélocité |
| 7 | BOUSSOLE | Produit & Recherche | 1,0 | P3 (mesure sans surveillance), R4 |
| 8 | GARDE-FOU | Conformité & Marque | 0,2 | R1, R5, EAA |
| — | *(moi)* | Responsable produit & technique | 1,0 | Arbitrage, P1–P5 |

**Capacité totale : 8,2 ETP**, dont **5,5 ETP de construction** (CHARPENTE, SOURCE, ŒIL,
FILET, MAIN, plus la moitié de LISIBILITÉ qui produit aussi des composants).
Sur 24 sprints de 2 semaines, avec un abattement de 15 % pour congés et imprévus :
**≈ 110 sprints-personne de capacité de construction**. Affectation détaillée dans
[ROADMAP.md](ROADMAP.md).

---

## Ce que je n'ai délibérément pas recruté

Ces absences sont des décisions, pas des oublis. On me les reprochera ; voici la réponse.

| Poste écarté | Pourquoi |
|---|---|
| **Développeur backend / DevOps serveur** | Il n'y a pas de serveur, et il ne doit pas y en avoir. Le catalogue est un **fichier statique signé**, publié sur un stockage objet. Recruter un backend créerait la tentation d'un service — et le compte utilisateur suivrait dans les six mois (risque R3). |
| **Développeur iOS** | Le catalogue franco-belge et l'accessibilité valent plus qu'une deuxième plateforme. L'extraction du domaine en Kotlin pur ([ADR-002](DECISIONS.md)) garde la porte ouverte sans en payer le prix maintenant. |
| **Growth / marketing** | Sans télémétrie, la croissance passe par le produit, le bouche-à-oreille et les libraires. BOUSSOLE porte la fiche store ; on réévaluera au moment de la publication publique. |
| **Data scientist / recommandation** | « Vous aimerez aussi… » exige de savoir ce que les gens lisent. Contradiction frontale avec P3. Poste structurellement impossible ici. |
| **Second designer** | Un seul design system, une seule main. Le risque est la surcharge, pas la divergence : arbitré par le veto et le périmètre. |

---

## Rituels

| Rituel | Fréquence | Durée | Qui | Objet |
|---|---|---|---|---|
| Point de flux | Quotidien | 10 min | Construction | Blocages uniquement, pas de rapport d'activité |
| Revue de sprint | Fin de sprint | 60 min | Tous | Démonstration **sur appareil réel**, jamais sur émulateur |
| Terrain | Sprints pairs | 90 min | BOUSSOLE + 2 tournants | Test d'usage filmé avec le panel |
| Comité d'architecture | Mensuel | 90 min | CHARPENTE, SOURCE, FILET, moi | ADR, dette, budgets de performance |
| Revue de spec | Avant chaque épopée | 60 min | Tous | Passage en « prêt à développer » (voir [specs/README.md](specs/README.md)) |
| Comité direction | Mensuel | 45 min | Moi + direction | Jalons, budget, risques R1–R5 |

**Règle de réunion** : toute décision produit une entrée dans [DECISIONS.md](DECISIONS.md)
ou n'a pas eu lieu. Une réunion sans décision écrite est annulée pour la fois suivante.
</content>

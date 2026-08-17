# FEUILLE DE ROUTE — T4 2026 → T3 2027

> Arbitrée en [comité R7](COMITES.md#r7--arbitrage-de-la-feuille-de-route) le 2026-10-13.
> Capacité : **≈ 110 sprints-personne** de construction (5,5 ETP × 24 sprints × 0,85).
> Sprints de 2 semaines. Sprint 1 : **2026-10-20**.

---

## 1. Les 8 épopées financées

| Réf | Épopée | Spécification | Coût | Horizon |
|---|---|---|---|---|
| **E1** | Le Catalogue — référentiel BD embarqué | [E1-catalogue.md](specs/E1-catalogue.md) | 24 sp | H1 |
| **E2** | Le Verdict — un scan qui ne renonce jamais | [E2-scan.md](specs/E2-scan.md) | 16 sp | H2 |
| **E3** | L'Amorçage — de 0 à 2 000 albums | [E3-amorcage.md](specs/E3-amorcage.md) | 14 sp | H2 |
| **E4** | Le Coffre — durabilité de la collection | [E4-coffre.md](specs/E4-coffre.md) | 10 sp | H1 |
| **E5** | Prêts, souhaits & partage | [E5-partage.md](specs/E5-partage.md) | 10 sp | H3 |
| **E6** | Qualité, accessibilité & exploitation | [E6-qualite.md](specs/E6-qualite.md) | 16 sp | H1 → continu |
| **E7** | L'Étagère vivante | [E7-etagere.md](specs/E7-etagere.md) | 12 sp | H3 |
| **E8** | Distribution & marque | [E8-distribution.md](specs/E8-distribution.md) | 6 sp | H1 → H2 |
| | **Total affecté** | | **108 sp** | |
| | *Réserve non affectée (retours du panel)* | | *15 %* | |

`sp` = sprint-personne.

---

## 2. Priorisation RICE

Score = (Portée × Impact × Confiance) / Effort. Portée : part des utilisateurs touchés.
Impact : 0,25 (marginal) à 3 (transformateur). Confiance : 0,5 à 1,0.

| Rang | Épopée | Portée | Impact | Conf. | Effort | **RICE** | Commentaire d'arbitrage |
|---|---|---|---|---|---|---|---|
| 1 | **E6** Qualité & a11y | 100 % | 2,0 | 1,0 | 16 | **12,5** | Ne se voit pas, conditionne tout le reste. Non négociable. |
| 2 | **E1** Catalogue | 100 % | 3,0 | 0,8 | 24 | **10,0** | Le pari. Confiance 0,8 : dépend du taux de couverture BnF, mesuré avant engagement. |
| 3 | **E4** Coffre | 100 % | 1,5 | 1,0 | 10 | **15,0** | Score le plus élevé, effort le plus faible : contient la correction de la dette D1. |
| 4 | **E2** Scan complet | 80 % | 2,5 | 0,7 | 16 | **8,8** | Confiance 0,7 : la performance de l'OCR sur des couvertures anciennes reste à prouver. |
| 5 | **E3** Amorçage | 90 % | 2,5 | 0,9 | 14 | **14,5** | Condition d'acquisition. Fort score, mais dépend de E1 : ne peut pas passer avant. |
| 6 | **E8** Distribution | 100 % | 1,0 | 1,0 | 6 | **16,7** | Score maximal, mais sans objet tant que E6 n'a pas livré ses portes de qualité. |
| 7 | **E5** Prêts & partage | 60 % | 1,5 | 0,9 | 10 | **8,1** | Le besoin relationnel révélé en R2. Différé faute de dépendances prêtes. |
| 8 | **E7** Étagère vivante | 100 % | 1,0 | 0,8 | 12 | **6,7** | Le plaisir. Volontairement en dernier : on gagne le droit de faire du beau. |

**Lecture honnête du tableau.** Le RICE brut place E8 et E4 en tête et E1 en cinquième
position. **Nous ne suivons pas ce classement**, et c'est délibéré : le RICE mesure le
rendement d'un incrément, pas les dépendances ni la valeur d'option. E1 débloque E2, E3
et E7 ; le retarder décale tout le reste d'un trimestre. E8 sans E6 publierait un
produit non testé à des inconnus.

Ce que le RICE **a** changé : E4 est remonté de H2 à H1 (score 15, effort 10, et il
contient une correction de perte de données), et E7 est descendu en dernier sans débat.

---

## 3. Séquencement

### H1 — Solder & fonder · sprints 1–6 · 2026-10-20 → 2027-01-12

**Objectif de sortie : v2.0 « Le Catalogue », piste interne Play, 12 testeurs.**

| Sprint | Contenu | Qui |
|---|---|---|
| **1** | D1 sauvegardes SAF · `app/schemas/` + tests de migration 1→2 · création de `androidTest` · moissonnage BnF pilote (200 séries) | MAIN, CHARPENTE, FILET, SOURCE |
| **2** | Extraction `:core:domain` · 5 parcours critiques instrumentés · format de bundle v1 + signature Ed25519 · audit a11y | CHARPENTE, FILET, SOURCE, LISIBILITÉ |
| **3** | `catalog.db` + import de bundle · anomalies a11y A-01 et A-02 · pipeline de release signée | CHARPENTE, SOURCE, LISIBILITÉ, FILET |
| **4** | Moteur de correspondance catalogue ↔ collection · vue liste équivalente (ADR-009) · assistant de restauration | SOURCE, LISIBILITÉ, MAIN |
| **5** | Trous réels sur toute la série · « À paraître » alimenté par le catalogue · portes de qualité G1–G5 en CI | SOURCE, MAIN, FILET |
| **6** | Verdict enrichi hors-ligne · budgets de performance · piste interne Play · **décision ADR-011 (nom)** | Tous |

**Critères de sortie de H1** (tous requis, sinon H2 est décalé)
- Scanner un album absent de la collection, **en mode avion**, affiche sa série et son tome.
- Taux de résolution EAN sur le corpus de 500 ≥ **90 %**.
- Les 5 portes de qualité sont vertes et bloquantes.
- Une sauvegarde survit à une désinstallation-réinstallation.
- 12 testeurs ont l'application sur leur téléphone.

### H2 — Ouvrir · sprints 7–12 · 2027-01-12 → 2027-04-06

**Objectif de sortie : v2.1, publication publique Play + F-Droid.**

| Sprint | Contenu |
|---|---|
| 7–8 | E2 : chaîne de résolution complète, repli OCR couverture, durcissement du mode inventaire |
| 9–10 | E3 : imports Bubble/BDGest/CSV, ajout d'une série entière depuis le catalogue |
| 11 | E4 : transfert d'appareil par appairage local, export chiffré |
| 12 | E8 : fiche store, Data Safety, F-Droid, **publication publique** |

**Critères de sortie de H2**
- Taux de résolution global du scan ≥ **90 %**, y compris albums sans code-barres.
- Import d'une collection de 600 albums en moins de 5 minutes, sans perte.
- Application publiée, note ≥ 4,0, **zéro donnée collectée** déclarée.

### H3 — Incarner · sprints 13–24 · 2027-04-06 → 2027-09-28

**Objectif de sortie : v3.0 et arbitrage du modèle économique (ADR-012).**

| Sprint | Contenu |
|---|---|
| 13–15 | E5 : prêts avec emprunteur et date, liste de souhaits, partage inerte |
| **14** | **Go/no-go R&D photo d'étagère** — seuil : ≥ 60 % de séries correctes au premier choix sur 30 photos réelles |
| 16–19 | E7 : mur d'étagères, statistiques de collection, widget d'écran d'accueil |
| 20–22 | R&D photo d'étagère *(si go)* — sinon capacité rebasculée sur E5 et la réserve |
| 23–24 | Stabilisation, bilan, décision v3 sur le modèle économique |

---

## 4. Jalons et démonstrations à la direction

| Date | Jalon | Ce qui est montré |
|---|---|---|
| 2026-11-17 | Fin sprint 2 | Migration testée, parcours instrumentés verts, premier bundle signé |
| 2026-12-15 | Fin sprint 4 | **Scan d'un album inconnu en mode avion** — le moment de bascule du projet |
| 2027-01-12 | **v2.0** | Application sur le téléphone de 12 testeurs, catalogue complet, portes vertes |
| 2027-04-06 | **v2.1** | Publication publique, fiche store, F-Droid |
| 2027-05-04 | Sprint 14 | Résultat chiffré du go/no-go R&D, décision assumée en séance |
| 2027-09-28 | **v3.0** | Bilan 12 mois, données d'usage du panel, recommandation économique |

**Note d'animation.** La démonstration du 2026-12-15 est la plus importante des six.
Passer un téléphone en mode avion devant la direction, scanner un album acheté le matin
même, et voir apparaître « Thorgal — Tome 40 — Le Feu écarlate » sans réseau : c'est
là que le budget devient visible. Tout H1 est séquencé pour que cette démonstration
tienne à cette date.

---

## 5. Affectation par personne

| Indicatif | H1 (s1–6) | H2 (s7–12) | H3 (s13–24) |
|---|---|---|---|
| CHARPENTE | Domaine, `catalog.db`, migrations, performance | Modèle de données E3/E4 | Widget, performance à 2 000 albums |
| SOURCE | **E1 de bout en bout** | Fraîcheur, sorties automatiques | Extension manga *(si seuil atteint)* |
| LISIBILITÉ | Audit, design system, ADR-009 | Parcours d'amorçage, fiche store | Étagère vivante, statistiques |
| ŒIL | *(arrivée s2)* corpus, étude OCR | **E2 de bout en bout** | R&D photo d'étagère, sous plafond |
| FILET | Portes G1–G5, chaîne de release | Publication, F-Droid | Exploitation, incidents, ferme d'appareils |
| MAIN | D1, restauration, verdict enrichi | Imports, transfert d'appareil | E5, E7 |
| BOUSSOLE | Panel, corpus EAN, tests d'usage | Fiche store, retours terrain | Bilan, arbitrage v3 |
| GARDE-FOU | Licences, INPI, Data Safety | Conformité EAA | Veille, revue de release |

---

## 6. Ce qui est explicitement hors périmètre 12 mois

Décidé en R7. Ces sujets ne sont pas « plus tard », ils sont **refusés**, avec motif.
Les réouvrir exige un ADR.

| Sujet | Motif |
|---|---|
| Synchronisation multi-appareils par cloud | Exige un compte → viole P3. Remplacé par le transfert local (E4). |
| Recommandations, « vous aimerez aussi » | Exige de savoir ce que les gens lisent → viole P3. |
| Estimation de la valeur d'une collection | Données de cote non libres → contredit ADR-004. |
| Mode multi-utilisateur | Complexité disproportionnée pour un usage familial marginal. |
| Notes, avis, communauté | Autre produit. Bien servi ailleurs. |
| Thème personnalisable | Coût d'accessibilité (contrastes non garantis) supérieur au bénéfice. |
| Internationalisation | Un catalogue franco-belge ne se traduit pas. Réexamen T3 2027. |
| Manga / comics | Normalisation d'une autre nature. Conditionné au succès franco-belge. |

---

## 7. Suivi des risques

Revus au comité direction mensuel. Sortie de la zone verte = point à l'ordre du jour.

| Risque | Indicateur suivi | Seuil d'alerte | Mesuré par |
|---|---|---|---|
| R1 Licences | Avis GARDE-FOU à jour sur chaque source du bundle | Toute source non couverte | GARDE-FOU |
| R2 Poids du catalogue | Taille du bundle compressé | > 15 Mo | CI du pipeline |
| R3 Dérive produit | Nombre de demandes contredisant P1–P5 | ≥ 2 par trimestre | Moi |
| R4 Dépendance à une personne | Sujets sans binôme documenté | ≥ 1 | Moi |
| R5 Marque | Recherche d'antériorité rendue | Non rendue au sprint 6 | GARDE-FOU |
| **Nouveau** — couverture BnF | Taux de résolution sur corpus de 500 EAN | < 85 % | SOURCE, mensuel |

**Clause d'arrêt sur E1.** Si le taux de résolution mesuré au sprint 2 est inférieur à
**75 %**, le pari n°1 est réexaminé en comité direction avant l'engagement des 24
sprints. Nous mesurons avant de dépenser, pas après.
</content>

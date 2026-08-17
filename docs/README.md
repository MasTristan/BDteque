# Documentation du projet — BDteque v2

> Reprise de projet, vision et spécifications de développement.
> Gel des spécifications : **2026-10-15**. Sprint 1 : **2026-10-20**.

---

## Par où commencer

| Vous êtes… | Lisez d'abord |
|---|---|
| **La direction** | [VISION.md](VISION.md) puis [ROADMAP.md](ROADMAP.md) |
| **Un développeur qui arrive** | [specs/README.md](specs/README.md) puis l'épopée qui vous concerne |
| **Un développeur qui touche à la base** | [specs/CONTRATS-DE-DONNEES.md](specs/CONTRATS-DE-DONNEES.md) |
| **Un designer** | [specs/DESIGN-SYSTEM.md](specs/DESIGN-SYSTEM.md) |
| **Quelqu'un qui se demande « pourquoi ce choix ? »** | [DECISIONS.md](DECISIONS.md) |
| **Quelqu'un qui veut comprendre l'existant** | [v1/SPEC-V1.md](v1/SPEC-V1.md) |

---

## Les documents

### Cadrage

| Document | Contenu |
|---|---|
| [VISION.md](VISION.md) | État des lieux, thèse produit, cinq piliers, trois paris, risques |
| [TEAM.md](TEAM.md) | Les 8 recrutements, mandats, ce qui n'a pas été recruté, rituels |
| [COMITES.md](COMITES.md) | Comptes rendus des 9 réunions de cadrage, avec les arbitrages réels |
| [DECISIONS.md](DECISIONS.md) | 12 décisions d'architecture (ADR-001 à 012) |
| [ROADMAP.md](ROADMAP.md) | Priorisation RICE, séquencement sur 24 sprints, jalons, suivi des risques |

### Spécifications de développement

| Réf | Épopée | Horizon |
|---|---|---|
| [E1](specs/E1-catalogue.md) | Le Catalogue — référentiel BD embarqué | H1 |
| [E2](specs/E2-scan.md) | Le Verdict — un scan qui ne renonce jamais | H2 |
| [E3](specs/E3-amorcage.md) | L'Amorçage — de 0 à 2 000 albums | H2 |
| [E4](specs/E4-coffre.md) | Le Coffre — durabilité de la collection | H1 |
| [E5](specs/E5-partage.md) | Prêts, souhaits & partage | H3 |
| [E6](specs/E6-qualite.md) | Qualité, accessibilité & exploitation | H1+ |
| [E7](specs/E7-etagere.md) | L'Étagère vivante | H3 |
| [E8](specs/E8-distribution.md) | Distribution & marque | H1→H2 |

### Références

| Document | Contenu |
|---|---|
| [specs/README.md](specs/README.md) | Conventions de spec, « prêt à développer », « terminé », budgets |
| [specs/CONTRATS-DE-DONNEES.md](specs/CONTRATS-DE-DONNEES.md) | Schémas de base, migrations, formats de fichiers, chaîne de confiance |
| [specs/DESIGN-SYSTEM.md](specs/DESIGN-SYSTEM.md) | Jetons mesurés, typographie, états, langue |

### Historique

| Document | Contenu |
|---|---|
| [v1/SPEC-V1.md](v1/SPEC-V1.md) | Spécification fondatrice de la v1 — la référence des « §6.4 » du code |
| [v1/CLAUDE-V1.md](v1/CLAUDE-V1.md) | Mémo d'implémentation de la v1, conventions de code |

---

## Les cinq piliers, en une page

Toute décision produit se juge contre ces cinq lignes. Une fonctionnalité qui en viole
une est refusée, quel que soit son intérêt.

1. **Le verdict en trois secondes.** La boucle « scanner → savoir » est le produit.
2. **Hors-ligne d'abord, absolument.** Le réseau améliore, il ne conditionne jamais.
3. **Aucun compte, aucune collecte, jamais.** On mesure par la recherche, pas par la surveillance.
4. **L'accessibilité est une fonctionnalité.** WCAG 2.2 AA + EN 301 549, vérifié en CI.
5. **Durabilité à dix ans.** Aucun service payant, aucun format fermé, aucun serveur.

---

## Les trois moitiés, en un schéma

```
Moitié A — LA COLLECTION            Moitié B — LES SORTIES        Moitié C — LE CATALOGUE
Room · bdshelf.db                   JSON · filesDir               Room · catalog.db
IRREMPLAÇABLE                       jetable                       jetable
```

Aucune clé étrangère, jointure SQL ou transaction ne traverse ces frontières.
Le croisement se fait en mémoire, dans le domaine, par identifiant.
</content>

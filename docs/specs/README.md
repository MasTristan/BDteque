# SPÉCIFICATIONS DE DÉVELOPPEMENT — v2

> **Gelées le 2026-10-15** en [comité R8](../COMITES.md#r8--revue-de-spécifications--gel).
> Toute modification ultérieure passe par une entrée dans [DECISIONS.md](../DECISIONS.md)
> et une revue de 30 minutes.

---

## Index

| Réf | Épopée | Horizon | Coût | Statut |
|---|---|---|---|---|
| [E1](E1-catalogue.md) | Le Catalogue — référentiel BD embarqué | H1 | 24 sp | Prêt |
| [E2](E2-scan.md) | Le Verdict — un scan qui ne renonce jamais | H2 | 16 sp | Prêt |
| [E3](E3-amorcage.md) | L'Amorçage — de 0 à 2 000 albums | H2 | 14 sp | Prêt |
| [E4](E4-coffre.md) | Le Coffre — durabilité de la collection | H1 | 10 sp | Prêt |
| [E5](E5-partage.md) | Prêts, souhaits & partage | H3 | 10 sp | Prêt |
| [E6](E6-qualite.md) | Qualité, accessibilité & exploitation | H1+ | 16 sp | Prêt |
| [E7](E7-etagere.md) | L'Étagère vivante | H3 | 12 sp | Prêt |
| [E8](E8-distribution.md) | Distribution & marque | H1→H2 | 6 sp | Prêt |
| — | [Contrats de données](CONTRATS-DE-DONNEES.md) | — | — | Référence |
| — | [Design system](DESIGN-SYSTEM.md) | — | — | Référence |

---

## Comment lire une spécification

Chaque épopée suit la même structure :

1. **Pourquoi** — le problème utilisateur, relié à un constat de terrain ou à une dette.
2. **Périmètre** — ce qui est dans, ce qui est dehors. Les deux sont normatifs.
3. **Récits utilisateur** — `US-xx`, chacun avec ses critères d'acceptation.
4. **Conception technique** — modèle de données, contrats, algorithmes, erreurs.
5. **Interface & accessibilité** — écrans, états, exigences a11y spécifiques.
6. **Plan de test** — ce qui doit être vert avant de dire « terminé ».
7. **Risques & questions ouvertes** — nommés, avec un titulaire.

Les critères d'acceptation sont écrits en **Étant donné / Quand / Alors**. Ils sont
directement traduisibles en test : un critère qu'on ne sait pas tester est un critère
mal écrit, et il est renvoyé en revue.

---

## Définition de « prêt à développer » (DoR)

Une spécification n'entre pas en sprint sans ces 8 points :

1. Le problème utilisateur est relié à un constat de terrain ou à une dette identifiée.
2. Le périmètre nomme explicitement ce qui est **hors** périmètre.
3. Chaque récit a des critères d'acceptation testables.
4. Le **comportement hors-ligne** est spécifié pour chaque interaction réseau.
5. Le **comportement en erreur** est spécifié : réseau coupé, disque plein, donnée
   corrompue, permission refusée, interruption en cours d'opération.
6. Les impacts sur le schéma de base sont décrits **avec leur migration**.
7. Les exigences d'accessibilité spécifiques sont listées.
8. Les budgets de performance applicables sont chiffrés.

Le point 5 est celui qui fait le plus souvent échouer la revue. C'est volontaire :
c'est la marque de fabrique de ce projet depuis sa v1, et elle ne se perd pas.

---

## Définition de « terminé » (DoD)

Une tâche est terminée quand **tout** ce qui suit est vrai :

- [ ] Les critères d'acceptation sont vérifiés sur **appareil réel** (pas émulateur).
- [ ] Tests unitaires écrits pour toute logique de domaine ajoutée.
- [ ] Test instrumenté écrit si un parcours critique est touché.
- [ ] Aucune chaîne de caractères codée en dur dans le Compose — tout dans `strings.xml`,
      nommé `ecran_element_description`.
- [ ] Description de contenu présente sur tout élément interactif ou porteur de sens.
- [ ] Rendu vérifié à `fontScale` 1,0 / 1,5 / **2,0**.
- [ ] Rendu vérifié en thème clair **et** sombre.
- [ ] Comportement en mode avion vérifié.
- [ ] Aucune couleur hors jetons du [design system](DESIGN-SYSTEM.md).
- [ ] Les 5 portes de qualité ([ADR-005](../DECISIONS.md#adr-005)) sont vertes.
- [ ] Documentation mise à jour si un contrat de données change.

---

## Conventions de code (héritées, confirmées)

Reprises de la v1 et **non négociables** — elles sont la raison pour laquelle ce code
est encore lisible après trois ans.

| Domaine | Règle |
|---|---|
| Architecture | `ViewModel → Repository → (Dao | Api)`. Jamais de DAO dans un écran. Jamais de logique métier dans un composable. |
| État | Un seul `StateFlow<XxxUiState>` par ViewModel, plus des fonctions `onXxx()`. Aucun état mutable dans l'écran. |
| Flux | `collectAsStateWithLifecycle()`, jamais `collectAsState()`. |
| Chaînes | Toutes dans `strings.xml`, y compris les mots isolés. Convention : `ecran_element_description`. |
| Langue | Interface intégralement en **français**, sans jargon. « code-barres », pas « EAN ». « mettre à jour les nouveautés », pas « synchroniser ». |
| Identifiants d'album | `"${seriesId}-${tomeNumber}"`. Hors-série : `"${seriesId}-hs-${index}"`. |
| Cibles tactiles | `Modifier.defaultMinSize(minHeight = 56.dp)` sur tout composant interactif. |
| Interdits | `Thread.sleep`, `runBlocking` dans le code d'interface, `TODO` silencieux. |
| Fichiers | Une classe publique principale par fichier, nommé comme elle. |
| Écriture disque | Toute écriture de fichier durable est **atomique** (temporaire + renommage). Déjà appliqué aux couvertures et aux sauvegardes ; c'est la règle générale. |
| Dégradation | Toute opération réseau ou disque échoue **silencieusement et proprement** : l'usage principal n'est jamais bloqué par une fonction d'appoint. |

---

## Les trois moitiés — rappel normatif

C'est la règle d'architecture la plus importante du projet. Toute violation est un
défaut bloquant, pas une préférence de style.

```
Moitié A — LA COLLECTION            Moitié B — LES SORTIES        Moitié C — LE CATALOGUE
Room · bdshelf.db                   JSON · filesDir               Room · catalog.db
Propriété de l'utilisateur          Donnée curée, signée          Donnée publique, signée
IRREMPLAÇABLE                       jetable                       jetable
sauvegardée, exportée               ni sauvegardée ni exportée    ni sauvegardée ni exportée
lecture/écriture                    lecture seule                 lecture seule
```

**Interdits absolus**
- Aucune clé étrangère, jointure SQL ou transaction traversant A↔B, A↔C ou B↔C.
- Aucune entité de B ou C stockée dans la base de A.
- Aucun élément de A dans un export de B ou C.

**Autorisé** : le croisement **en mémoire, dans le domaine**, par identifiant — comme le
fait déjà `buildShoppingList()`, qui combine albums (A) et sorties (B) sans jamais les
mélanger en base. C'est le modèle à reproduire.

---

## Budgets opposables

Un dépassement rend le build rouge ([ADR-005](../DECISIONS.md#adr-005)).

| Budget | Valeur | Mesuré sur |
|---|---|---|
| Démarrage à froid | < 1,2 s | Galaxy A14 |
| Scan → verdict (p95, hors-ligne) | < 1,5 s | Galaxy A14 |
| Caméra prête après ouverture du scanner | < 800 ms | Galaxy A14 |
| Recherche dans le catalogue (35 000 albums) | < 150 ms | Galaxy A14 |
| Fluidité de l'étagère à 2 000 albums | 0 image perdue | Pixel 6a |
| Taille de l'APK hors catalogue | < 30 Mo | CI |
| Bundle catalogue compressé | < 15 Mo | CI du pipeline |
| Catalogue installé sur disque | < 60 Mo | CI |
| Durée de la CI | < 15 min | CI |
</content>

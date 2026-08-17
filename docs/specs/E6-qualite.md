# E6 — QUALITÉ, ACCESSIBILITÉ & EXPLOITATION

> Ce qui ne se voit pas, et sans quoi rien d'autre ne tient.
> **16 sprints-personne · H1 (sprints 1–5) puis continu · pilote : FILET, avec LISIBILITÉ**
> Décisions applicables : [ADR-005](../DECISIONS.md#adr-005), [ADR-009](../DECISIONS.md#adr-009), [ADR-010](../DECISIONS.md#adr-010)

---

## 1. Pourquoi

Trois faits, vérifiés dans le dépôt :

1. **Aucun test instrumenté.** Le répertoire `app/src/androidTest` n'existe pas, alors
   que les dépendances Espresso et JUnit Android sont déclarées dans `build.gradle.kts`.
   Aucun parcours utilisateur n'est vérifié de bout en bout.
2. **Les schémas Room ne sont pas versionnés.** `exportSchema = true` et
   `schemaDirectory` sont configurés, mais `app/schemas/` n'est pas au dépôt. La
   migration 1→2 déjà livrée n'a **jamais** été vérifiée contre son schéma de référence.
   Elle est probablement correcte — écrite à la main, lisible. Nous n'en avons aucune preuve.
3. **Aucune chaîne de release.** Pas de signature, pas de `minify`, pas d'AAB,
   `versionCode = 1`. Le produit ne peut pas atteindre un deuxième utilisateur.

À cela s'ajoutent six anomalies d'accessibilité relevées à l'audit
([R5](../COMITES.md#r5--design-system--accessibilité)) — sur une base par ailleurs
nettement au-dessus de la moyenne du marché.

FILET l'a formulé en comité, et la phrase est restée : *« une migration ratée sur cette
application, ce n'est pas un crash, c'est la collection d'un homme de 70 ans. »*

---

## 2. Périmètre

### Dans le périmètre
- Schémas Room versionnés + tests de migration, rétroactivement.
- Source `androidTest` et 5 parcours critiques instrumentés.
- CI complète : analyse statique, tests, accessibilité, budgets, taille.
- Les 5 portes de qualité bloquantes.
- Correction des 6 anomalies d'accessibilité, dont la vue liste ([ADR-009](../DECISIONS.md#adr-009)).
- Banc de performance sur appareils de référence.
- Journal d'incident local et envoi manuel ([ADR-010](../DECISIONS.md#adr-010)).
- Chaîne de release signée.

### Hors périmètre
- Tout SDK de collecte, y compris « anonymisé » ([ADR-010](../DECISIONS.md#adr-010)).
- Ferme d'appareils commerciale (coût disproportionné ; deux appareils physiques).
- Couverture de code chiffrée comme objectif — nous couvrons des **parcours**, pas des
  lignes. Un pourcentage élevé sur du code trivial ne prouve rien.

---

## 3. Les 5 portes de qualité

Aucune n'est consultative. Le contournement exige un ADR nominatif et daté
([ADR-005](../DECISIONS.md#adr-005)).

| Porte | Critère | Outil |
|---|---|---|
| **G1** Tests unitaires | Suite JVM verte | `./gradlew test` |
| **G2** Parcours critiques | 5 tests instrumentés verts | `connectedAndroidTest` sur émulateur API 26 + 35 |
| **G3** Accessibilité | 0 anomalie bloquante ; rendus capturés à `fontScale` 1,0 / 1,5 / 2,0 | Accessibility Test Framework + captures |
| **G4** Performance | Démarrage < 1,2 s · verdict p95 < 1,5 s · APK < 30 Mo | Macrobenchmark + `apkanalyzer` |
| **G5** Migration | Migration testée pour chaque version de base, de 1 à N | `room-testing`, `MigrationTestHelper` |

---

## 4. Les 5 parcours critiques

Ce sont les parcours dont la rupture rend le produit inutile ou destructeur.

| # | Parcours | Pourquoi critique |
|---|---|---|
| **P1** | Premier lancement → prénom → collection utilisable | Sans lui, aucun utilisateur n'existe |
| **P2** | Scan → verdict → ajout à la collection | La boucle centrale du produit |
| **P3** | Ouvrir une série → voir ses trous → basculer un tome | L'usage librairie |
| **P4** | Export → installation neuve → import → **collection identique** | La survie des données |
| **P5** | Mise à jour du catalogue → verdict enrichi hors-ligne | La promesse de la v2 |

Chacun est instrumenté, exécuté à chaque PR, sur API 26 (minimum supporté) et API 35.

---

## 5. Conception technique

### 5.1 Schémas et migrations

```
app/schemas/com.bdshelf.app.data.local.AppDatabase/
  1.json    ← reconstruit à partir des entités de la v1
  2.json    ← état actuel
  3.json    ← + series.catalogSeriesId  (E1)
  4.json    ← + albums.lentTo, lentAt, wishlisted  (E5)
```

```kotlin
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test fun migrate1To2_preservesCollection() { … }
    @Test fun migrate2To3_addsCatalogLink() { … }
    @Test fun migrateAll_fromV1_toLatest() { … }   // le chemin réel d'un ancien utilisateur
}
```

**Règles**
- Chaque migration est testée sur une base **peuplée**, pas vide. Une migration qui
  passe sur une base vide ne prouve rien.
- `fallbackToDestructiveMigration()` est **interdit** dans tout le projet. Vérifié par
  une règle d'analyse statique, pas par la vigilance humaine.
- Toute PR modifiant une entité sans migration ni schéma est refusée automatiquement.

### 5.2 Chaîne d'intégration continue

```yaml
# évolution de .github/workflows/
statique:      ktlint · detekt · android lint (0 avertissement toléré sur le code neuf)
unitaires:     ./gradlew test  (module :core:domain compris — quelques secondes)
instrumentés:  émulateurs API 26 et 35, 5 parcours critiques
accessibilité: Accessibility Test Framework + captures fontScale 1,0/1,5/2,0
performance:   Macrobenchmark — démarrage, verdict, fluidité de l'étagère
budgets:       taille de l'APK, taille du bundle, durée de la CI
release:       AAB signé (tag uniquement), versionCode issu de la CI
```

**Budget de durée : < 15 min.** Au-delà, l'équipe contourne la CI, c'est une constante
humaine. Parallélisation et cache Gradle sont des exigences, pas des optimisations.

### 5.3 Les 6 anomalies d'accessibilité

| # | Anomalie | Correction | Sprint |
|---|---|---|---|
| A-01 | Contrastes non revérifiés depuis l'ajout du thème sombre | Mesure des 14 jetons dans les deux thèmes, correction, **test automatisé de contraste** | 3 |
| A-02 | Étagère horizontale = tunnel en TalkBack | Vue liste équivalente ([ADR-009](../DECISIONS.md#adr-009)) + accès direct au tome N | 3–4 |
| A-03 | Aucune vue testée au-delà de `fontScale 1.5` | Captures automatisées à 2,0 sur les 8 écrans principaux ; grilles qui se réorganisent au lieu de tronquer | 4 |
| A-04 | États de tranche codés uniquement par couleur et forme | Description vocale d'état en toutes lettres, sur chaque tranche | 3 |
| A-05 | Ordre de focus par défaut plaçant l'action principale en dernier | Ordre de parcours déclaré explicitement sur verdict, formulaires et réglages | 4 |
| A-06 | Messages d'erreur non annoncés | `liveRegion` sur les zones de message ; annonce à l'apparition | 4 |

**Cible officielle : WCAG 2.2 AA + EN 301 549.** Depuis l'entrée en application de
l'*European Accessibility Act* en juin 2025, une application grand public publiée dans
l'Union est susceptible d'y être soumise ; le veto de LISIBILITÉ cesse d'être une
politique interne pour devenir une exigence de conformité, donc non arbitrable en fin
de sprint (analyse GARDE-FOU, [R5](../COMITES.md#r5--design-system--accessibilité)).

### 5.4 Incidents sans télémétrie

```kotlin
// Thread.setDefaultUncaughtExceptionHandler
// → filesDir/crashes/crash_<horodatage>.txt
// Contenu : pile d'appels, version de l'app, version d'Android, modèle d'appareil.
// JAMAIS : identifiant, contenu de la collection, chemin de fichier utilisateur.
```

Au démarrage suivant :

```
L'application s'est arrêtée la dernière fois.
Veux-tu envoyer le rapport pour qu'on corrige le problème ?

[ Voir le rapport ]  [ Envoyer ]  [ Non merci ]
```

- « Voir le rapport » affiche **l'intégralité** du contenu avant tout envoi. On ne
  demande pas de faire confiance, on montre.
- L'envoi passe par le sélecteur système : nous ne choisissons ni le canal, ni le
  destinataire.
- Aucun envoi automatique. Aucun identifiant. Aucun SDK.
- Purge automatique après 30 jours.

### 5.5 Appareils de référence

| Appareil | Rôle | Caractéristiques |
|---|---|---|
| **Galaxy A14** | Cible **basse** — celle qui compte | 4 Go de RAM, écran 720p, Android 14 |
| **Pixel 6a** | Cible haute | Android 16, référence de fluidité |

Les budgets de performance sont mesurés sur le **Galaxy A14**. Optimiser sur un appareil
haut de gamme reviendrait à optimiser pour quelqu'un qui n'est pas notre utilisateur.
Aucune démonstration de fin de sprint sur émulateur n'est recevable.

---

## 6. Chaîne de release

```
commit → CI (G1–G5) → tag v2.x.y → AAB signé → piste interne → fermée → ouverte
                                             → APK reproductible → F-Droid
```

- Clé de signature : secret de CI, sauvegardée hors ligne, procédure de perte écrite.
- `versionCode` dérivé du numéro de build CI, jamais saisi à la main.
- `minifyEnabled = true` en release, avec règles ProGuard vérifiées sur un build réel
  (Room, kotlinx.serialization et ML Kit exigent des règles de conservation — une
  release minifiée non testée casse silencieusement la sérialisation).
- Notes de version rédigées **en français, pour un lecteur non technique**.
- Chaque release s'accompagne du rituel export → installation neuve → import
  ([D-R6-5](../COMITES.md#r6--qualité-livraison--incidents)).

---

## 7. Plan de test *(de la qualité elle-même)*

- **Test de la porte G5** : introduire volontairement une migration fautive → la CI
  doit devenir rouge. Une porte qu'on n'a jamais vue bloquer n'est pas une porte prouvée.
- **Test de la porte G3** : introduire un contraste insuffisant → la CI doit le détecter.
- **Test du journal d'incident** : provoquer un plantage → vérifier le contenu du
  fichier, et **l'absence** de tout identifiant.
- **Test de la release minifiée** : installer l'AAB de release sur les deux appareils,
  exécuter les 5 parcours critiques à la main.

---

## 8. Ce qui prouve que c'est terminé

- [ ] `app/schemas/` contient tous les schémas, de la version 1 à la courante.
- [ ] Le chemin de migration complet d'un utilisateur de la v1 est testé sur base peuplée.
- [ ] Les 5 parcours critiques sont verts sur API 26 et API 35.
- [ ] Les 6 anomalies d'accessibilité sont corrigées et couvertes par des tests.
- [ ] Une migration fautive introduite volontairement rend la CI rouge.
- [ ] Un plantage produit un rapport local sans identifiant, jamais envoyé sans accord.
- [ ] Une release signée s'obtient en une commande, à partir d'un tag.
- [ ] La CI dure moins de 15 minutes.
</content>

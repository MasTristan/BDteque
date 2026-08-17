# Schémas Room exportés

> Généré automatiquement par Room à la compilation (`room { schemaDirectory(...) }`,
> voir `app/build.gradle.kts`). **Ne pas éditer à la main.**

Ce dossier est vide pour l'instant : aucune compilation complète (KSP) n'a encore été
commise depuis que `schemaDirectory` a été configuré. Le prochain build réussi de
`:app` (CI ou poste avec le SDK Android) écrira ici `2.json`, reflet exact du schéma
actuel (`Series`, `Album`, `CachedIsbnLookup`) — il devra être committé. La porte de
qualité de la CI (`.github/workflows/build-debug-apk.yml`, étape *Verify Room schemas
are committed*) échoue tant que ce n'est pas fait, pour que ça ne puisse plus être
oublié.

**`1.json` n'existe pas et ne peut pas être reconstruit fidèlement après coup** — son
`identityHash` dépend du compilateur Room tel qu'il tournait à l'époque, pas d'une
description SQL qu'on pourrait recopier à la main. Ce n'est pas grave : la migration
1→2 (`AppDatabase.MIGRATION_1_2`) est vérifiée sans lui, par un test qui rejoue le SQL
réel de la version 1 et appelle la vraie migration de production — voir
`app/src/androidTest/java/com/bdshelf/app/data/local/MigrationTest.kt` et son
commentaire d'en-tête pour le détail du raisonnement.

À partir de `3.json` (prochaine migration, `series.catalogSeriesId` — voir
[E1](../../docs/specs/E1-catalogue.md)), la voie normale s'applique : le schéma est
exporté par la CI, committé, et les tests de migration utilisent
`androidx.room.testing.MigrationTestHelper` (déjà câblé : voir
`androidTest.assets.srcDirs` dans `app/build.gradle.kts` et la dépendance
`androidx.room:room-testing`).

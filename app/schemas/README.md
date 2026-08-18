# Schémas Room exportés

> Généré automatiquement par Room à la compilation (`room { schemaDirectory(...) }`,
> voir `app/build.gradle.kts`). **Ne pas éditer à la main.**

`com.bdshelf.app.data.local.AppDatabase/2.json` est le schéma réel généré par Room/KSP
pour la version 2 actuelle (`Series`, `Album`, `CachedIsbnLookup`), récupéré depuis un
run de CI (le SDK Android et le réseau nécessaires au moissonnage KSP n'étaient pas
accessibles dans l'environnement de développement qui a écrit ce commit). La porte de
qualité de la CI (`.github/workflows/build-debug-apk.yml`, étape *Verify Room schemas
are committed*) échoue désormais si une future compilation produit un schéma différent
de celui-ci sans qu'il soit committé, pour que ça ne puisse plus être oublié.

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

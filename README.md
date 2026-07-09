# BDShelf

![Build debug APK](https://github.com/MasTristan/BDteque/actions/workflows/build-debug-apk.yml/badge.svg)

Application Android pour gérer une collection de bandes dessinées, pensée pour
être utilisée sans effort par une personne non technophile. Elle fonctionne
entièrement hors ligne, sans compte, sans publicité et sans collecte de
données.

> Le dépôt s'appelle `BDteque`, l'application `BDShelf` (le nom de code du
> projet). Les deux désignent la même chose.

## À quoi ça sert

- Voir sa collection sous forme d'étagères : chaque série est une rangée de
  tranches de livres, les tomes manquants apparaissent en creux.
- Scanner le code-barres d'un album en librairie pour savoir immédiatement si
  on le possède déjà ou s'il manque à la collection.
- Suivre les nouveautés à paraître des séries que l'on suit, avec une
  notification quand un nouveau tome sort.
- Sauvegarder et restaurer sa collection dans un fichier (JSON ou CSV lisible).

L'accessibilité est un objectif de premier plan : grandes cibles tactiles,
police très lisible (Atkinson Hyperlegible), respect de la taille de texte
système, libellés en langage clair.

## Choix techniques

- **100 % hors ligne.** Aucune image réseau, aucun scraping, aucun compte.
- **Séparation stricte** entre la collection (base locale) et les sorties à
  paraître (fichier JSON distant mis en cache), qui ne se mélangent jamais.
- **Aucune dépendance à un service payant.**

## Stack

| Domaine | Technologie |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Base de données | Room |
| Préférences | DataStore |
| Tâches de fond | WorkManager |
| Scan de code-barres | CameraX + ML Kit (modèle embarqué, hors ligne) |
| Sérialisation | kotlinx.serialization |
| Injection | manuelle (pas de Hilt) |

- minSdk 26, targetSdk 35
- Module unique, thème clair uniquement, versions épinglées
  (`gradle/libs.versions.toml`).

## Construire le projet

Prérequis : JDK 17 et le SDK Android (via Android Studio ou
`android-actions/setup-android` en CI).

```bash
# APK de debug
./gradlew assembleDebug

# Tests unitaires JVM
./gradlew test

# Installer sur un appareil connecté
./gradlew installDebug
```

L'APK produit se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
La CI GitHub Actions construit l'APK et lance les tests à chaque push sur
`main` et sur chaque pull request.

## Les sorties à paraître

L'écran « À paraître » lit un fichier `releases.json` distant (par défaut celui
à la racine de ce dépôt) puis le met en cache localement. Le format est décrit
dans ce fichier. L'URL est modifiable dans les réglages avancés de
l'application.

## Licence

MIT, voir [LICENSE](LICENSE).

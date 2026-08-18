# Add project specific ProGuard rules here.
# https://developer.android.com/studio/build/shrinker

# kotlinx.serialization (§E6 6) : sans ces règles, la sérialisation des
# entités @Serializable (sauvegarde/export, bundle catalogue, cache des
# sorties) casse SILENCIEUSEMENT en release — R8 supprime les sérialiseurs
# synthétiques générés par le plugin de compilation faute de les savoir
# utilisés par réflexion. Règles reprises telles quelles de la documentation
# officielle (https://github.com/Kotlin/kotlinx.serialization#android).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bdshelf.app.**$$serializer { *; }
-keepclassmembers class com.bdshelf.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.bdshelf.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager (BackupWorker, ReleasesSyncWorker) instancie les Worker par
# réflexion (Class.forName sur le nom stocké en base WorkSpec) : sans cette
# règle, R8 peut renommer la classe et faire planter l'exécution en tâche de
# fond, invisible tant qu'on n'a pas attendu le prochain run planifié.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ML Kit barcode scanning : défensif, faute de pouvoir vérifier le
# comportement à l'exécution en release dans cet environnement (pas
# d'appareil connecté). Room et AndroidX n'ont pas besoin de règles ici :
# leurs artefacts embarquent leurs propres règles de conservation.
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }

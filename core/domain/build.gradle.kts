import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// §ADR-002 : module Kotlin pur, aucune dépendance Android. Le compilateur
// interdit ce qu'une revue de code ne peut qu'espérer empêcher — un
// `import android.*` ou `androidx.*` ici est une erreur de compilation dans
// :app, pas un oubli qui passe en revue.

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bouncycastle.prov)

    testImplementation(libs.junit)
}

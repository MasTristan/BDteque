import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
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

java {
    // Le plugin Kotlin/JVM applique aussi le plugin Java, dont compileJava
    // suit par défaut le JDK du toolchain (17 en CI) — sans ceci,
    // compileJava (17) et compileKotlin (11, ci-dessus) sont incohérents.
    // Même cible que :app (compileOptions, app/build.gradle.kts).
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bouncycastle.prov)

    testImplementation(libs.junit)
}

// Porte de qualité (§E6 5.2) : voir app/build.gradle.kts pour la justification.
detekt {
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

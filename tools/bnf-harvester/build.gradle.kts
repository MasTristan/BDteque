import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// Outil, pas produit : jamais embarqué dans l'app (§ADR-004, le pipeline de
// construction du bundle est un chantier séparé de l'app elle-même). Exécuté
// uniquement en CI, où le réseau existe — voir .github/workflows/harvest-bnf.yml.
// Absence délibérée de detekt/lint ici : ce module ne livre rien à
// l'utilisateur, seulement des données à committer sous contrôle humain.

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    // Le plugin `application` applique aussi le plugin Java, dont compileJava
    // suit le JDK du toolchain (17 en CI) — même piège que :core:domain
    // (build.gradle.kts, "Inconsistent JVM-target compatibility") si les deux
    // ne concordent pas explicitement.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.bdshelf.tools.bnfharvester.MainKt")
}

dependencies {
    implementation(project(":core:domain"))
}

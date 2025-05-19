// build.gradle.kts

plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "com.emailprocessor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // Apache Camel core
    implementation("org.apache.camel:camel-core:4.0.0")
    implementation("org.apache.camel:camel-main:4.0.0") 
    
    // Camel components
    implementation("org.apache.camel:camel-mail:4.0.0")      // Obsługa email
    implementation("org.apache.camel:camel-http:4.0.0")      // Komunikacja HTTP dla LLM API
    implementation("org.apache.camel:camel-jacksonxml:4.0.0") // Serializacja JSON
    implementation("org.apache.camel:camel-jdbc:4.0.0")      // Dostęp do bazy danych
    
    // SQLite dla prostego przechowywania danych
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    
    // Logowanie
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    
    // HTTP Client dla komunikacji z LLM API
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    
    // JSON parsing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    
    // Testowanie
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("com.emailprocessor.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

// Zadanie do kopiowania zależności do folderu lib
tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory}/libs/dependencies")
}

// Uruchomienie zadania copyDependencies po kompilacji
tasks.named("build") {
    finalizedBy("copyDependencies")
}

// Konfiguracja dla uruchomienia aplikacji
tasks.named<JavaExec>("run") {
    // Przekazanie zmiennych środowiskowych z pliku .env jeśli istnieje
    doFirst {
        val envFile = file(".env")
        if (envFile.exists()) {
            val props = java.util.Properties()
            props.load(envFile.inputStream())
            props.forEach { key, value ->
                environment(key.toString(), value)
            }
        }
    }
}
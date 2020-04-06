import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.3.50"
    kotlin("plugin.spring") version "1.3.50"
    id("org.springframework.boot") version "2.1.7.RELEASE"
}

val jacksonVersion = "2.9.9"
val retrofitVersion = "2.6.0"

version = "1.0-SNAPSHOT"
application {
    mainClassName = "org.gravetti.stw.main.ApplicationKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "com.squareup.retrofit2", name = "retrofit", version = retrofitVersion)
    implementation(group = "com.squareup.retrofit2", name = "converter-jackson", version = retrofitVersion)
    implementation(group = "commons-io", name = "commons-io", version = "2.6")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter", version = "2.1.7.RELEASE")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = jacksonVersion)
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)
    implementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion)
    implementation(group = "com.github.ajalt", name = "clikt", version = "2.1.0")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "1.6.26")
    implementation(group = "org.apache.commons", name = "commons-csv", version = "1.5")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

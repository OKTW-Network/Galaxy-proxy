import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlinVersion = "1.3.21"
    repositories { jcenter() }

    dependencies {
        classpath("org.jetbrains.kotlin","kotlin-serialization", kotlinVersion)
    }
}

plugins {
    kotlin("jvm")
}
apply(plugin = "kotlinx-serialization")

group = "one.oktw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.mongodb", "bson", "3.10.1")
//    compile("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", "0.10.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

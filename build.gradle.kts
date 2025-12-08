import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("kapt") version "2.2.20"
    id("com.gradleup.shadow") version "9.3.0"
}

group = "one.oktw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "adventure"
    }
}

dependencies {
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("io.fabric8:kubernetes-client:7.4.0")
    implementation("io.fabric8:kubernetes-httpclient-okhttp:7.4.0")
    implementation("com.github.fkorotkov:k8s-kotlin-dsl:3.5.0")
    implementation("io.lettuce:lettuce-core:6.8.1.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")// lettuce need this
    implementation("one.oktw:galaxy-lib:f4e1b25")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.shadowJar {
    archiveClassifier = ""
    minimize {
        exclude(dependency("io.fabric8:kubernetes-client:.*"))
    }
    exclude("*.aut")
    exclude("*.properties")
    exclude("OSGI-INF")
    exclude("schema")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

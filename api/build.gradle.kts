import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

group = "one.oktw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.mongodb", "bson", "3.11.0")

    shadow(kotlin("stdlib-jdk8"))
    shadow("org.mongodb", "bson", "3.11.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = "all"
    configurations = listOf(project.configurations.shadow.get())
    exclude("META-INF")
    minimize()
}

val sourcesJar by tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.getByName<Jar>("jar") {
    dependsOn(shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("api") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(shadowJar)
        }
    }
}

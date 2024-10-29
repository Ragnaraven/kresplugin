import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(libs.plugins.jvm.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    id("java-gradle-plugin")
    id("maven-publish")
    `java-library`
    groovy
}

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.gradle.plugin)
    implementation(gradleApi())
}

group = "io.kresplugin"
version = "1.0.0"
val artifact = "kresplugin"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

gradlePlugin {
    plugins {
        create("kresplugin") {
            id = "io.kresplugin"
            implementationClass = "io.kresplugin.KResPlugin"
        }
    }
}

tasks.register("listResources") {
    doLast {
        val resourcesDir = sourceSets["main"].resources.srcDirs.single()
        project.fileTree(resourcesDir).forEach {
            println(it)
        }
    }
}
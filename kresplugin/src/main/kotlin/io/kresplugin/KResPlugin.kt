package io.kresplugin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.kresplugin.generators.FilesGenerator
import io.kresplugin.generators.StringsGenerator
import io.kresplugin.tasks.KResGenerateTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

//Get package from gradle
//Get name from gradle

//Save to build folder
//Clean and replace on build
//Gradle tasks

//getString function
class KResPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val pluginExtension = project.extensions.create("kres", KResPluginExtension::class.java)

        project.afterEvaluate {
            val kotlinExtension =
                project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            // List to store tasks for active targets
            val activeTargetTasks = mutableListOf<TaskProvider<out Task>>()

            // Keep track of registered tasks
            val registeredTasks = mutableSetOf<String>()

            // Prioritize commonMain
            val commonMainTaskName = "generateKResForCommonMain"
            if (!registeredTasks.contains(commonMainTaskName) && kotlinExtension.sourceSets.findByName(
                    "commonMain"
                ) != null
            ) {
                val generateTask = project.tasks.register(
                    commonMainTaskName,
                    KResGenerateTask::class.java
                ) { task ->
                    task.stringsRoot.set(pluginExtension.stringsRoot)
                    task.kresPackage.set(pluginExtension.kresPackage)
                    task.krName.set(pluginExtension.krName)
                    task.kresResourcesRoot.set(pluginExtension.resourcesRoot)
                    task.kresDisableFileResourceGeneration.set(pluginExtension.disableFileResourceGeneration)
                    //Any new task params go here

                    val generatedDir =
                        project.layout.buildDirectory.file("${pluginExtension.generatedFileDir}/commonMain")
                            .get().asFile
                    task.generatedFileDir.set(generatedDir)

                    // Add the directory to the source set immediately
                    kotlinExtension.sourceSets.getByName("commonMain").kotlin.srcDir(
                        File(
                            generatedDir,
                            "src"
                        )
                    )
                }
                activeTargetTasks.add(generateTask)
                registeredTasks.add(commonMainTaskName)
            }

            val kresClean = project.tasks.register("kresclean", Delete::class.java) {
                it.group = "kres"
                it.description = "Deletes all generated files by KRes."
                it.delete(project.layout.buildDirectory.dir(pluginExtension.generatedFileDir))
            }

            // Create the main 'generatekres' task
            val mainGenerateTask = project.tasks.register("kresgenerate") {
                it.dependsOn(kresClean)
                it.mustRunAfter(kresClean)

                it.dependsOn(activeTargetTasks)
            }

            //Run this every build (clean and replace)
            project.tasks.named("build").configure {
                it.dependsOn("kresgenerate")
            }

            //Ensure the dirs are there before compilation
            project.tasks.withType(KotlinCompile::class.java).configureEach {
                it.doFirst {
                    val generatedDir =
                        project.layout.buildDirectory.file("${pluginExtension.generatedFileDir}/commonMain")
                            .get().asFile
                    generatedDir.mkdirs()

                    kotlinExtension.sourceSets.getByName("commonMain").kotlin.srcDir(
                        File(
                            generatedDir,
                            "src"
                        )
                    )
                }
            }
        }
    }
}

fun generateKRFile(
    project: Project,
    packageName: String,
    name: String = "KR",
    outPath: File,
    resourceRoot: File?,
    disableFileResourceGeneration: Boolean
) {
    val builder = FileSpec.builder(packageName, name.cap())
    val resObj = TypeSpec.objectBuilder(name)

    val stringsGenerator = StringsGenerator(project, builder, resObj, packageName, "en") //TODO EN
    stringsGenerator.generate()

    if (!disableFileResourceGeneration) {
        val filesGenerator =
            FilesGenerator(project, builder, resObj, packageName, resourceRoot)
        filesGenerator.generate()
    }

    builder.addType(resObj.build())

    println("Writing KRES file to: $outPath")
    builder.build().writeTo(outPath)
    println("Wrote KRES file to: $outPath")
}


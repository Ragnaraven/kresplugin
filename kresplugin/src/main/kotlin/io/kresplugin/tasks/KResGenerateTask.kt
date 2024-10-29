package io.kresplugin.tasks

import io.kresplugin.generateKRFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

public abstract class KResGenerateTask : DefaultTask() {
    @Input
    val stringsRoot: Property<String?> = project.objects.property(String::class.java)

    @Input
    val kresPackage: Property<String> = project.objects.property(String::class.java)

    @Input
    val krName: Property<String?> = project.objects.property(String::class.java)

    @Optional
    @InputDirectory
    val kresResourcesRoot: DirectoryProperty = project.objects.directoryProperty()

    @Input
    val kresDisableFileResourceGeneration: Property<Boolean?> =
        project.objects.property(Boolean::class.java)

    @OutputDirectory
    val generatedFileDir: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun generateKrResources() {
        val outputPath = File(generatedFileDir.get().asFile, "src")
        outputPath.mkdirs()

        val resourcesRootPath =
            if (kresResourcesRoot.isPresent) kresResourcesRoot.get().asFile else determineResourcesRoot()

        generateKRFile(
            project,
            kresPackage.get(),
            krName.get() ?: "KR",
            outputPath,
            resourcesRootPath,
            kresDisableFileResourceGeneration.get() ?: false
        )
    }

    fun determineResourcesRoot(): File? {
        val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val commonMainSourceSet = kotlinExtension.sourceSets.getByName("commonMain")
        return commonMainSourceSet.resources.srcDirs.first()
    }
}

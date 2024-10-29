package io.kresplugin

import java.io.File

open class KResPluginExtension {
    // The root directory or path where the input strings are located.
    var stringsRoot: String = "strings"

    // The package name of the generated `KR.kt` file.
    var kresPackage: String? = null

    // The name of the root object in the generated file.
    var krName: String = "KR"

    // The base directory where the generated `KR.kt` file will be placed.
    // The actual directory will be determined dynamically based on the source set in KResPlugin.kt.
    var generatedFileDir: String = "generated/kres"

    var disableFileResourceGeneration: Boolean = false
    var resourcesRoot: File? = null
}
package io.kresplugin

import org.gradle.api.Project
import java.util.Locale

fun loadResourceFile(project: Project, resourcePath: String): String {
    val resourceName = "src/commonMain/resources/$resourcePath.json"
    val resourceFile = project.file(resourceName)

    if (resourceFile.exists()) {
        return resourceFile.readText()
    } else {
        throw IllegalArgumentException("Cannot find resource with name: $resourceName")
    }
}

fun getCurrentLanguageCode(): String {
    return Locale.getDefault().language
}

fun String.cap(): String = capitalizeFirstLetter(this)
fun capitalizeFirstLetter(input: String): String {
    if (input.isEmpty()) return input
    return input[0].uppercase() + input.substring(1)
}

fun String.decap(): String = decapitalizeFirstLetter(this)
fun decapitalizeFirstLetter(input: String): String {
    if (input.isEmpty()) return input
    return input[0].lowercase() + input.substring(1)
}

//Support non-char names? If you are here and want this, please reach out or submit a PR.
fun validateJsonKeyForKotlin(key: String, path: String = "") {
    val fullPath = "$path.$key".removeDot()

    // Check if key is empty
    if (key.isEmpty()) throw IllegalArgumentException("Keys cannot be empty: $path.\"\"")

    // Check if the key starts with an alphabet character
    if (!key[0].isLetter()) throw IllegalArgumentException("Keys must start with an alphabet character: $fullPath")

    // Check if the key conforms to Kotlin's variable naming conventions
    key.forEachIndexed { index, char ->
        if (index == 0 && !char.isLetter()) {
            throw IllegalArgumentException("Key must start with an alphabet character: $fullPath")
        } else if (!char.isLetterOrDigit() && char != '_' && char != '$') {
            throw IllegalArgumentException("Invalid character in key: $char. Key: $fullPath")
        }
    }
}

fun String.removeDot(): String {
    if (this.startsWith('.')) return this.substring(1)

    return this
}
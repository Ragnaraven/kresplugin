package io.kresplugin.generators

import com.squareup.kotlinpoet.*
import io.kresplugin.decap
import io.kresplugin.loadResourceFile
import io.kresplugin.validateJsonKeyForKotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.Project
import java.util.regex.Pattern

class StringsGenerator(
    private val project: Project,
    private val fileSpec: FileSpec.Builder,
    private val resObj: TypeSpec.Builder,
    private val packageName: String,
    private val defaultLang: String,
    private val stringsObjName: String = "strings"
) {
    private val currentLangProperty = PropertySpec.builder("currentLang", String::class)
        .mutable()
        .initializer("%S", defaultLang)
        .build()

    fun generate() {
        val jsonStringsFile = loadResourceFile(project, stringsObjName)
        val jsonObject = Json.parseToJsonElement(jsonStringsFile).jsonObject

        val stringsObject = TypeSpec.objectBuilder(stringsObjName)
        stringsObject.addProperty(currentLangProperty)

        generateStrings(stringsObject, jsonObject, "")
        resObj.addType(stringsObject.build())
    }

    private fun generateStrings(
        currentObject: TypeSpec.Builder,
        jsonObject: JsonObject,
        currentPath: String
    ) {
        jsonObject.entries.forEach { (key, value) ->
            when (value) {
                is JsonObject -> {
                    if (isTranslationObject(value)) {
                        val (functionBuilder, interpolationVariables) = createFunctionWithInterpolation(
                            key,
                            value
                        )
                        currentObject.addFunction(functionBuilder.build())
                    } else {
                        val nestedObject = TypeSpec.objectBuilder(key.decap())
                        val newPath = if (currentPath.isEmpty()) key else "$currentPath.$key"
                        generateStrings(nestedObject, value, newPath)
                        currentObject.addType(nestedObject.build())
                    }
                }

                is JsonPrimitive -> {
                    if (value.isString) {
                        val (functionBuilder, interpolationVariables) = createFunctionWithInterpolation(
                            key,
                            value
                        )
                        currentObject.addFunction(functionBuilder.build())
                    }
                }

                else -> println("Warning: Unsupported JSON type detected at $currentPath.$key")
            }
        }
    }

    private fun isTranslationObject(jsonObject: JsonObject): Boolean {
        return jsonObject.keys.all { it.length == 2 }
    }

    private fun createFunctionWithInterpolation(
        key: String,
        value: JsonElement
    ): Pair<FunSpec.Builder, List<String>> {
        val interpolationPattern = Pattern.compile("\\$\\{?(\\w+(?:\\.\\w+)*)\\}?")
        val matcher = interpolationPattern.matcher(value.toString())

        val interpolationVariables = mutableListOf<String>()
        while (matcher.find()) {
            interpolationVariables.add(matcher.group(1).replace(".", "_"))
        }

        val uniqueVariables = interpolationVariables.distinct()

        val functionBuilder = FunSpec.builder(key.decap())
            .returns(String::class)

        when (value) {
            is JsonObject -> {
                val casesBlock = generateLanguageCases(value, uniqueVariables).build()
                if (casesBlock.toString().contains("$")) {
                    functionBuilder.addCode("return %L", casesBlock)
                } else {
                    functionBuilder.addStatement("return %L", casesBlock)
                }
            }

            is JsonPrimitive -> {
                var content = value.content.replace("\n", "\\n")
                interpolationVariables.forEach { variable ->
                    val variableWithDots = variable.replace("_", ".")
                    content = content.replace("\${$variableWithDots}", "\$$variable")
                    content = content.replace("\$$variableWithDots", "\$$variable")
                }
                if (content.contains("$")) {
                    functionBuilder.addStatement("return %P", content)
                } else {
                    functionBuilder.addStatement("return %S", content)
                }
            }

            is JsonArray -> println("Error: Arrays are not supported yet. Unsupported JSON type detected at $key")
        }

        uniqueVariables.forEach {
            validateJsonKeyForKotlin(it, key)
            functionBuilder.addParameter(it, String::class)
        }

        return Pair(functionBuilder, uniqueVariables)
    }

    private fun generateLanguageCases(
        translations: JsonObject,
        interpolationVariables: List<String>
    ): CodeBlock.Builder {
        val casesBuilder = CodeBlock.builder().beginControlFlow("when (currentLang)")
        translations.forEach { (lang, value) ->
            if (value is JsonPrimitive && value.isString) {
                var content = value.content
                interpolationVariables.forEach { variable ->
                    content =
                        content.replace("\${" + variable.replace("_", ".") + "}", "\$$variable·")
                }
                if (content.contains("$")) {
                    casesBuilder.addStatement("%P·->·%P", lang, content)
                } else {
                    casesBuilder.addStatement("%P·->·%S", lang, content)
                }
            }
        }
        val defaultContent = translations[defaultLang]?.jsonPrimitive?.content?.let {
            var tempContent = it
            interpolationVariables.forEach { variable ->
                tempContent =
                    tempContent.replace("\${" + variable.replace("_", ".") + "}", "\$$variable·")
            }
            tempContent
        } ?: "N/A"
        if (defaultContent.contains("$")) {
            casesBuilder.addStatement("else·->·%P", defaultContent)
        } else {
            casesBuilder.addStatement("else·->·%S", defaultContent)
        }
        return casesBuilder.endControlFlow()
    }
}
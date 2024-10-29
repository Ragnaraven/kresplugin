package io.kresplugin.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.kresplugin.decap
import org.gradle.api.Project
import java.io.File

data class ResourceNode(
    val name: String,
    val path: String,
    val children: MutableList<ResourceNode> = mutableListOf()
)

class FilesGenerator(
    private val project: Project,
    private val fileSpec: FileSpec.Builder,
    private val resObj: TypeSpec.Builder,
    private val packageName: String,
    private val rootDir: File?
) {
    fun generate() {
        if (rootDir == null) return
        val rootTree = buildResourceTree(rootDir) ?: return
        generateResourcesObject(rootTree, resObj)
    }

    private fun generateResourcesObject(node: ResourceNode, currentTypeSpec: TypeSpec.Builder) {
        node.children.forEach { childNode ->
            val sanitizedChildName = sanitizeName(childNode.name)
            if (childNode.children.isNotEmpty()) {
                // If it's a directory, generate an object
                val objectTypeSpec = TypeSpec.objectBuilder(sanitizedChildName)
                generateResourcesObject(childNode, objectTypeSpec)
                currentTypeSpec.addType(objectTypeSpec.build())
            } else {
                // If it's a file, generate a property
                val property = PropertySpec.builder(sanitizedChildName.decap(), String::class)
                    .initializer(
                        "%S",
                        childNode.path.trimStart('/')
                    ) // Removing the preceding slash
                    .build()
                currentTypeSpec.addProperty(property)
            }
        }
    }

    private fun buildResourceTree(root: File, currentPath: String = ""): ResourceNode? {
        if (!root.exists() || !root.isDirectory) return null

        val node = ResourceNode(root.name, currentPath)

        for (child in root.listFiles() ?: emptyArray()) {
            if (child.isDirectory) {
                buildResourceTree(child, "$currentPath/${child.name}")?.let { childNode ->
                    node.children.add(childNode)
                }
            } else {
                node.children.add(
                    ResourceNode(
                        sanitizeName(child.name),
                        "$currentPath/${child.name}"
                    )
                )
            }
        }

        return node
    }

    fun sanitizeName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }
}
package com.itiviti

import java.io.File
import java.nio.file.Paths

@Suppress("UNCHECKED_CAST")
class DotnetProject(private val eval: Map<String, Any>) {
    /**
     * Subset of https://docs.microsoft.com/en-us/visualstudio/ide/build-actions?view=vs-2019
     * For getting all input files
    */
    enum class BuildAction(val value: String) {
        ApplicationDefinition("ApplicationDefinition"),
        Compile("Compile"),
        Content("Content"),
        EmbeddedResource("EmbeddedResource"),
        Page("Page"),
        Resource("Resource"),
        None("None"),
        SplashScreen("SplashScreen"),
        XamlAppDef("XamlAppDef")
    }

    data class PackageReference(val name: String, val version: String?)

    fun getProperty(key: String): String? {
        return getProperties()[key]
    }

    private fun getPropertyThrows(key: String): String {
        return getProperties()[key] ?: error("$key not found in project properties")
    }

    fun getTargetPath(): String {
        return getPropertyThrows("TargetPath")
    }

    fun getProjectFile(): String {
        return getPropertyThrows("MSBuildProjectFullPath")
    }

    fun getProjectName(): String {
        return getPropertyThrows("MSBuildProjectName")
    }

    fun getProjectDirectory(): String {
        return getPropertyThrows("MSBuildProjectDirectory")
    }

    fun getPackageReferences(): List<PackageReference> {
        val list = eval["PackageReference"] as Collection<Map<String, String?>>? ?: return listOf()
        return list.map { PackageReference(it["Include"] as String, it["Version"] ?: "") }.toList()
    }

    private fun getProperties(): Map<String, String> {
        return eval["Properties"] as Map<String, String>? ?: mapOf()
    }

    fun getSources(buildAction: BuildAction): Collection<File> {
        if (!eval.containsKey(buildAction.value)) {
            return listOf()
        }
        return (eval[buildAction.value] as Collection<Any>)
                .map { (it as Map<String, String>) }
                .filter { it.containsKey("Include") }
                .filter { buildAction != BuildAction.None || it["CopyToOutputDirectory"] == "Always" || it["CopyToOutputDirectory"] == "PreserveNewest" }
                .map { resolveFile(it["Include"] as String) }
    }

    fun getOutputPath(): File? {
        return getProjectPropertyPath("OutputPath")
    }

    fun getIntermediateOutputPath(): File? {
        return getProjectPropertyPath("IntermediateOutputPath")
    }

    fun getPackageOutputPath(): File? {
        return getProjectPropertyPath("PackageOutputPath")
    }

    fun getPublishDir(): File? {
        return getProjectPropertyPath("PublishDir")
    }

    fun getInputFiles(): List<File> {
        return listOf(File(getProjectFile())) +
            BuildAction.values().map { getSources(it) }
                .flatten()
    }

    fun getOutputPaths(): List<File> {
        return listOfNotNull(getOutputPath(), getIntermediateOutputPath(), getPackageOutputPath(), getPublishDir())
    }

    fun getProjectPropertyPath(propertyKey: String): File? {
        val property = getProperty(propertyKey) ?: return null
        return resolveFile(property)
    }

    private fun resolveFile(str: String): File {
        val path = Paths.get(str)
        if (path.isAbsolute) {
            return path.toFile()
        }
        return File(getProjectDirectory(), str)
    }
}
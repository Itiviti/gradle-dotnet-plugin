package com.itiviti

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories

open class DotnetBuildTask: DotnetBaseTask("build") {
    init {
        // explicitly restored in evaluation phase
        args("--no-restore")
        val buildExtension = (getPluginExtension() as ExtensionAware).extensions.getByType(DotnetBuildExtension::class.java)
        buildExtension.parameters.forEach {
            args("-p:${it.key}=${it.value}")
        }
    }

    @InputFiles
    fun getSources() {
        getPluginExtension().allProjects.map { it.value.getInputFiles() }.flatten()
    }

    @OutputDirectories
    fun getDestinations() {
        getPluginExtension().allProjects.map { it.value.getOutputPaths() }.flatten()
    }
}
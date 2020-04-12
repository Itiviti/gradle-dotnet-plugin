package com.itiviti

import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import java.io.File

open class DotnetBuildTask: DotnetBaseTask("build") {

    @InputFiles
    val sources: ListProperty<File> = project.objects.listProperty(File::class.java)
            .convention (project.provider { getPluginExtension().allProjects.map { it.value.getInputFiles() }.flatten() })

    @OutputDirectories
    val destinations: ListProperty<File> = project.objects.listProperty(File::class.java)
            .convention (project.provider { getPluginExtension().allProjects.map { it.value.getOutputPaths() }.flatten() })

    init {
        sources.finalizeValueOnRead()
        destinations.finalizeValueOnRead()

        // explicitly restored in evaluation phase
        args("--no-restore")
        val buildExtension = (getPluginExtension() as ExtensionAware).extensions.getByType(DotnetBuildExtension::class.java)
        buildExtension.parameters.forEach {
            args("-p:${it.key}=${it.value}")
        }
    }
}
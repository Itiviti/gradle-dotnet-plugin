package com.itiviti.tasks

import com.itiviti.extensions.DotnetBuildExtension
import com.itiviti.extensions.DotnetRestoreExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.process.ExecSpec
import java.io.File

open class DotnetBuildTask: DotnetBaseTask("build") {

    companion object {
        fun restoreArgs(restoreExtension: DotnetRestoreExtension, buildExtension: DotnetBuildExtension, exec: ExecSpec) {
            if (restoreExtension.force) {
                exec.args("--force")
            }
            if (restoreExtension.noCache) {
                exec.args("--no-cache")
            }
            restoreExtension.source.forEach {
                exec.args("--source", it)
            }
            buildExtension.addCustomBuildProperties(exec)
        }
    }
    @Input
    @Optional
    val version: Property<String> = project.objects.property(String::class.java).convention(getNestedExtension(DotnetBuildExtension::class.java).version)

    @Input
    @Optional
    val packageVersion: Property<String> = project.objects.property(String::class.java).convention(getNestedExtension(DotnetBuildExtension::class.java).packageVersion)

    @InputFiles
    val sources: ListProperty<File> = project.objects.listProperty(File::class.java)
            .convention (project.provider { getPluginExtension().allProjects.map { it.value.getInputFiles() }.flatten() })

    @OutputDirectories
    val destinations: ListProperty<File> = project.objects.listProperty(File::class.java)
            .convention (project.provider { getPluginExtension().allProjects.map { it.value.getOutputPaths() }.flatten() })



    init {
        sources.finalizeValueOnRead()
        destinations.finalizeValueOnRead()

        args("/nodereuse:false")

        val restoreExtension = getNestedExtension(DotnetRestoreExtension::class.java)
        val buildExtension = getNestedExtension(DotnetBuildExtension::class.java)

        // Require restore during build
        if (restoreExtension.beforeBuild) {
            restoreArgs(restoreExtension, buildExtension, this)
        } else {
            args("--no-restore")
        }

        if (!version.orNull.isNullOrBlank()) {
            args("-p:Version=${version.get()}")
        }
        if (!packageVersion.orNull.isNullOrBlank()) {
            args("-p:PackageVersion=${packageVersion.get()}")
        }

        buildExtension.addCustomBuildProperties(this)
    }
}
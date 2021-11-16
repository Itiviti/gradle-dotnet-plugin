package com.itiviti.tasks

import com.itiviti.extensions.DotnetBuildExtension
import com.itiviti.extensions.DotnetRestoreExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
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

        // Require to restore during build
        if (restoreExtension.beforeBuild) {
            restoreArgs(restoreExtension, buildExtension, this)
        } else {
            args("--no-restore")
        }

        if (buildExtension.version.isNotEmpty()) {
            args("-p:Version=${buildExtension.version}")
        }
        if (buildExtension.packageVersion.isNotEmpty()) {
            args("-p:PackageVersion=${buildExtension.packageVersion}")
        }

        buildExtension.addCustomBuildProperties(this)
    }
}
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
        fun restoreArgs(restoreExtension: DotnetRestoreExtension, exec: ExecSpec) {
            if (restoreExtension.configFile != null) {
                exec.args("--configfile \"$restoreExtension.configFile\"")
            }
            if (restoreExtension.force) {
                exec.args("--force")
            }
            if (restoreExtension.noCache) {
                exec.args("--no-cache")
            }
            restoreExtension.source.forEach {
                exec.args("--source", it)
            }
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

        val restoreExtension = getNestedExtension(DotnetRestoreExtension::class.java)
        // Require to restore during build
        if (restoreExtension.beforeBuild) {
            restoreArgs(restoreExtension, this)
        } else {
            args("--no-restore")
        }

        val buildExtension = getNestedExtension(DotnetBuildExtension::class.java)
        if (buildExtension.version.isNotEmpty()) {
            args("-p:Version=${buildExtension.version}")
        }
        if (buildExtension.packageVersion.isNotEmpty()) {
            args("-p:PackageVersion=${buildExtension.packageVersion}")
        }

        buildExtension.getProperties().forEach {
            args("-p:${it.key}=${it.value}")
        }
    }
}

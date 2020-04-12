package com.itiviti

import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Exec

open class DotnetBaseTask(action: String) : Exec() {

    init {
        logging.captureStandardOutput(LogLevel.INFO)
        logging.captureStandardError(LogLevel.ERROR)

        val pluginExtension = getPluginExtension()
        workingDir = pluginExtension.workingDir

        setup(pluginExtension, action)
    }

    protected fun getPluginExtension(): DotnetPluginExtension {
        return project.extensions.getByType(DotnetPluginExtension::class.java)
    }

    private fun setup(pluginExtension: DotnetPluginExtension, action: String) {
        commandLine(pluginExtension.dotnetExecutable)
        args(action)

        if (!pluginExtension.solution.isNullOrEmpty()) {
            args(pluginExtension.solution)
        }

        args("--verbosity", pluginExtension.verbosity)
        args("--configuration", pluginExtension.configuration)
        if (!pluginExtension.framework.isNullOrBlank()) {
            args("--framework", pluginExtension.framework)
        }
    }
}
package com.itiviti.tasks

import com.itiviti.extensions.DotnetPluginExtension
import org.gradle.api.logging.LogLevel

open class DotnetBaseTask(action: String) : BaseExecTask(action) {
    init {
        logging.captureStandardOutput(LogLevel.INFO)
        logging.captureStandardError(LogLevel.ERROR)

        val pluginExtension = getPluginExtension()
        workingDir = pluginExtension.workingDir

        setup(pluginExtension)
    }

    private fun setup(pluginExtension: DotnetPluginExtension) {
        if (!pluginExtension.solution.isNullOrEmpty()) {
            args(pluginExtension.solution)
        }

        args("--verbosity", pluginExtension.verbosity)
        args("--configuration", pluginExtension.configuration)
        if (!pluginExtension.framework.isNullOrBlank()) {
            args("--framework", pluginExtension.framework)
        }
        if (!pluginExtension.runtime.isNullOrBlank()) {
            args("--runtime", pluginExtension.runtime)
        }
    }
}
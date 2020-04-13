package com.itiviti.tasks

import com.itiviti.extensions.DotnetPluginExtension
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Internal

open class BaseExecTask(vararg actions: String): Exec() {
    init {
        logging.captureStandardOutput(LogLevel.INFO)
        logging.captureStandardError(LogLevel.ERROR)

        val pluginExtension = getPluginExtension()
        workingDir = pluginExtension.workingDir

        commandLine(pluginExtension.dotnetExecutable)
        actions.forEach {
            args(it)
        }
    }

    @Internal
    protected fun getPluginExtension(): DotnetPluginExtension {
        return project.extensions.getByType(DotnetPluginExtension::class.java)
    }

    @Internal
    protected fun <T> getNestedExtension(type: Class<T>): T {
        return (getPluginExtension() as ExtensionAware).extensions.getByType(type)
    }
}
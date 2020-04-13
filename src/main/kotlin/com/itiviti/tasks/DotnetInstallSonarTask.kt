package com.itiviti.tasks

import com.itiviti.extensions.DotnetSonarExtension
import org.gradle.api.tasks.Internal

open class DotnetInstallSonarTask: BaseExecTask("tool", "install", "--global", "dotnet-sonarscanner") {
    init {
        isIgnoreExitValue = true

        val version = getNestedExtension(DotnetSonarExtension::class.java).version
        if (version != null) {
            args("--version", version)
        }
    }

    @Internal
    fun isInstalled(): Boolean {
        return execResult != null && execResult!!.exitValue >= 0
    }
}
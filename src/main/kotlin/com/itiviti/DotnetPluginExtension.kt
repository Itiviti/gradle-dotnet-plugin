package com.itiviti

import org.gradle.api.GradleException
import java.io.File

open class DotnetPluginExtension(var projectName: String) {

    /**
     * Support quiet, minimal, normal, detailed, and diagnostic
     */
    var verbosity: String = "Normal"

    /**
     * Location of dotnet executable, default to use the one setup in PATH
     */
    var dotnetExecutable: String = "dotnet"

    /**
     * Working dir for executing commands, default to project dir
     */
    var workingDir: File = File(".")

    /**
     * project file, either .sln or .csproj, default to the project found in the [workingDir]
     */
    var solution: String? = null

    /**
     * build configuration, default as Release
     */
    var configuration: String = "Release"

    /**
     * target platform, default defined as in solution
     */
    var platform: String? = null

    /**
     * target framework, the framework must be defined in the project file, default defined as in solution
     */
    var framework: String? = null

    lateinit var allProjects: Map<String, DotnetProject>

    fun getMainProject(): DotnetProject {
        return allProjects[projectName] ?: throw GradleException("$projectName not found in parsed projects")
    }
}
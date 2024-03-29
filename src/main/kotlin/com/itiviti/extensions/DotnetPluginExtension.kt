package com.itiviti.extensions

import com.itiviti.DotnetProject
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

open class DotnetPluginExtension(var projectName: String, projectDir: File, projectEvaluate: () -> Map<String, DotnetProject>) {

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
    var workingDir: File = projectDir

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

    /**
     * target runtime
     */
    var runtime: String? = null

    /**
     * If set to true, the build will fail if pre-release reference is detected, for use when building release build
     */
    var preReleaseCheck = false

    /**
     * environment variable MSBuildSDKsPath passed when executing dotnet build for selecting SDKs
     */
    var msbuildSDKsPath: String? = null

    val allProjects: Map<String, DotnetProject> by lazy { projectEvaluate() }

    fun getMainProject(): DotnetProject {
        return allProjects[projectName] ?: throw GradleException("$projectName not found in parsed projects")
    }
}
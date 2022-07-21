/*************************************************************************
 * ULLINK CONFIDENTIAL INFORMATION
 * _______________________________
 *
 * All Rights Reserved.
 *
 * NOTICE: This file and its content are the property of Ullink. The
 * information included has been classified as Confidential and may
 * not be copied, modified, distributed, or otherwise disseminated, in
 * whole or part, without the express written permission of Ullink.
 ************************************************************************/
package com.itiviti

import com.itiviti.extensions.DotnetPluginExtension
import com.itiviti.extensions.DotnetRestoreExtension
import org.gradle.api.GradleException
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.TaskExecuter
import org.gradle.api.internal.tasks.TaskStateInternal
import org.gradle.api.internal.tasks.execution.DefaultTaskExecutionContext
import org.gradle.api.provider.Provider
import org.gradle.execution.ProjectExecutionServices
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

class DotnetPluginSpec extends Specification {

    @Unroll
    def "Project is restored and parsed correctly with beforeBuild = #beforeBuild with publishing = #publishing"() {
        setup:
        def project = ProjectBuilder.builder()
                .build()
        project.plugins.apply('com.itiviti.dotnet')
        if (publishing) {
            project.plugins.apply('publishing')
        }

        def pluginExtension = project.extensions.getByType(DotnetPluginExtension)
        pluginExtension.projectName = 'core'
        pluginExtension.workingDir = new File(this.class.getResource('project').toURI())
        def restoreExtension = pluginExtension.extensions.getByType(DotnetRestoreExtension)
        restoreExtension.beforeBuild = beforeBuild

        expect:
        pluginExtension.allProjects.size() == 2

        pluginExtension.mainProject.projectName == 'core'
        pluginExtension.mainProject.targetPath != null
        pluginExtension.mainProject.projectDirectory != null
        pluginExtension.mainProject.projectFile != null
        pluginExtension.mainProject.packageReferences.size() == 3
        pluginExtension.mainProject.outputPaths.size() == 4
        pluginExtension.mainProject.inputFiles.size() == 12
        [ DotnetProject.BuildAction.Content,
          DotnetProject.BuildAction.None,
          DotnetProject.BuildAction.EmbeddedResource,
          DotnetProject.BuildAction.Page,
          DotnetProject.BuildAction.Compile,
          DotnetProject.BuildAction.Resource].each {
            assert pluginExtension.mainProject.getSources(it).size() == 1
        }
        pluginExtension.mainProject.getSources(DotnetProject.BuildAction.ApplicationDefinition).size() == 3
        project.tasks.clean.dependsOn.findAll { it instanceof Provider && it.get().name == 'dotnetClean' }.size() > 0
        project.tasks.assemble.dependsOn.findAll { it instanceof Provider && it.get().name == 'dotnetBuild' }.size() > 0
        project.tasks.build.dependsOn.findAll { it instanceof Provider && it.get().name == 'dotnetTest' }.size() > 0
        if (publishing) {
            project.tasks.publish.dependsOn.findAll { it instanceof Provider && it.get().name == 'dotnetNugetPush' }.size() > 0
        }

        where:
        [beforeBuild, publishing] << [
                [ true, false ],
                [ true, false ]
        ].combinations()
    }

    def "Project evaluation fails when preReleaseCheck is set"() {
        setup:
        def project = ProjectBuilder.builder()
                .build()
        project.plugins.apply('com.itiviti.dotnet')

        def pluginExtension = project.extensions.getByType(DotnetPluginExtension)
        pluginExtension.workingDir = new File(this.class.getResource('project').toURI())
        pluginExtension.preReleaseCheck = true

        def restoreExtension = pluginExtension.extensions.getByType(DotnetRestoreExtension)
        restoreExtension.beforeBuild = true

        when:
        project.evaluate()

        then:
        thrown(GradleException)
    }

    def ".net sdk project can be built"() {
        setup:
        def project = ProjectBuilder.builder()
                .build()
        project.version = '9.14.0'
        project.plugins.apply('com.itiviti.dotnet')

        def pluginExtension = project.extensions.getByType(DotnetPluginExtension)
        pluginExtension.workingDir = new File(this.class.getResource('project').toURI())

        def buildTask = project.tasks.getByName("build")
        def executionServices = new ProjectExecutionServices(project as ProjectInternal)

        when:
        project.evaluate()
        project.gradle.taskGraph.addEntryTasks([buildTask])
        project.gradle.taskGraph.populate()

        and:
        executionServices.get(TaskExecuter).execute(buildTask as TaskInternal, buildTask.state as TaskStateInternal, new DefaultTaskExecutionContext(null))

        then:
        buildTask.state.failure == null
    }

    def "MSBuildSDKsPath version mismatch is fixed automatically"() {
        setup:
        def project = ProjectBuilder.builder()
                .build()
        project.version = '9.14.0'
        project.plugins.apply('com.itiviti.dotnet')
        def extension = project.extensions.getByType(DotnetPluginExtension)
        def envPaths = envVersion.replace(']', '').split(' \\[').reverse()
        def path = Paths.get(envPaths[0], envPaths[1], 'Sdks').toString()
        def currentVersion = DotnetPlugin.getDotnetVersion(project, extension)

        when:
        DotnetPlugin.fixEnv(project, path, extension, currentVersion)

        then:
        if (envVersion.startsWith(envVersion)) {
            extension.msbuildSDKsPath == null
        } else {
            extension.msbuildSDKsPath.contains(currentVersion)
        }

        where:
        envVersion << listSdks().readLines()
    }

    static def listSdks() {
        Process command = new ProcessBuilder("dotnet", "--list-sdks").start()
        return command.text
    }
}

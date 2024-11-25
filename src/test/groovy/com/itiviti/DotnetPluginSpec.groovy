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
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class DotnetPluginSpec extends Specification {

    @TempDir
    File testProjectDir

    def "Project is restored and parsed correctly"() {
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
        pluginExtension.mainProject.getSources(DotnetProject.BuildAction.Content).size() == 3
        [ DotnetProject.BuildAction.None,
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
        def properties = new File(testProjectDir, 'gradle.properties')
        properties << 'version = 11.12'
        def buildFile = new File(testProjectDir, 'build.gradle')
        buildFile << """
            plugins {
                id 'com.itiviti.dotnet'
            }

            dotnet {
                workingDir = file('${new File(this.class.getResource('project').toURI()).toString().replace("\\", "\\\\")}')
            }
        """.stripIndent()

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('build', '--info')
            .withPluginClasspath()
            .build()

        then:
        result.task(':build').outcome == TaskOutcome.SUCCESS
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

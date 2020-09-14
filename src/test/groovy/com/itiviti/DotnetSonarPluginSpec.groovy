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

import org.gradle.api.tasks.Exec
import org.gradle.testfixtures.ProjectBuilder
import org.sonarqube.gradle.SonarQubeExtension
import spock.lang.Specification

class DotnetSonarPluginSpec extends Specification {
    def 'Applying dotnet sonar setups tasks'() {
        setup:
        def project = ProjectBuilder.builder()
                .build()

        when:
        project.plugins.apply('com.itiviti.dotnet')
        project.plugins.apply('com.itiviti.dotnet-sonar')

        then:
        project.tasks.getByName('sonarqube') != null
        project.tasks.getByName('dotnetInstallSonar') != null
    }

    def 'dotnet-sonarscanner could be executed after install'() {
        setup:
        def project = ProjectBuilder.builder()
                .build()

        when:
        project.plugins.apply('com.itiviti.dotnet')
        project.plugins.apply('com.itiviti.dotnet-sonar')

        and: 'Install sonar'
        (project.tasks.getByName('dotnetInstallSonar') as Exec).exec()

        and:
        def sonarTask = project.tasks.getByName('sonarqube') as Exec
        sonarTask.ignoreExitValue = true
        sonarTask.exec()

        then: 'dotnet-sonarscanner can be started'
        noExceptionThrown()
        sonarTask.execResult.exitValue > 0
    }

    def 'sonar.login is passed in dotnet-sonarscanner end'() {
        setup:
        def project = ProjectBuilder.builder()
                .build()

        when:
        project.plugins.apply('com.itiviti.dotnet')
        project.plugins.apply('com.itiviti.dotnet-sonar')

        ((SonarQubeExtension)project.extensions.getByName(SonarQubeExtension.SONARQUBE_EXTENSION_NAME)).properties {
            property 'sonar.login', 'foo'
        }

        and: 'doFirst in dotnetBuild'
        def dotnetBuild = project.tasks.getByName('dotnetBuild') as Exec
        dotnetBuild.actions.first().execute(dotnetBuild)

        then:
        def sonarTask = project.tasks.getByName('sonarqube') as Exec
        sonarTask.args == [ 'end', '/d:sonar.login=foo' ]
    }
}

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

import org.gradle.testfixtures.ProjectBuilder
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
}

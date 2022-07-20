Will tests pass?
# Gradle Dotnet Plugin [![Build Status](https://dev.azure.com/ngyukman/ngyukman/_apis/build/status/Itiviti.gradle-dotnet-plugin?branchName=master)](https://dev.azure.com/ngyukman/ngyukman/_build/latest?definitionId=1&branchName=master) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/Itiviti/gradle-dotnet-plugin) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This plugin allows executing dotnet cli commands for building dotnet projects.
It supports only project with sdk format.

Supported tasks are
* dotnetClean
* dotnetBuild
* dotnetTest
* dotnetNugetPush
* dotnetInstallSonar
* sonarqube

It also supports project file parsing, and some basic up-to-date checks to skip the build.
You can access the properties via `project.dotnet.allProjects` and `project.dotnet.mainProject`.
dotnet restore will be done at project evaluation phase to make sure all project properties can be retrieved.

Plugin applies the `LifecycleBasePlugin` automatically,
and hooks tasks to standard lifecycle, such as `clean`, `assemble` and `build`.
if `publising` plugin is applied, `dotnetNugetPush` task will hook on `publish` 

## Prerequisites
* .Net Core SDK 3.1 / .Net 5.0 / .Net 6.0
* Gradle 6.1

## To Apply
```groovy
plugins {
    id 'com.itiviti.dotnet'
}
```

or
```groovy
buildscript {
    repositories {
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }

    dependencies {
        classpath 'com.itiviti.gradle:gradle-dotnet-plugin:1.6.1'
    }
}
```

## Configuration
For finding all available options and explanations, please refer to src/main/kotlin/com/itiviti/extensions/

```groovy
dotnet {
    // where dotnet sdk is installed, default is to use the one specified in PATH
    dotnetExecutable = 'dotnet'

    // the workingDir of the dotnet cli, default is the current directory
    workingDir = '.'

    // the solution / csproj to be built, it will search the workingDir if not specified
    solution = 'my-solution.sln'

    // configuration to be used, default is Release
    configuration = 'Release'

    // Log verbosity of dotnet command
    verbosity = 'Normal'

    // Stop builds if pre-release is detected, can be used when building release build, default is false
    preReleaseCheck = false

    restore {
        // Specifies to not cache packages and HTTP requests.
        noCache = false

        // Forces all dependencies to be resolved even if the last restore was successful. Specifying this flag is the same as deleting the project.assets.json file.
        force = false

        // Delay dotnet restore until dotnetBuild rather than in evaluation phase, could lead to missing project properties due to missing dependencies
        beforeBuild = false
    }

    build {
        // Any build parameter to be passed to msbuild, as /p:key=value, for example
        maxCpuCount = ''

        // Default values applied
        version = project.version
        packageVersion = project.version
    }

    test {
        // filter test to be executed, or use command arguments --dotnet-tests to override (similar to --tests)
        filter = ''

        // test run settings file, no default value
        settings = file(".runsettings")

        // collect code coverage via coverlet, default is true
        collectCoverage = true

        // coverlet output formats, default is opencover
        coverletOutputFormat = 'opencover'

        // [sonar aware] coverlet output path, it must be a directory, default is build/reports/coverlet/
        coverletOutput = file('build/reports/coverlet/')

        nunit {
            // [sonar aware] nunit output path, default is build/reports/nunit/
            testOutputXml = file('build/reports/nunit/')
        }
    }

    nugetPush {
        // The API key for the server.
        apiKey = ''
        // Nuget feed url, default is DefaultPushSource in Nuget config if not set
        source = ''
    }

    sonarqube {
        // version of dotnet-sonarscanner to be installed as global dotnet tool, default is latest
        version = '3.7.1'
    }
}
```

## Sonarqube

Follows the same configuration syntax as the [Sonar Scanner for Gradle plugin](https://github.com/SonarSource/sonar-scanner-gradle).

### Example usage

```groovy
plugins {
    id 'com.itiviti.dotnet-sonar'
}

sonarqube {
    properties {
        property 'sonar.projectKey', 'my.project.key'
        property 'sonar.language', 'cs'
        property 'sonar.host.url', 'https://my-sonar'
    }
}
```

Note:
* when `sonar.projectKey` is not set, it will use the project name as project key.
* `sonar.cs.nunit.reportsPaths` and `sonar.cs.opencover.reportsPaths` are set automatically base on dotnet configuration

## Troubleshoot
#### dotnet pack
It is not supported, recommended adding `<GeneratePackageOnBuild>true</GeneratePackageOnBuild>` to your csproj

#### dotnet test
To run testing successfully, make sure you have the following packages referenced
* Microsoft.NET.Test.Sdk
* NUnit3TestAdapter (or other relevant adapter)

To have coverage report from coverlet, make sure
* DebugType is set to `Portable`
* Add package reference `coverlet.msbuild` or `coverlet.collector` (.net core only, for details please refer to [coverlet](https://github.com/tonerdo/coverlet))

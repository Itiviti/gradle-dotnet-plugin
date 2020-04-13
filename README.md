# Gradle Dotnet Plugin [![Build Status](https://dev.azure.com/ngyukman/ngyukman/_apis/build/status/Itiviti.gradle-dotnet-plugin?branchName=master)](https://dev.azure.com/ngyukman/ngyukman/_build/latest?definitionId=1&branchName=master)

This plugin allows executing dotnet cli commands for building dotnet projects.

Supported tasks are
* dotnetClean
* dotnetBuild
* dotnetTest 

It also supports project file parsing, and some basic up-to-date checks to skip the build.
You can access the properties via `project.dotnet.allProjects` and `project.dotnet.mainProject`.
dotnet restore will be done at project evaluation phase to make sure all project properties can be retrieved.

Plugin applies the base plugin automatically, and hooks tasks to standard build tasks, like `clean`, `assemble` and `test`.

## Prerequisites
* .Net Core SDK 3.0
* Gradle 6.1

## Configuration
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

    restore {
        // Specifies to not cache packages and HTTP requests.
        noCache = false

        // Forces all dependencies to be resolved even if the last restore was successful. Specifying this flag is the same as deleting the project.assets.json file.
        force = false
    }

    build {
        // Any build parameter to be passed to msbuild, as /p:key=value, for example
        maxCpuCount = ""
        
        // Default values applied
        version = project.version
        packageVersion = project.version
    }
       
    nugetPush {
        // The API key for the server.
        apiKey = ""
        // Nuget feed url, default is DefaultPushSource in Nuget config if not set
        source = ""
    }
}
```
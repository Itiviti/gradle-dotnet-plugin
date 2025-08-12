package com.itiviti.tasks

import com.itiviti.extensions.DotnetNUnitExtension
import com.itiviti.extensions.DotnetTestExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class DotnetTestTask: DotnetBaseTask("test") {

    @get:Input
    @get:Optional
    @get:Option(option = "dotnet-tests", description = "Override filter options")
    abstract val filter: Property<String>


    init {
        // built with DotnetBuildTask
        args("--no-build")

        val testExtension = getNestedExtension(DotnetTestExtension::class.java)
        if (project.properties["ignoreFailures"] == "true" || testExtension.ignoreExitValue) {
            isIgnoreExitValue = true
        }

        val escapeExcludeQuote = if (System.getProperty("os.name").contains("windows", true)) "\\\"" else "\""

        if (testExtension.settings?.exists() == true) {
            args("--settings", testExtension.settings!!.absolutePath)
        }
        if (testExtension.collectCoverage) {
            args("/p:CollectCoverage=true")
            args("/p:ExcludeByFile=${escapeExcludeQuote}${testExtension.coverletExcludeFiles}${escapeExcludeQuote}", "/p:CoverletOutputFormat=${testExtension.coverletOutputFormat}", "/p:CoverletOutput=${testExtension.coverletOutput.absolutePath}/")
        }

        val testExtensionAware = (testExtension as ExtensionAware)

        args("--")

        // nunit
        val nunitExtension = testExtensionAware.extensions.getByType(DotnetNUnitExtension::class.java)
        args("NUnit.TestOutputXml=${nunitExtension.testOutputXml.absolutePath}")
        if (nunitExtension.numberOfTestWorkers >= 0) {
            args("NUnit.NumberOfTestWorkers=${nunitExtension.numberOfTestWorkers}")
        }

        if (nunitExtension.where.isNotBlank()) {
            args("NUnit.Where=${nunitExtension.where}")
        }

        if (nunitExtension.stopOnError) {
            args("NUnit.StopOnError=true")
        }
    }

    @TaskAction
    override fun exec() {
        if (!filter.orNull.isNullOrBlank()){
            args = (listOf(args!![0], "--filter", filter.get())) + args!!.drop(1)
        }

        super.exec()
    }
}
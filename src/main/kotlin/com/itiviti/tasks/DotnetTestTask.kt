package com.itiviti.tasks

import com.itiviti.extensions.DotnetNUnitExtension
import com.itiviti.extensions.DotnetTestExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class DotnetTestTask: DotnetBaseTask("test") {
    private var filter: String? = null

    @Option(option = "dotnet-tests", description = "Override filter options")
    fun setFilter(filter: String?) {
        this.filter = filter
    }

    init {
        if (project.properties["ignoreFailures"] == "true") {
            isIgnoreExitValue = true
        }

        // built with DotnetBuildTask
        args("--no-build")

        val escapeQuote = if (System.getProperty("os.name").contains("windows", true)) "\\\"" else "\""

        val testExtension = getNestedExtension(DotnetTestExtension::class.java)
        if (testExtension.settings?.exists() == true) {
            args("--settings", testExtension.settings!!.absolutePath)
        }
        if (testExtension.collectCoverage) {
            args("/p:CollectCoverage=true")
            args("/p:ExcludeByFile=${escapeQuote}${testExtension.coverletExcludeFiles}${escapeQuote}", "/p:CoverletOutputFormat=${testExtension.coverletOutputFormat}", "/p:CoverletOutput=${escapeQuote}${testExtension.coverletOutput.absolutePath}/${escapeQuote}")
        }
        if (testExtension.ignoreExitValue) {
            setIgnoreExitValue(true)
        }

        val testExtensionAware = (testExtension as ExtensionAware)

        args("--")

        // nunit
        val nunitExtension = testExtensionAware.extensions.getByType(DotnetNUnitExtension::class.java)
        args("NUnit.TestOutputXml=\"${nunitExtension.testOutputXml.absolutePath}\"")
        if (nunitExtension.numberOfTestWorkers >= 0) {
            args("NUnit.NumberOfTestWorkers=${nunitExtension.numberOfTestWorkers}")
        }

        if (nunitExtension.where.isNotBlank()) {
            args("NUnit.Where=\"${nunitExtension.where}\"")
        }

        if (nunitExtension.stopOnError) {
            args("NUnit.StopOnError=true")
        }
    }

    @TaskAction
    override fun exec() {
        // Override filter when set
        val testExtension = (getPluginExtension() as ExtensionAware).extensions.getByType(DotnetTestExtension::class.java)
        if (!filter.isNullOrBlank()) {
            args = (listOf(args!![0], "--filter", filter)) + args!!.drop(1)
        }
        else if (!testExtension.filter.isNullOrBlank()) {
            args = (listOf(args!![0], "--filter", testExtension.filter)) + args!!.drop(1)
        }

        super.exec()
    }
}
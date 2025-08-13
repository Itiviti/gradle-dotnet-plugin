package com.itiviti.extensions

import groovy.lang.GroovyObjectSupport
import groovy.lang.MissingPropertyException
import groovy.lang.ReadOnlyPropertyException
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Property
import org.gradle.process.ExecSpec

abstract class DotnetBuildExtension : GroovyObjectSupport() {
    private val storage = mutableMapOf<String, Any?>()

    abstract val version : Property<String>;
    abstract val packageVersion: Property<String>;

    operator fun get(property: String): String? {
        return storage[property]?.toString()
    }

    operator fun set(property: String, value: Any?) {
        storage[property] = value
    }

    fun getProperties(): Map<String, String> {
        return storage.filter { it.value != null }.map { it.key to it.value.toString() }.toMap()
    }

    override fun getProperty(name: String): Any? {
        return when {
            name == "properties" -> {
                getProperties()
            }
            storage.containsKey(name) -> {
                storage[name]
            }
            else -> {
                throw MissingPropertyException(ExtraPropertiesExtension.UnknownPropertyException.createMessage(name), name, null as Class<*>?)
            }
        }
    }

    override fun setProperty(name: String, newValue: Any?) {
        if (name == "properties") {
            throw ReadOnlyPropertyException(name, DotnetBuildExtension::class.java)
        }

        this[name] = newValue
    }

    open fun propertyMissing(name: String): Any? {
        return this[name]
    }

    open fun propertyMissing(name: String, value: Any?) {
        this[name] = value
    }

    fun addCustomBuildProperties(exec: ExecSpec) {
        getProperties().forEach {
            exec.args("-p:${it.key}=${it.value}")
        }
    }
}
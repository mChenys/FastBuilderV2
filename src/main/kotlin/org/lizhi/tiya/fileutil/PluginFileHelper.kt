package org.lizhi.tiya.fileutil

import com.google.gson.Gson
import org.gradle.api.Project
import java.io.File

object PluginFileHelper {

    private var isInit = false

    fun initHelper(project: Project) {
        if (isInit) {
            return
        }
        isInit = true

        val obtainConfigDir = obtainConfigDir(project)
        if (!obtainConfigDir.exists()) {
            obtainConfigDir.mkdirs()
        }

        val obtainConfigFile = obtainConfigFile(project)
        if (!obtainConfigFile.exists()) {
            obtainConfigFile.createNewFile()
        }

        val cacheConfigFile = obtainCacheFile(project)
        if (!cacheConfigFile.exists()) {
            cacheConfigFile.createNewFile()
        }

    }

    fun obtainConfigDir(project: Project): File {
        return project.rootProject.file("./.gradle/.fasterBuilderV2")
    }

    fun obtainConfigFile(project: Project): File {
        return File(obtainConfigDir(project), "jsonConfig")
    }

    fun obtainCacheFile(project: Project): File {
        return File(obtainConfigDir(project), "cacheResult.properties")
    }

    fun readConfig(applyProject: Project): ConfigBean {
        val obtainConfigFile = obtainConfigFile(applyProject)
        val configTxt = obtainConfigFile.readText(Charsets.UTF_8);
        val gson = Gson()
        return gson.fromJson<ConfigBean>(configTxt, ConfigBean::class.java) ?: ConfigBean(emptyList())

    }
}
/*
 * Copyright (C) 2021 Tiya.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lizhi.tiya.config

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.impldep.aQute.bnd.annotation.component.Modified
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask
import org.lizhi.tiya.fileutil.PluginFileHelper
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.AppHelper
import org.lizhi.tiya.plugin.IPluginContext
import org.lizhi.tiya.project.ModuleProject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * 配置文件
 */
class PropertyFileConfig(private val pluginContext: IPluginContext) {

    private val props = Properties()

    var isInit = false


    private fun getPropertyInfo(): Properties {
        if (!isInit) {
            props.load(FileReader(getCacheFile()))
        }
        isInit = true
        return props
    }

    /**
     * 缓存是否有效
     */
    fun isCacheValid(project: ModuleProject): Boolean {
        val propertyInfo = getPropertyInfo()
        val currentModify = project.obtainLastModified()
        val lastModify = propertyInfo.getProperty(project.obtainKeyName())
        if (currentModify.toString() == lastModify) {
            FastBuilderLogger.logLifecycle("${project.project.name} found cache .")
            return true
        }
        FastBuilderLogger.logLifecycle("${project.project.name}  cache invalid.")
        return false
    }

    /**
     * 保存缓存
     */
    fun updateModify(project: ModuleProject) {
        val propertyInfo = getPropertyInfo()
        val curAARProLastModified = project.obtainLastModified()
        propertyInfo.setProperty(project.obtainKeyName(), curAARProLastModified.toString())
    }

    /**
     * 保存配置
     */
    fun saveConfig() {
        // 先保存app的缓存
        saveAppLastModified()
        val propertyInfo = getPropertyInfo()

        propertyInfo.store(
            FileWriter(getCacheFile()),
            "用于存储缓存模块映射关系"
        )
    }

    fun getCacheFile(): File {
        return PluginFileHelper.obtainCacheFile(pluginContext.getApplyProject())
    }

    /**
     * 通过配置初始化
     */
    fun prepareByConfig(): List<ModuleProject> {

        val configBean = PluginFileHelper.readConfig(pluginContext.getApplyProject())

        val moduleProjectList = mutableListOf<ModuleProject>()

        for (curProject in pluginContext.getApplyProject().rootProject.allprojects) {

            if (curProject == pluginContext.getApplyProject().rootProject) {
                continue
            }
            if (curProject == pluginContext.getApplyProject()) {
                continue
            }

            if (!configBean.exclude.contains(curProject.path)) {
                moduleProjectList.add(ModuleProject(curProject, pluginContext))
            }
        }


        // 读取工程配置的子模块配置,并转成对应的模块工程对象

        val countDownLatch = CountDownLatch(moduleProjectList.size)
//
        var hasErr = false
        // 开启多线程处理
        for (moduleProject in moduleProjectList) {
            thread {
                try {
                    moduleProject.cacheValid = isCacheValid(moduleProject)
                } catch (e: Exception) {
                    hasErr = true
                    e.printStackTrace()
                } finally {
                    countDownLatch.countDown()
                }
            }
        }
//
        countDownLatch.await()
        if (hasErr) {
            throw GradleException("FastBuilderPlugin插件在初始化模块进程出错，请确保配置的模块存在")
        }

        return moduleProjectList
    }


    var appLastModified = 0L

    fun saveAppLastModified() {
        val propertyInfo = getPropertyInfo()
        propertyInfo[pluginContext.getApplyProject().name.replace(":", "")] = "$appLastModified"
    }

    fun obtainAppLastModifiedFromConfig(): Long {
        val propertyInfo = getPropertyInfo()
        val name = pluginContext.getApplyProject().name.replace(":", "")
        return propertyInfo.getProperty(name, "0").toLong()
    }

    /**
     * app的缓存是否有效
     */
    fun isAppCacheValid(): Boolean {
        if (appLastModified == 0L) {
            appLastModified = AppHelper.obtainLastModified(pluginContext.getApplyProject())
        }

        return obtainAppLastModifiedFromConfig() == appLastModified

    }


}
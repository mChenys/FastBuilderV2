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

package org.lizhi.tiya.plugin

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.joor.Reflect
import org.lizhi.tiya.config.PropertyFileConfig
import org.lizhi.tiya.extension.ProjectExtension
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.project.ModuleProject
import org.lizhi.tiya.proxy.AppKCompileTaskProxy
import org.lizhi.tiya.proxy.AppKaptTaskProxy
import org.lizhi.tiya.proxy.ModuleKCompilerTaskProxy
import org.lizhi.tiya.proxy.ModuleKaptTaskProxy


/**
 * 插件入口
 */
class FastBuilderPlugin : Plugin<Project>, IPluginContext {

    // apply的工程
    private lateinit var project: Project

    // 工程的配置项
    private lateinit var projectExtension: ProjectExtension

    // 配置文件
    private lateinit var propertyFileConfig: PropertyFileConfig

    // module工程集合
    private var moduleProjectList: List<ModuleProject> = emptyList()

    override fun apply(project: Project) {
        this.project = project

        // 注册Project的配置项
        this.projectExtension = project.extensions.create<ProjectExtension>(
            "moduleArchive",
            ProjectExtension::class.java,
            project
        )
        // 初始化配置文件
        this.propertyFileConfig = PropertyFileConfig(this)

        // 全局配置完成后执行
        project.afterEvaluate {
            if (!projectExtension.pluginEnable) {
                return@afterEvaluate
            }
            val starTime = System.currentTimeMillis();
            //赋值日志是否启用
            FastBuilderLogger.enableLogging = projectExtension.logEnable

            if (currentTaskIsCompile()) {
                return@afterEvaluate
            }
            // 处理app编译相关的task hack
            handleHackAppTask(project)
            // 跳过app的kapt相关的task（仅在缓存有效时跳过）
            handleSkipAppKaptTask(project)
            // 处理其他模块编译相关的task hack
            handleOtherModuleCompile(project)
            // 跳过其他模块的task（仅在缓存有效时跳过）
            handleSkipOtherModuleTask()
            // 保存配置
            handleConfigSave(project)
            val endTime = System.currentTimeMillis();
            FastBuilderLogger.logLifecycle("插件花費的配置時間${endTime - starTime}")
        }
    }

    /**
     * app没有变动时可以跳过kapt相关的task执行
     */
    private fun handleSkipAppKaptTask(project: Project) {
        if (this.getPropertyConfig().isAppCacheValid()) {
            FastBuilderLogger.logLifecycle("app 缓存有效")
            project.tasks.withType(KaptGenerateStubsTask::class.java).all { task ->
                task.onlyIf { false }
            }
            project.tasks.withType(KaptWithKotlincTask::class.java).all { task ->
                task.onlyIf { false }
            }
            project.tasks.withType(AbstractKotlinCompile::class.java).all { task ->
                task.onlyIf { false }
            }
        } else {
            FastBuilderLogger.logLifecycle("app 缓存无效")
        }
    }

    /**
     * 处理其他模块未改动时智能跳过任务
     */
    private fun handleSkipOtherModuleTask() {
        moduleProjectList = propertyFileConfig.prepareByConfig()
        for (moduleProject in moduleProjectList) {
            if (moduleProject.cacheValid) {
                moduleProject.obtainProject().tasks.all { proTask ->
                    proTask.onlyIf { false }
                }
            }
        }
    }

    /**
     * hack其他模块的编译任务
     */
    private fun handleOtherModuleCompile(project: Project) {
        project.rootProject.allprojects { pro ->
            if (pro != project) {
                pro.tasks.withType(AbstractKotlinCompile::class.java).all { task ->
                    Reflect.on(task).set(
                        FastHackPlugin.INJECT_FIELD_NAME,
                        ModuleKCompilerTaskProxy(task)
                    )
                }
                pro.tasks.withType(KaptGenerateStubsTask::class.java).all { task ->
                    Reflect.on(task).set(
                        FastHackPlugin.INJECT_FIELD_NAME,
                        ModuleKaptTaskProxy(task)
                    )
                }
                pro.tasks.withType(KaptWithKotlincTask::class.java).all { task ->
                    Reflect.on(task).set(
                        FastHackPlugin.INJECT_FIELD_NAME,
                        ModuleKaptTaskProxy(task)
                    )
                }
            }
        }
    }

    /**
     * 存储增量配置信息
     */
    private fun handleConfigSave(project: Project) {
        val androidExtension = project.extensions.getByName("android") as BaseAppModuleExtension
        androidExtension.applicationVariants.all { variant ->
            // 在assemble任务之后执行保存配置信息
            variant.assembleProvider.get().doLast {
                for (moduleProject in this.getModuleProjectList()) {
                    this.getPropertyConfig().updateModify(moduleProject)
                }
                this.getPropertyConfig().saveConfig()
            }
        }
    }

    /**
     * 这个函数主要用hook app的AbstractKotlinCompile、KaptGenerateStubsTasktask和KaptWithKotlincTask，从而实现更高的编译效率
     */
    private fun handleHackAppTask(project: Project) {
        /**
         * 处理app工程的编译
         */
        project.tasks.withType(AbstractKotlinCompile::class.java).all { task ->
            Reflect.on(task).set(
                FastHackPlugin.INJECT_FIELD_NAME,
                AppKCompileTaskProxy(task)
            )
        }
        /**
         * 处理app工程的kapt->stub的生产
         */
        project.tasks.withType(KaptGenerateStubsTask::class.java).all { task ->
            Reflect.on(task).set(
                FastHackPlugin.INJECT_FIELD_NAME,
                AppKaptTaskProxy(task)
            )
        }
        /**
         * 处理app工程的注解处理器
         */
        project.tasks.withType(KaptWithKotlincTask::class.java).all { task ->
            Reflect.on(task).set(
                FastHackPlugin.INJECT_FIELD_NAME,
                AppKaptTaskProxy(task)
            )
        }
    }

    /**
     * 判断是否是在编译
     */
    private fun currentTaskIsCompile(): Boolean {
        // 获取有效的启动任务,若没有配置,则采主工程命名的task
        val launcherTaskName = project.gradle.startParameter.taskNames.firstOrNull { taskName ->
            if (projectExtension.detectLauncherRegex.isNullOrBlank()) {
                taskName.contains(project.name)
            } else {
                taskName.contains(projectExtension.detectLauncherRegex)
            }
        }
        // 避免无效的任务执行
        if (launcherTaskName.isNullOrBlank()) {
            FastBuilderLogger.logLifecycle("检测任务不相关不启用替换逻辑")
            return true
        }
        return false
    }

    override fun getContext(): IPluginContext = this

    override fun getProjectExtension(): ProjectExtension = projectExtension

    override fun getApplyProject(): Project = project

    override fun getPropertyConfig(): PropertyFileConfig = propertyFileConfig

    override fun getModuleProjectList(): List<ModuleProject> = moduleProjectList
}
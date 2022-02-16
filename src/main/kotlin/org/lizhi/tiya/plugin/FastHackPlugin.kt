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

import javassist.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.annotation.Annotation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.lizhi.tiya.fileutil.PluginFileHelper
import org.lizhi.tiya.log.FastBuilderLogger
import java.util.*

/**
 * 用于注入HackCompilerIntermediary类字段和hack AbstractKotlinCompile和KaptWithKotlincTask的task方法执行
 */
data class InjectClass(val ctClass: CtClass, val methodName: String = "") {

    /**
     * 注入HackCompilerIntermediary字段到目标类
     */
    fun injectFile() {
        val addField = CtField.make(
            "public org.lizhi.tiya.hack.HackCompilerIntermediary ${FastHackPlugin.INJECT_FIELD_NAME} " +
                    "=new org.lizhi.tiya.hack.HackCompilerIntermediary(this);",
            ctClass
        )
        val annotationsAttribute = AnnotationsAttribute(ctClass.classFile.constPool, AnnotationsAttribute.visibleTag)
        val annotation = Annotation("org.gradle.api.tasks.Internal", ctClass.classFile.constPool)
        annotationsAttribute.addAnnotation(annotation)
        addField.fieldInfo.addAttribute(annotationsAttribute)
        ctClass.addField(addField)
    }

    /**
     * hack目标task的action方法
     */
    fun injectMethod() {
        val declaredMethod = ctClass.getDeclaredMethod(
            methodName,
            arrayOf<CtClass>(ClassPool.getDefault().get("org.gradle.api.tasks.incremental.IncrementalTaskInputs"))
        )
        // 在目标task方法执行前先执行HackCompilerIntermediary的changeIncrementalTaskInputs方法
        declaredMethod.insertBefore(
            "$1 = ${FastHackPlugin.INJECT_FIELD_NAME}.changeIncrementalTaskInputs(inputs);"
                    + "if(${FastHackPlugin.INJECT_FIELD_NAME}.hackTaskAction($1)){return;}"
        )
    }

    fun toCLass() {
        ctClass.toClass()
    }
}


/**
 * 提供了一个hack
 */
class FastHackPlugin : Plugin<Project> {
    companion object {
        // 用于注入HackCompilerIntermediary类的字段名
        const val INJECT_FIELD_NAME: String = "hackField"

        // 用于标记Hack操作相关class加载成功的标记类全名
        const val INJECT_INTERFACE_NAME: String = "org.lizhi.tiya.hack.IHackTaskFlag"
    }

    private val cp: ClassPool = ClassPool.getDefault()

    override fun apply(project: Project) {
        cp.insertClassPath(ClassClassPath(this.javaClass))
        cp.insertClassPath(LoaderClassPath(this.javaClass.classLoader.parent))
        cp.importPackage("org.jetbrains.kotlin.gradle.tasks")
        try {
            loadFlagClass()
            handleJavaCompile()
            handleKotlinCompile()
        } catch (e: Exception) {
            project.exec {
                val cmd = "${project.rootProject.rootDir}/gradlew --stop"
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
                    it.commandLine = listOf("cmd", "/c", cmd)
                } else {
                    it.commandLine = listOf("sh", "-c", cmd)
                }
                Logging.getLogger(project::class.java).error("\n插件注入异常,请重新编译!!!")
            }
        }
    }

    /**
     * 提前加载标记接口类
     */
    private fun loadFlagClass() {
        val flagClass = cp.get(INJECT_INTERFACE_NAME)
        val classLoader = this.javaClass.classLoader.parent
        if (!flagClass.isFrozen) {
            try {
                flagClass.toClass(classLoader)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 对AbstractKotlinCompile和KaptWithKotlincTask进行字节码操作
     */
    private fun handleKotlinCompile() {
        mutableListOf(
            InjectClass(cp.get("org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile"), "execute"),
            InjectClass(cp.get("org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask"), "compile")
        )
            // 过滤已经被注入的对象
            .filter { !hasInjectFlag(it.ctClass) && !it.ctClass.isFrozen }
            // 属性和方法注入
            .forEach { hackClass ->
                hackClass.injectFile()
                hackClass.injectMethod()
                hackClass.toCLass()
            }
    }

    /**
     * 对JavaCompileCreationAction的getType方法进行hack操作，返回我们的HackJavaCompile类
     */
    private fun handleJavaCompile() {
        val fitiClass = cp.get("org.lizhi.tiya.hack.FastIncrementalTaskInputs")
        if (!fitiClass.isFrozen && !hasInjectFlag(fitiClass)) {
            injectFlagInterface(fitiClass)
            fitiClass.toClass(this.javaClass.classLoader.parent)
        }

        val hjcClass = cp.get("org.lizhi.tiya.hack.HackJavaCompile")
        if (!hjcClass.isFrozen && !hasInjectFlag(hjcClass)) {
            injectFlagInterface(hjcClass)
            hjcClass.toClass(this.javaClass.classLoader.parent)
        }

        val wrapperActionClass = cp.get("org.lizhi.tiya.hack.WrapperAction")
        if (!wrapperActionClass.isFrozen && !hasInjectFlag(wrapperActionClass)) {
            injectFlagInterface(wrapperActionClass)
            wrapperActionClass.toClass(this.javaClass.classLoader.parent)
        }

        val jCCAClass = cp.get("com.android.build.gradle.tasks.JavaCompileCreationAction")
        if (!jCCAClass.isFrozen && !hasInjectFlag(jCCAClass)) {
            val declaredMethod = jCCAClass.getDeclaredMethod("getType")
            declaredMethod.setBody(" return org.lizhi.tiya.hack.HackJavaCompile.class;")
            jCCAClass.toClass(this.javaClass.classLoader.parent)
        }
    }

    /**
     * 添加标志接口
     */
    private fun injectFlagInterface(ctClass: CtClass) {
        ctClass.addInterface(ClassPool.getDefault().get(INJECT_INTERFACE_NAME))
    }

    /**
     * 是否存在注入的接口
     */
    private fun hasInjectFlag(ctClass: CtClass): Boolean {
        val filter = ctClass.interfaces.toList().filter {
            it.name.equals(INJECT_INTERFACE_NAME)
        }
        return filter.isNotEmpty()
    }


}
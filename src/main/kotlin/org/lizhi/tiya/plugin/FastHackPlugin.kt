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

import javassist.ClassClassPath
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.annotation.Annotation
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 提供了一个hack
 */
class FastHackPlugin : Plugin<Project> {
    companion object {
        const val INJECT_FIELD_NAME: String = "hackField"
    }

    override fun apply(project: Project) {
        val cp = ClassPool.getDefault()
        cp.insertClassPath(ClassClassPath(this.javaClass))
        cp.importPackage("org.jetbrains.kotlin.gradle.tasks")


//        val cavaCompile = cp.get("org.gradle.api.tasks.compile.JavaCompile")
//        val toClass = cavaCompile.toClass()
        println("")
        println("插件的加载器 ${this.javaClass.classLoader} ${this.javaClass.classLoader.hashCode()}")
//        println("插件的加载器22 ${toClass.classLoader} ${toClass.classLoader.hashCode()}")
        println("")
        val hackClassList = mutableListOf<CtClass>(
//            cp.get("org.gradle.api.tasks.compile.JavaCompile"),
            cp.get("org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile"),
            cp.get("org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask")
        )
        //fun execute(inputs: IncrementalTaskInputs)
        //fun compile(inputs: IncrementalTaskInputs)
        val hackMethodList = listOf<String>(
//             "compile",
            "execute", "compile"
        )

        val hackClassIterator = hackClassList.iterator()
        whileFlag@ while (hackClassIterator.hasNext()) {
            val hackClass = hackClassIterator.next()
            for (ctClass in hackClass.interfaces) {
                if (ctClass.name == "org.lizhi.tiya.hack.IHackTaskFlag") {
                    hackClassIterator.remove()
                    continue@whileFlag
                }
            }
            hackClass.addInterface(cp.get("org.lizhi.tiya.hack.IHackTaskFlag"))
        }

        if (hackClassList.isEmpty()) {
            return
        }


        //属性注入
        for (hackName in hackClassList) {
            injectFile(hackName)
        }
        for (index in hackClassList.indices) {
            val hackClass = hackClassList[index]
            val hackMethod = hackMethodList[index]
            val declaredMethod = hackClass.getDeclaredMethod(
                hackMethod,
                arrayOf<CtClass>(cp.get("org.gradle.api.tasks.incremental.IncrementalTaskInputs"))
            )

            declaredMethod.insertBefore(
                "$1 = ${INJECT_FIELD_NAME}.changeIncrementalTaskInputs(inputs);" +
                        "if(${INJECT_FIELD_NAME}.hackTaskAction($1)){return;}"
            )

        }

        //属性注入
        for (hackName in hackClassList) {
            try {
                hackName.toClass();
                hackName.writeFile("/Users/fmy/IdeaProjects/FastBuilder/gradle/tes")


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val jCCAClass = cp.get("com.android.build.gradle.tasks.JavaCompileCreationAction")


        if (!jCCAClass.isFrozen) {


            val fitiClass = cp.get("org.lizhi.tiya.plugin.FastIncrementalTaskInputs")
            if (!fitiClass.isFrozen) {
                fitiClass.toClass(this.javaClass.classLoader.parent)
            }

            val hjcClass = cp.get("org.lizhi.tiya.hack.HackJavaCompile")
            if (!hjcClass.isFrozen) {
                hjcClass.toClass(this.javaClass.classLoader.parent)
            }
            val wrapperActionClass = cp.get("org.lizhi.tiya.hack.WrapperAction")
            if (!wrapperActionClass.isFrozen) {
                wrapperActionClass.toClass(this.javaClass.classLoader.parent)
            }

            val declaredMethod = jCCAClass.getDeclaredMethod("getType")
            declaredMethod.setBody(" return org.lizhi.tiya.hack.HackJavaCompile.class;")
            jCCAClass.toClass(this.javaClass.classLoader.parent)
            jCCAClass.writeFile("/Users/fmy/IdeaProjects/FastBuilder/gradle/tes")

        }
    }

    fun injectFile(injectClass: CtClass) {
        val addField = CtField.make(
            "public org.lizhi.tiya.hack.HackCompilerIntermediary $INJECT_FIELD_NAME =new org.lizhi.tiya.hack.HackCompilerIntermediary(this);",
            injectClass
        )
        val annotationsAttribute =
            AnnotationsAttribute(injectClass.classFile.constPool, AnnotationsAttribute.visibleTag)
        val annotation = Annotation("org.gradle.api.tasks.Internal", injectClass.classFile.constPool)
        annotationsAttribute.addAnnotation(annotation)
        addField.fieldInfo.addAttribute(annotationsAttribute)
        injectClass.addField(addField)
    }


}
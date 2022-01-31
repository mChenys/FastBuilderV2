/*
 * Copyright (C) 2021 TIYA.
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

package org.lizhi.tiya.hack

import com.android.build.gradle.tasks.JavaCompileCreationAction
import org.gradle.api.Action
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.FastIncrementalTaskInputs
import java.util.ArrayList

open class WrapperAction(val list: ArrayList<InputFileDetails>) : Action<InputFileDetails> {
    override fun execute(t: InputFileDetails) {
        list.add(t)
    }
}

@CacheableTask
open class HackJavaCompile : JavaCompile() {

    val modified = ArrayList<InputFileDetails>()

    val removed = ArrayList<InputFileDetails>()

    override fun compile(input: IncrementalTaskInputs) {
        if (!input.isIncremental) {
            super.compile(input)
        } else {
            modified.clear()
            removed.clear()
            input.outOfDate(WrapperAction(modified))
            input.removed(WrapperAction(removed))
            println("------------------------------------------------------------------------")
            println("+++++++++ ${project.name}:${name}: isIncremental ${input.isIncremental}+++++")
            println("modified ${modified.map { it.file }.map { it.path }}")
            println("removed ${modified.map { it.file }.map { it.path }}")
            println("------------------------------------------------------------------------")
            filterFile(modified.iterator(), project.buildDir.absolutePath)
            filterFile(removed.iterator(), project.buildDir.absolutePath)
            println("-------------------------------过滤结果-----------------------------------------")
            println("+++++++++ ${project.name}:${name}: isIncremental ${input.isIncremental}+++++")
            println("modified ${modified.map { it.file }.map { it.path }}")
            println("removed ${modified.map { it.file }.map { it.path }}")
            println("------------------------------------------------------------------------")

            super.compile(FastIncrementalTaskInputs(input.isIncremental, modified, removed))

        }


        println("触发HackJavaCompile")
    }

    private fun filterFile(
        moIterator: MutableIterator<InputFileDetails>,
        buildDir: String
    ) {
        while (moIterator.hasNext()) {
            val moFileDetails = moIterator.next()
            val moFile = moFileDetails.file
            val moPath = moFile.path
            if (!moFile.path.startsWith(buildDir) && !moPath.endsWith(".kt") && !moPath.endsWith(".java")) {
                moIterator.remove()
            } else {
                if (!moPath.endsWith(".kt") && !moPath.endsWith(".java")) {
                    moIterator.remove()
                    continue
                }
            }

        }
    }
}
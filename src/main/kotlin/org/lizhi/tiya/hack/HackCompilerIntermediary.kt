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

import org.gradle.api.Task
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.FastIncrementalTaskInputs
import java.util.ArrayList

open class HackCompilerIntermediary constructor(val task: Task) {

    open fun hackTaskAction(input: IncrementalTaskInputs): Boolean {
        return false
    }

    private val modified = ArrayList<InputFileDetails>()
    private val removed = ArrayList<InputFileDetails>()

    open fun changeIncrementalTaskInputs(input: IncrementalTaskInputs): IncrementalTaskInputs {
        println("------------task ${task.project.name}:${task.name} ----------")
//        try {
//            input.outOfDate { modified.add(it) }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        input.removed { removed.add(it) }
//        println("modified ${modified.map { it.file }.map { it.path }}")
//        println("removed ${modified.map { it.file }.map { it.path }}")
        println("------------end ----------")


        return input

    }
//
//    open fun obtainChangeFilesWrapper(input: IncrementalTaskInputs): ChangedFileWrapper? {
//        return null
//    }

//    open fun obtainChangeFiles(input: IncrementalTaskInputs): ChangedFiles {
//        val obtainChangeFilesWrapper = obtainChangeFilesWrapper(input)
//        return if (obtainChangeFilesWrapper != null) {
//            ChangedFileWrapper(emptyList(), emptyList()).convert()
//        } else {
//            return ChangedFiles(input)
//        }
//    }
}
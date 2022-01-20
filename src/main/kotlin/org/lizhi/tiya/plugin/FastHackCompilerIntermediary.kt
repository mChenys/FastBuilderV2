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

package org.lizhi.tiya.plugin

import org.gradle.api.Task
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.jetbrains.kotlin.gradle.tasks.HackCompilerIntermediary
import java.util.ArrayList

class FastHackCompilerIntermediary constructor(task: Task) : HackCompilerIntermediary(task) {
    val modified = ArrayList<InputFileDetails>()
    val removed = ArrayList<InputFileDetails>()
    override fun changeIncrementalTaskInputs(input: IncrementalTaskInputs): IncrementalTaskInputs {
        if (!input.isIncremental) {
            return super.changeIncrementalTaskInputs(input)
        }
        modified.clear()
        removed.clear()

        input.outOfDate { modified.add(it) }
        input.removed { removed.add(it) }
        val buildDir = task.project.buildDir.path
        filterFile(modified.iterator(), buildDir)
        filterFile(removed.iterator(), buildDir)

        return FastIncrementalTaskInputs(input.isIncremental, modified, removed)
    }

    override fun hackTaskAction(input: IncrementalTaskInputs):Boolean {
        return if (input.isIncremental && removed.isEmpty() && modified.isEmpty()) {
            true
        } else {
            false
        }
    }

    private fun filterFile(
        moIterator: MutableIterator<InputFileDetails>,
        buildDir: String
    ) {
        while (moIterator.hasNext()) {
            val moFileDetails = moIterator.next()
            val moFile = moFileDetails.file
            val moPath = moFile.path
            if (!moFile.path.startsWith(buildDir)&&!moPath.endsWith(".kt") && !moPath.endsWith(".java")) {
                moIterator.remove()
            } else {
                if (moPath.endsWith(".jar", true)) {
                    moIterator.remove()
                    continue
                }
                if (!moPath.endsWith(".kt") && !moPath.endsWith(".java")) {
                    moIterator.remove()
                    continue
                }
            }

        }
    }
}
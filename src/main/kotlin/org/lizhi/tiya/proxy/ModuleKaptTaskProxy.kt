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

package org.lizhi.tiya.proxy

import org.apache.commons.io.FileUtils
import org.gradle.api.Task
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.lizhi.tiya.hack.HackCompilerIntermediary

/**
 * 处理其他模块的kapt相关的task输入源的代理类
 */
class ModuleKaptTaskProxy constructor(task: Task) : HackCompilerIntermediary(task) {

    override fun hackTaskAction(input: IncrementalTaskInputs): Boolean {
        return task.name.startsWith("kapt", true) && input.isIncremental
                && removed.isEmpty() && modified.isEmpty()
    }

    override fun filterFile(
        moIterator: MutableIterator<InputFileDetails>,
        buildDir: String
    ) {

        while (moIterator.hasNext()) {
            val moFileDetails = moIterator.next()
            val moFile = moFileDetails.file
            val moPath = moFile.path
            val isClassFile = (moPath.endsWith(".kt", true) || moPath.endsWith(".java", true))
            if (!moFile.path.startsWith(buildDir) && !moPath.endsWith(".kt")
                && !moPath.endsWith(".java")
            ) {
                moIterator.remove()
            } else {
                if (moFileDetails.isRemoved) {
                    continue
                }
                if (moPath.endsWith(".jar", true)) {
                    moIterator.remove()
                    continue
                }

                if (isClassFile &&  moFileDetails.isAdded) {
                    continue
                }


//                //todo 这里应该根据内容来定义是否生产
//                if ((moPath.endsWith(".kt") || moPath.endsWith(".java"))) {
//                    moIterator.remove()
//                    continue
//                }

//                if (!isClassFile) {
//                    moIterator.remove()
//                    continue
//                }
                moIterator.remove()
            }

        }
    }
}
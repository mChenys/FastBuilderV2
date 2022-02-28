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
import java.util.ArrayList

/**
 * 用于hack编译相关任务的父类
 */
open class HackCompilerIntermediary constructor(val task: Task) {
    // 存储新增和修改的输入源
    protected val modified = ArrayList<InputFileDetails>()

    // 存储已删除的输入源
    protected val removed = ArrayList<InputFileDetails>()

    /**
     * 返回值表示true表示已经完成对目标task的hack操作，子类可重写
     */
    open fun hackTaskAction(input: IncrementalTaskInputs): Boolean {
        return false
    }

    /**
     * 修改输入源
     */
    fun changeIncrementalTaskInputs(input: IncrementalTaskInputs): IncrementalTaskInputs {
        if (!input.isIncremental) {
            return input
        }
        modified.clear()
        removed.clear()
        input.outOfDate { modified.add(it) }
        input.removed { removed.add(it) }
        loggerBefore(input)
        val buildDir = task.project.buildDir.path
        filterFile(modified.iterator(), buildDir)
        filterFile(removed.iterator(), buildDir)
        loggerAfter(input)
        return FastIncrementalTaskInputs(input.isIncremental, modified, removed)
    }

    /**
     * 子类实现具体的过滤条件
     */
    open fun filterFile(iterator: MutableIterator<InputFileDetails>, buildDir: String) {

    }


    /**
     * changeIncrementalTaskInputs方法处理前打印log
     */
    fun loggerBefore(input: IncrementalTaskInputs) {
        FastBuilderLogger.logLifecycle("-----------------------------------过滤前结果-------------------------------------")
        FastBuilderLogger.logLifecycle("+++++++++ ${task.project.name}:${task.name}: isIncremental ${input.isIncremental}+++++")
        FastBuilderLogger.logLifecycle("modified ${modified.map { it.file }.map { it.path }}")
        FastBuilderLogger.logLifecycle("removed ${removed.map { it.file }.map { it.path }}")
        FastBuilderLogger.logLifecycle("------------------------------------------------------------------------")
    }

    /**
     * changeIncrementalTaskInputs方法处理后打印log
     */
    fun loggerAfter(input: IncrementalTaskInputs) {
        FastBuilderLogger.logLifecycle("-------------------------------过滤后结果-----------------------------------------")
        FastBuilderLogger.logLifecycle("+++++++++ ${task.project.name}:${task.name}: isIncremental ${input.isIncremental}+++++")
        FastBuilderLogger.logLifecycle("modified ${modified.map { it.file }.map { it.path }}")
        FastBuilderLogger.logLifecycle("removed ${removed.map { it.file }.map { it.path }}")
        FastBuilderLogger.logLifecycle("------------------------------------------------------------------------")
    }

}
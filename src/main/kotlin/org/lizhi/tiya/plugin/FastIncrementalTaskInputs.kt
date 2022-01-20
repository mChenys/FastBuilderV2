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

import org.gradle.api.Action
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

class FastIncrementalTaskInputs(
    private val incremental: Boolean,
    private val outOfDataList: List<InputFileDetails>,
    private val removeList: List<InputFileDetails>
) : IncrementalTaskInputs {

    override fun isIncremental() = incremental

    override fun outOfDate(action: Action<in InputFileDetails>) {
        for (inputFileDetails in outOfDataList) {
            action.execute(inputFileDetails)
        }
    }

    override fun removed(action: Action<in InputFileDetails>) {
        for (inputFileDetails in removeList) {
            action.execute(inputFileDetails)
        }
    }
}
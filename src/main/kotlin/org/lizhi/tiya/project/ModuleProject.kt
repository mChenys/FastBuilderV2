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

package org.lizhi.tiya.project

import org.gradle.api.Project
import org.lizhi.tiya.plugin.AppHelper
import org.lizhi.tiya.plugin.IPluginContext

/**
 * 和module对应的工程
 */
class ModuleProject(val project: Project, private val pluginContext: IPluginContext) {

    /**
     * 缓存是否有效
     */
    var cacheValid = false


    /**
     * 最後修改文件夾的時間
     */
    private var lastModified: Long = 0


    /**
     * 这个project文件下的最后修改时间
     */
    fun obtainLastModified(): Long {
        if (lastModified <= 0) {
            lastModified = AppHelper.obtainLastModified(project)
        }
        return lastModified
    }

    /**
     * 这个project文件下的最后修改时间
     */
    fun obtainKeyName(): String {
        return project.path.replace(":", "_")
    }


}
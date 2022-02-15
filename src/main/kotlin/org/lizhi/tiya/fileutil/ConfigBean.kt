package org.lizhi.tiya.fileutil


data class ConfigBean(val exclude: List<String>, val enable: Boolean = true, val logEnable: Boolean = true)
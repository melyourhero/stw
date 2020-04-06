package org.gravetti.stw.main.ext

import org.apache.commons.io.FileUtils
import java.io.File

fun String.normalizePath() = this
        .let {
            if (!it.startsWith("~")) {
                it
            } else {
                it.replaceFirst("~", FileUtils.getUserDirectoryPath())
            }
        }

fun String.convertToFile(): File = File(this.normalizePath())

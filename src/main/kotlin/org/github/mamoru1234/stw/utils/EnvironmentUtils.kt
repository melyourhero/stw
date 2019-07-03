package org.github.mamoru1234.stw.utils

import org.apache.commons.io.FileUtils.getFile
import org.apache.commons.io.FileUtils.getUserDirectory
import java.io.File

fun getWorkingDir(): File {
    val workingDir = getFile(getUserDirectory(), ".stw_new")
    if (!workingDir.exists()) {
        workingDir.mkdirs()
    }
    return workingDir
}

package org.gravetti.stw.main.utils

import org.gravetti.stw.main.ext.convertToFile

fun validateDir(value: String): Boolean {
    val file = value.convertToFile()
    return file.exists() && file.isDirectory
}

fun validateFile(value: String): Boolean {
    val file = value.convertToFile()
    return file.exists() && file.isFile
}

fun nonEmpty(value: String): Boolean {
    return value.isNotEmpty()
}

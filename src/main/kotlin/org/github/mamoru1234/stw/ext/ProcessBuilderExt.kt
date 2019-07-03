package org.github.mamoru1234.stw.ext

import com.github.ajalt.clikt.core.PrintMessage
import java.util.*

fun ProcessBuilder.commandString(command: String): ProcessBuilder {
    if (command.isEmpty())
        throw IllegalArgumentException("Empty command")

    val st = StringTokenizer(command)
    val cmdArray = arrayOfNulls<String>(st.countTokens())
    var i = 0
    while (st.hasMoreTokens()) {
        cmdArray[i] = st.nextToken()
        i++
    }
    return command(*cmdArray)
}

fun ProcessBuilder.shellCommand(command: String, silent: Boolean = false): ProcessBuilder {
    if (command.isEmpty())
        throw IllegalArgumentException("Empty command")
    if (!silent) {
        println("$$> $command")
    }
    return command("sh", "-c", command)
}

fun ProcessBuilder.environment(env: Map<String, String>): ProcessBuilder {
    env.forEach {
        environment(it.key, it.value)
    }
    return this
}

fun ProcessBuilder.environment(key: String, value: String): ProcessBuilder {
    environment()[key] = value
    return this
}

fun ProcessBuilder.inheritOutput(): ProcessBuilder {
    return this
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
}

fun Process.waitForString(): String = this.let {
    val stringBuilder = StringBuilder()
    val stdoutScanner = Scanner(it.inputStream)
    while (stdoutScanner.hasNextLine()) {
        stringBuilder.append(stdoutScanner.nextLine())
        stringBuilder.append(System.lineSeparator())
    }
    stringBuilder.toString()
}

fun Process.saveWait() {
    val code = this.waitFor()
    if (code != 0) {
        throw PrintMessage("Failed to execute command")
    }
}

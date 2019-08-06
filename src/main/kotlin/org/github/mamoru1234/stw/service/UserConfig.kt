package org.github.mamoru1234.stw.service

import com.github.ajalt.clikt.output.TermUi
import mu.KotlinLogging
import org.apache.commons.io.FileUtils.getFile
import org.github.mamoru1234.stw.utils.getWorkingDir
import org.github.mamoru1234.stw.utils.nonEmpty
import java.io.FileInputStream
import java.io.FileWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

class UserConfig {
    private val log = KotlinLogging.logger {}

    private val userProperties: Properties by lazy {
        log.info("Init of user properties")
        val file = getFile(getWorkingDir(), "user.properties")
        if (!file.isFile) {
            file.createNewFile()
        }
        val props = Properties()
        props.load(FileInputStream(file))
        return@lazy props
    }

    val machineIP: String by lazy {
        val publicHost = System.getenv("PUBLIC_HOST")
        if (publicHost != null) {
            return@lazy publicHost
        }
        if (hasProperty(MACHINE_IP)) {
            return@lazy getProperty(MACHINE_IP)!!
        }
        Socket().use {
            it.connect(InetSocketAddress("google.com", 80))
            val hostAddress = it.localAddress.hostAddress
            log.debug("Detected ip: $hostAddress")
            return@lazy hostAddress
        }
    }

    fun getProperty(name: String): String? {
        return userProperties.getProperty(name)
    }

    fun getProperty(name: String, defaultValue: String): String {
        return userProperties.getProperty(name) ?: defaultValue
    }

    fun hasProperty(name: String): Boolean = userProperties.getProperty(name) != null

    fun prompt(name: String, userMessage: String, validate: ((value: String) -> Boolean)?): String {
        while (true) {
            val userValue = TermUi.prompt(userMessage) ?: continue
            val validation = validate?.invoke(userValue) ?: false
            if (!validation) {
                log.info("Wrong value $userValue")
                continue
            }
            userProperties[name] = userValue
            saveProperties()
            return userValue
        }
    }

    fun readValue(name: String, userMessage: String, validate: ((value: String) -> Boolean)?): String {
        return getProperty(name)
            ?: prompt(name, userMessage, validate)
    }

    fun getDockerImageName(imageSuf: String): String {
        val registry = readValue(DOCKER_REGISTRY_URL, "Enter docker registry", ::nonEmpty)
        return "$registry/$imageSuf"
    }

    private fun saveProperties() {
        val file = getFile(getWorkingDir(), "user.properties")
        val fileWriter = FileWriter(file)
        userProperties.store(fileWriter, "user.properties")
        fileWriter.close()
    }
}

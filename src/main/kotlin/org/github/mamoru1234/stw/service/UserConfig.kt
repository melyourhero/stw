package org.github.mamoru1234.stw.service

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.output.TermUi
import mu.KotlinLogging
import org.apache.commons.io.FileUtils.getFile
import org.github.mamoru1234.stw.utils.getWorkingDir
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.io.FileWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

@Component
class UserConfig {
    private val log = KotlinLogging.logger {}

    private val defaults = mapOf(
        UI_IMAGE to "gravetti/gravetti-ui:2.1.3",
        CSV_ADAPTER_IMAGE to "gravetti/engine-csv-adapter:2.0.5",
        DEMO_ADAPTER_IMAGE to "gravetti/engine-demo-adapter:2.0.1",
        ATOM_IMAGE to "gravetti/gravetti-engine:2.0.7",
        CLOUD_MAX_FILE_SIZE to "100MB",
        CLOUD_HEALTH_SLEEP to "5000",
        ATOM_DISABLE_ANALYTICS to "n",
        CASSANDRA_XMX to "2048m",
        CASSANDRA_XMS to "512m",
        DOCKER_REGISTRY_URL to "docker-private.genesisnet.com"
    )

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

    fun getPropertyWithDefault(name: String): String {
        return getProperty(name) ?: defaults[name] ?: throw PrintMessage("No default defined for $name")
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
        val registry = getPropertyWithDefault(DOCKER_REGISTRY_URL)
        return "$registry/$imageSuf"
    }

    private fun saveProperties() {
        val file = getFile(getWorkingDir(), "user.properties")
        val fileWriter = FileWriter(file)
        userProperties.store(fileWriter, "user.properties")
        fileWriter.close()
    }
}

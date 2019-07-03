package org.github.mamoru1234.stw.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.PrintMessage
import mu.KLogging
import org.apache.commons.io.FileUtils
import org.github.mamoru1234.stw.ext.convertToFile
import org.github.mamoru1234.stw.utils.getArrayNode
import org.github.mamoru1234.stw.utils.getObjectNode
import org.github.mamoru1234.stw.utils.validateFile
import java.io.File

class CloudComposeService(
        private val yamlMapper: ObjectMapper,
        private val userConfig: UserConfig
) {
    companion object: KLogging()

    fun prepareComposeDir(srcComposeDir: File, dstComposeDir: File) {
        if (dstComposeDir.isDirectory) {
            FileUtils.deleteDirectory(dstComposeDir)
        }
        val composeNode = yamlMapper.readTree(FileUtils.getFile(srcComposeDir, "docker-compose-public.yml"))
        val cassandraNode = getObjectNode(composeNode, arrayOf("services", "cassandra"))
        processCassandra(cassandraNode)
        processCloud(getObjectNode(composeNode, arrayOf("services", "cloud")))
        ensureProxy(composeNode)
        FileUtils.copyFile(
                FileUtils.getFile(srcComposeDir, ".env"),
                FileUtils.getFile(dstComposeDir, ".env")
        )
        yamlMapper.writeValue(
                FileUtils.getFile(dstComposeDir, "docker-compose.yml"),
                composeNode
        )
    }

    private fun parseEnvironment(rawEnv: ArrayNode): Map<String, String> = rawEnv.map {
                val text = it.asText()
                val eqInd = text.indexOf("=")
                text.substring(0, eqInd) to text.substring(eqInd + 1)
            }.toMap()

    private fun parseEnvironment(baseNode: ObjectNode): Map<String, String> = parseEnvironment(
            getArrayNode(baseNode, arrayOf("environment")))

    private fun convertEnv(env: Map<String, String>): ArrayNode = yamlMapper.createArrayNode().apply {
        env.forEach {
            val item = "${it.key}=${it.value}"
            this.add(item)
        }
    }

    private fun processEnv(node: ObjectNode, processor: (env: MutableMap<String, String>) -> Unit) {
        val env = parseEnvironment(node).toMutableMap()
        processor(env)
        node.set("environment", convertEnv(env))
    }

    private fun processCassandra(cassandraNode: ObjectNode) {
        val cassandraXmx = userConfig.getProperty("cloud.cassandra.xmx")
        val cassandraXms = userConfig.getProperty("cloud.cassandra.xms")
        processEnv(cassandraNode) {
            env ->
            env["JVM_OPTS"] = "-Xmx$cassandraXmx -Xms$cassandraXms"
        }
    }
    private fun processCloud(cloudNode: ObjectNode) {
        processEnv(cloudNode) {
            env ->
            if (userConfig.hasProperty(CLOUD_ENV_FILE_PATH)) {
                val cloudEnvValue = userConfig.getProperty(CLOUD_ENV_FILE_PATH)!!
                if (!validateFile(cloudEnvValue)) {
                    throw PrintMessage("Coudn't use env from file: $cloudEnvValue")
                }
                val cloudEnvFile = cloudEnvValue.convertToFile()
                val userEnv: Map<String, String> = yamlMapper.readValue(cloudEnvFile)
                logger.info { "Adding user env from file: ${cloudEnvFile.path}" }
                env.putAll(userEnv)
            }
            val disableAtomAnalytics = userConfig.getProperty("atom.disableAnalytics", "n") == "y"
            if (disableAtomAnalytics) {
                logger.debug("Disabling analytics on atom")
                env["RIOT_CLOUD_ENABLE_ANALYTICS_ON_EDGE"] = "false"
            }
            val fileSizeLimit = userConfig.getProperty("cloud.maxFileSize", "100MB")
            logger.debug("Cloud file limit is: $fileSizeLimit")
            env["SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE"] = fileSizeLimit
            env["SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE"] = fileSizeLimit
        }
    }

    private fun ensureProxy(composeNode: JsonNode) {
        if (!userConfig.hasProperty(RIOT_PROXY_CONFIG_PATH)) {
            return
        }
        logger.debug("Adding proxy to cloud compose")
        val riotProxyConfig = userConfig.getProperty(RIOT_PROXY_CONFIG_PATH)!!.convertToFile()
        val proxyNode = yamlMapper.createObjectNode()
        proxyNode.set("image", TextNode("nginx:1.15-alpine"))
        proxyNode.set("ports", yamlMapper.createArrayNode().add("5987:80"))
        val volumes = yamlMapper.createArrayNode()
                .add("${riotProxyConfig.absolutePath}:/etc/nginx/conf.d/default.conf:ro")
        proxyNode.set("volumes", volumes)
        proxyNode.set("depends_on", yamlMapper.createArrayNode().add("cloud").add("keycloak"))

        val cloudNode = getObjectNode(composeNode, arrayOf("services", "cloud"))
        processEnv(cloudNode) {
            env ->
            env["KEYCLOAK_AUTH_SERVER_URL"] = "http://${userConfig.machineIP}:5987/keycloak/auth"
        }

        val authNode = getObjectNode(composeNode, arrayOf("service", "keycloak"))
        processEnv(authNode) {
            env ->
            env["PROXY_ADDRESS_FORWARDING"] = "true"
        }

        val services = getObjectNode(composeNode, arrayOf("services"))
        services.set("stw-proxy", proxyNode)
    }
}

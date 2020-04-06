package org.gravetti.stw.main.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.PrintMessage
import mu.KLogging
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.getFile
import org.gravetti.stw.main.ext.convertToFile
import org.gravetti.stw.main.utils.getArrayNode
import org.gravetti.stw.main.utils.getObjectNode
import org.gravetti.stw.main.utils.validateFile
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.File

@Service
class CloudComposeService(
    @Qualifier("appYamlMapper")
    private val yamlMapper: ObjectMapper,
    private val userConfig: UserConfig
) {
    companion object: KLogging()

    private val log = KotlinLogging.logger {}

    fun prepareComposeDir(srcComposeDir: File, dstComposeDir: File) {
        if (dstComposeDir.isDirectory) {
            FileUtils.deleteDirectory(dstComposeDir)
        }
        FileUtils.copyDirectory(srcComposeDir, dstComposeDir)
        val dockerComposePublicYamlFile = getFile(dstComposeDir, "docker-compose-public.yml")
        val composeNode = yamlMapper.readTree(dockerComposePublicYamlFile)
        val cassandraNode = getObjectNode(composeNode, arrayOf("services", "cassandra"))
        processCassandra(cassandraNode)
        processCloud(getObjectNode(composeNode, arrayOf("services", "cloud-master-node")))
        ensureProxy(composeNode)
        val dockerComposeYamlFile = getFile(dstComposeDir, "docker-compose.yml")
        yamlMapper.writeValue(dockerComposeYamlFile, composeNode)
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
        val cassandraXmx = userConfig.getPropertyWithDefault(CASSANDRA_XMX)
        val cassandraXms = userConfig.getPropertyWithDefault(CASSANDRA_XMS)
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
            val disableAtomAnalytics = userConfig.getPropertyWithDefault(ATOM_DISABLE_ANALYTICS) == "y"
            if (disableAtomAnalytics) {
                logger.debug("Disabling analytics on atom")
                env["RIOT_CLOUD_ENABLE_ANALYTICS_ON_EDGE"] = "false"
            }
            val fileSizeLimit = userConfig.getPropertyWithDefault(CLOUD_MAX_FILE_SIZE)
            logger.debug("Cloud file limit is: $fileSizeLimit")
            env["SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE"] = fileSizeLimit
            env["SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE"] = fileSizeLimit
            val ignoreLicense = userConfig.getPropertyWithDefault(IGNORE_LICENSE) === "y"
            if (ignoreLicense) {
                env.remove("RIOT_CLOUD_LICENSE_KEY_FILE")
            }
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

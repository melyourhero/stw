package org.gravetti.stw.main.client.docker

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.PrintMessage

class DockerContainerInfoDeserializer: StdDeserializer<DockerContainerInfo>(DockerContainerInfo::class.java) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): DockerContainerInfo {
        val jsonNode = jp.codec.readValue(jp, JsonNode::class.java)

        return DockerContainerInfo(
                id = jsonNode["Id"].asText(),
                name = jsonNode["Name"].asText().substring(1),
                state = jsonNode["State"]["Status"].asText()
        ).apply {
            val networkSettings = jsonNode["NetworkSettings"]
            val ports = convertToObject(networkSettings["Ports"])
            this.args = jacksonObjectMapper().readValue(jsonNode["Args"].toString())
            ports.fields().forEach {
                val fieldName = it.key
                it.value[0]?.get("HostPort")?.let {
                    this.ports += it.asText() to fieldName.split("/")[0]
                }
            }
            val networksNode = convertToObject(networkSettings["Networks"])
            this.networkNames = networksNode.fieldNames()
                    .asSequence().toList()
            networksNode.fields().forEach {
                this.ips += it.key to it.value["IPAddress"].asText()
            }
        }
    }

    private fun convertToObject(node: JsonNode?): ObjectNode {
        if (node == null) {
            throw PrintMessage("Node is null")
        }
        if (node.nodeType != JsonNodeType.OBJECT) {
            throw PrintMessage("Node is not a object")
        }
        return node as ObjectNode
    }
}

package org.gravetti.stw.main.client.docker

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(using = DockerContainerInfoDeserializer::class)
data class DockerContainerInfo(
        val id: String,

        val name: String,

        val state: String,

        var args: List<String> = emptyList(),

        var ports: List<Pair<String, String>> = emptyList(),

        var networkNames: List<String> = emptyList(),

        var ips: Map<String, String> = emptyMap()
)

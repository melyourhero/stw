package org.github.mamoru1234.stw.client.docker

class DockerRunOptions(
        val imageName: String
) {
    var name: String? = null
    var detached: Boolean = true
    var env: List<Pair<String, String>> = emptyList()
    var ports: List<Pair<String, String>> = emptyList()
    var volumes: List<String> = emptyList()
    var network: String? = null
    var execOptions: String? = null
}

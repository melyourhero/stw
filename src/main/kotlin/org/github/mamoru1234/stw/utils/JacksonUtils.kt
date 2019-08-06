package org.github.mamoru1234.stw.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.ajalt.clikt.core.PrintMessage

private fun getNode(node: JsonNode, path: Array<String>): JsonNode = path.fold(node) { cur, property ->
    return@fold cur.get(property) ?: throw PrintMessage("Cannot get by path: ${path.joinToString(".")}")
}

fun getArrayNode(node: JsonNode, path: Array<String>): ArrayNode {
    val targetNode = getNode(node, path)
    if (!targetNode.isArray) {
        throw PrintMessage("node: $path is not array")
    }
    return targetNode as ArrayNode
}

fun getObjectNode(node: JsonNode, path: Array<String>): ObjectNode {
    val targetNode = getNode(node, path)
    if (!targetNode.isObject) {
        throw PrintMessage("node: $path is not object")
    }
    return targetNode as ObjectNode
}


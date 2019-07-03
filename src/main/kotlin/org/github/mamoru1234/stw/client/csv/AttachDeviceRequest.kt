package org.github.mamoru1234.stw.client.csv

import com.fasterxml.jackson.databind.node.ObjectNode

data class AttachDeviceRequest(
        var deviceId: String,
        var fileId: String,
        var columnMappings: Map<String, ObjectNode>,
        var name: String? = null,
        var delay: Int?,
        var deviceDimensions: Map<String, String> = emptyMap()
)

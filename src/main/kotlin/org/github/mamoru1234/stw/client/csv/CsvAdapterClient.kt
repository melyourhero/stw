package org.github.mamoru1234.stw.client.csv

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.ajalt.clikt.core.PrintMessage
import mu.KLogging
import okhttp3.MediaType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.github.mamoru1234.stw.ext.createPartFromFile
import org.github.mamoru1234.stw.ext.execRetry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileReader

@Component
class CsvAdapterClient(
    @Qualifier("appJsonMapper")
    private val mapper: ObjectMapper
) {
    companion object: KLogging()

    fun attachDevice(
            deviceName: String,
            adapterUrl: String,
            deviceId: String,
            data: File,
            delay: Int? = null) {
        val client = CsvAdapterApiClient.getClient(adapterUrl, mapper)
        logger.info("Uploading file...")
        val fileId = client.uploadFile(createPartFromFile("file", data, MediaType.get("application/octet-stream")))
            .execRetry()
            .body() ?: throw PrintMessage("Empty body received")
        val mappings = generateMappings(data)
        logger.info { "File id: $fileId" }
        logger.debug("Mappings:")
        logger.debug(mapper.writeValueAsString(mappings))
        val request = AttachDeviceRequest(
            deviceId = deviceId,
            fileId = fileId,
            name = deviceName,
            columnMappings = mappings,
            delay = if (hasTimestampColumn(mappings)) null else delay
        )
        client.attachDevice(request).execRetry()
    }

    private fun generateMappings(data: File): Map<String, ObjectNode> {
        val parser = CSVParser(FileReader(data), CSVFormat.DEFAULT)
        val records = parser.records
        if (records.size < 2) {
            throw PrintMessage("Csv data not enough min number of rows: 2")
        }
        val columns = records.first()
        val dataRecords = records.drop(1)
        logger.debug("Data length: ${dataRecords.size}")
        logger.debug("Columns in file: $columns")
        return columns.mapIndexed {
            ind, columnName ->
            if (columnName.toLowerCase() == "timestamp") {
                return@mapIndexed columnName to mapper.createObjectNode()
                        .put("columnType", "TIMESTAMP")
            }
            val hasText = hasTextValue(dataRecords, ind)
            logger.debug("Is records has text: $hasText")
            logger.debug("Column name: $columnName")
            if (hasText) {
                return@mapIndexed columnName to mapper.createObjectNode()
                        .put("columnType", "SENSOR_VALUE")
                        .put("sensorType", "STRING")
            }
            val (min, max) = calculateDataRange(dataRecords, ind)
            logger.debug("Data range for $columnName min: $min max: $max")
            return@mapIndexed columnName to mapper.createObjectNode()
                    .put("columnType", "SENSOR_VALUE")
                    .put("sensorType", "FLOAT")
                    .put("min", min)
                    .put("max", max)
        }.toMap()
    }
}

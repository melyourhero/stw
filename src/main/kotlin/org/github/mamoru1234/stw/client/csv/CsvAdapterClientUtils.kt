package org.riot.platform.client.csv

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.csv.CSVRecord
import org.riot.platform.exception.StwException
import java.lang.NumberFormatException

fun calculateDataRange(records: List<CSVRecord>, columnNumber: Int): Pair<Double, Double> {
    return records.fold(Double.MAX_VALUE to Double.MIN_VALUE) { acc, record ->
        try {
            val value = record.get(columnNumber).toDouble()
            return@fold Math.min(acc.first, value) to Math.max(acc.second, value)

        } catch (e: NumberFormatException) {
            throw StwException("Invalid csv record at: ${record.recordNumber}:$columnNumber [$record]")
        }
    }
}

fun hasTextValue(records: List<CSVRecord>, columnNumber: Int): Boolean = records
        .any { record ->
            val value = record.get(columnNumber)
            val isNumeric = value.matches("-?\\d+(\\.\\d+)?".toRegex())
            return !isNumeric
        }

fun hasTimestampColumn(mappings: Map<String, ObjectNode>) = mappings
        .any { it.value.get("columnType").textValue() == "TIMESTAMP" }

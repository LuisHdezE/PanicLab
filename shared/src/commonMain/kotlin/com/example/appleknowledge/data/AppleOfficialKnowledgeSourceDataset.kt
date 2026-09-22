package com.example.appleknowledge.data

import com.example.appleknowledge.model.AppleKnowledgeAuthority
import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import com.example.appleknowledge.model.AppleKnowledgeSourceType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AppleKnowledgeSourceSeedRecord(
    val source: AppleKnowledgeSource,
    val panicLabUtility: String,
    val proposedModule: String
)

data class AppleOfficialKnowledgeSourceSeedPart(
    val datasetVersion: String,
    val part: Int,
    val expectedPartCount: Int,
    val records: List<AppleKnowledgeSourceSeedRecord>
)

object AppleOfficialKnowledgeSourceJsonParser {
    fun parse(jsonString: String): AppleOfficialKnowledgeSourceSeedPart {
        val root = Json.parseToJsonElement(jsonString).jsonObject
        val datasetVersion = root.requiredString("datasetVersion")
        val part = root.requiredInt("part")
        val expectedPartCount = root.requiredInt("expectedPartCount")
        val records = root.requiredArray("sources").mapIndexed { index, element ->
            val sourceObject = element as? JsonObject
                ?: throw IllegalArgumentException("sources[$index] must be an object")

            val sourceType = sourceObject.requiredEnum(
                name = "sourceType",
                parser = AppleKnowledgeSourceType::valueOf
            )
            val sourceStatus = sourceObject.requiredEnum(
                name = "sourceStatus",
                parser = AppleKnowledgeSourceStatus::valueOf
            )
            val authority = sourceObject.requiredEnum(
                name = "authority",
                parser = AppleKnowledgeAuthority::valueOf
            )

            AppleKnowledgeSourceSeedRecord(
                source = AppleKnowledgeSource(
                    id = sourceObject.requiredString("id"),
                    sourceType = sourceType,
                    topic = sourceObject.requiredString("topic"),
                    applicableModels = listOf(sourceObject.requiredString("modelScope")),
                    title = sourceObject.requiredString("title"),
                    officialUrl = sourceObject.requiredString("officialUrl"),
                    sourceStatus = sourceStatus,
                    authority = authority,
                    verifiedAt = sourceObject.requiredString("verifiedAt"),
                    notes = sourceObject.nullableString("notes")
                ),
                panicLabUtility = sourceObject.requiredString("panicLabUtility"),
                proposedModule = sourceObject.requiredString("proposedModule")
            )
        }

        return AppleOfficialKnowledgeSourceSeedPart(
            datasetVersion = datasetVersion,
            part = part,
            expectedPartCount = expectedPartCount,
            records = records
        )
    }

    private fun JsonObject.requiredString(name: String): String =
        this[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required string '$name'")

    private fun JsonObject.requiredInt(name: String): Int =
        requiredString(name).toIntOrNull()
            ?: throw IllegalArgumentException("Missing required integer '$name'")

    private fun JsonObject.requiredArray(name: String): JsonArray =
        this[name] as? JsonArray
            ?: throw IllegalArgumentException("Missing required array '$name'")

    private fun JsonObject.nullableString(name: String): String? {
        val value = this[name] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.contentOrNull
    }

    private fun <T> JsonObject.requiredEnum(name: String, parser: (String) -> T): T {
        val raw = requiredString(name)
        return runCatching { parser(raw) }
            .getOrElse { throw IllegalArgumentException("Invalid '$name' value '$raw'") }
    }
}

object AppleOfficialKnowledgeSourceValidator {
    private val semVerRegex = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val sourceIdRegex = Regex("^AOK-\\d{3}$")
    private val isoDateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val appleOfficialUrlRegex = Regex(
        "^https://(?:[A-Za-z0-9-]+\\.)*apple\\.com(?:/.*)?$",
        RegexOption.IGNORE_CASE
    )

    fun validate(parts: List<AppleOfficialKnowledgeSourceSeedPart>): AppleKnowledgeDatasetValidationResult {
        val errors = mutableListOf<AppleKnowledgeDatasetIssue>()
        val warnings = mutableListOf<AppleKnowledgeDatasetIssue>()

        if (parts.isEmpty()) {
            errors += error("sources", "source seed parts must not be empty")
            return result(errors, warnings)
        }

        parts.forEachIndexed { index, part ->
            if (!semVerRegex.matches(part.datasetVersion)) {
                errors += error("parts[$index].datasetVersion", "datasetVersion must use MAJOR.MINOR.PATCH")
            }
            if (part.datasetVersion != "1.0.0") {
                errors += error("parts[$index].datasetVersion", "frozen v1 source catalog must use datasetVersion 1.0.0")
            }
            if (part.expectedPartCount != 3) {
                errors += error("parts[$index].expectedPartCount", "source catalog must declare three parts")
            }
        }

        val partNumbers = parts.map { it.part }
        if (partNumbers.sorted() != listOf(1, 2, 3)) {
            errors += error("parts", "source catalog parts must be exactly 1, 2 and 3")
        }

        val records = parts.flatMap { it.records }
        if (records.size != 91) {
            errors += error("sources", "frozen v1 source count must be 91, got ${records.size}")
        }

        val ids = mutableSetOf<String>()
        records.forEachIndexed { index, record ->
            val source = record.source
            val path = "sources[$index]"

            if (!sourceIdRegex.matches(source.id)) {
                errors += error("$path.id", "invalid source id '${source.id}'")
            }
            if (!ids.add(source.id)) {
                errors += error("$path.id", "duplicate source id '${source.id}'")
            }
            if (!appleOfficialUrlRegex.matches(source.officialUrl)) {
                errors += error("$path.officialUrl", "URL must be HTTPS on an Apple-controlled apple.com host")
            }
            if (!isoDateRegex.matches(source.verifiedAt)) {
                errors += error("$path.verifiedAt", "verifiedAt must use YYYY-MM-DD")
            }
            if (record.panicLabUtility.isBlank()) {
                errors += error("$path.panicLabUtility", "panicLabUtility must not be blank")
            }
            if (record.proposedModule != "Apple Official Knowledge") {
                errors += error("$path.proposedModule", "proposedModule must remain Apple Official Knowledge")
            }
        }

        return result(errors, warnings)
    }

    private fun result(
        errors: List<AppleKnowledgeDatasetIssue>,
        warnings: List<AppleKnowledgeDatasetIssue>
    ) = AppleKnowledgeDatasetValidationResult(
        isValid = errors.isEmpty(),
        errors = errors,
        warnings = warnings
    )

    private fun error(path: String, message: String) = AppleKnowledgeDatasetIssue(
        type = AppleKnowledgeDatasetIssueType.ERROR,
        path = path,
        message = message
    )
}
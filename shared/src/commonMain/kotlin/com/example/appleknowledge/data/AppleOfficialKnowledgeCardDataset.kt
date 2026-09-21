package com.example.appleknowledge.data

import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AppleKnowledgeCardSeedRecord(
    val id: String,
    val modelScopeRaw: String,
    val category: AppleKnowledgeCategory,
    val subcategory: String,
    val symptoms: String,
    val quickChecks: String,
    val appleDiagnostics: String,
    val inspectionOrDiscard: String,
    val appleAction: String,
    val primarySourceUrl: String,
    val secondarySourceRaw: String,
    val sourceStatusRaw: String,
    val detailLevelRaw: String,
    val panicLabCorrelation: String,
    val rulePackEffectRaw: String,
    val stateRaw: String,
    val verifiedAt: String,
    val applicabilityNotes: String
)

data class AppleOfficialKnowledgeCardSeedPart(
    val datasetVersion: String,
    val part: Int,
    val expectedPartCount: Int,
    val records: List<AppleKnowledgeCardSeedRecord>
)

data class AppleKnowledgeNormalizedSourceStatus(
    val primary: AppleKnowledgeSourceStatus,
    val contextual: List<AppleKnowledgeSourceStatus>
)

object AppleOfficialKnowledgeCardJsonParser {
    private const val COLUMN_COUNT = 18

    fun parse(jsonString: String): AppleOfficialKnowledgeCardSeedPart {
        val root = Json.parseToJsonElement(jsonString).jsonObject
        val datasetVersion = root.requiredString("datasetVersion")
        val part = root.requiredInt("part")
        val expectedPartCount = root.requiredInt("expectedPartCount")
        val records = root.requiredArray("rows").mapIndexed { index, element ->
            val row = element as? JsonArray
                ?: throw IllegalArgumentException("rows[$index] must be an array")
            if (row.size > COLUMN_COUNT) {
                throw IllegalArgumentException("rows[$index] has ${row.size} columns; expected at most $COLUMN_COUNT")
            }

            val categoryRaw = row.requiredStringAt(2, "rows[$index].category")
            val category = runCatching { AppleKnowledgeCategory.valueOf(categoryRaw) }
                .getOrElse { throw IllegalArgumentException("Invalid card category '$categoryRaw' at rows[$index]") }

            AppleKnowledgeCardSeedRecord(
                id = row.requiredStringAt(0, "rows[$index].id"),
                modelScopeRaw = row.requiredStringAt(1, "rows[$index].modelScopeRaw"),
                category = category,
                subcategory = row.requiredStringAt(3, "rows[$index].subcategory"),
                symptoms = row.stringAt(4),
                quickChecks = row.stringAt(5),
                appleDiagnostics = row.stringAt(6),
                inspectionOrDiscard = row.stringAt(7),
                appleAction = row.stringAt(8),
                primarySourceUrl = row.requiredStringAt(9, "rows[$index].primarySourceUrl"),
                secondarySourceRaw = row.stringAt(10),
                sourceStatusRaw = row.requiredStringAt(11, "rows[$index].sourceStatusRaw"),
                detailLevelRaw = row.requiredStringAt(12, "rows[$index].detailLevelRaw"),
                panicLabCorrelation = row.stringAt(13),
                rulePackEffectRaw = row.requiredStringAt(14, "rows[$index].rulePackEffectRaw"),
                stateRaw = row.requiredStringAt(15, "rows[$index].stateRaw"),
                verifiedAt = row.requiredStringAt(16, "rows[$index].verifiedAt"),
                applicabilityNotes = row.stringAt(17)
            )
        }

        return AppleOfficialKnowledgeCardSeedPart(
            datasetVersion = datasetVersion,
            part = part,
            expectedPartCount = expectedPartCount,
            records = records
        )
    }

    fun normalizeSourceStatus(raw: String): AppleKnowledgeNormalizedSourceStatus = when (raw) {
        "CURRENT" -> AppleKnowledgeNormalizedSourceStatus(
            primary = AppleKnowledgeSourceStatus.CURRENT,
            contextual = emptyList()
        )
        "HISTORICAL" -> AppleKnowledgeNormalizedSourceStatus(
            primary = AppleKnowledgeSourceStatus.HISTORICAL,
            contextual = emptyList()
        )
        "ENDED" -> AppleKnowledgeNormalizedSourceStatus(
            primary = AppleKnowledgeSourceStatus.ENDED,
            contextual = emptyList()
        )
        "CURRENT + HISTORICAL" -> AppleKnowledgeNormalizedSourceStatus(
            primary = AppleKnowledgeSourceStatus.CURRENT,
            contextual = listOf(AppleKnowledgeSourceStatus.HISTORICAL)
        )
        "CURRENT + ENDED_CONTEXT" -> AppleKnowledgeNormalizedSourceStatus(
            primary = AppleKnowledgeSourceStatus.CURRENT,
            contextual = listOf(AppleKnowledgeSourceStatus.ENDED)
        )
        else -> throw IllegalArgumentException("Unsupported card source status '$raw'")
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

    private fun JsonArray.stringAt(index: Int): String =
        getOrNull(index)?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonArray.requiredStringAt(index: Int, path: String): String =
        stringAt(index).takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required string '$path'")
}

object AppleOfficialKnowledgeCardValidator {
    private val semVerRegex = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val cardIdRegex = Regex("^AOKF-[A-Z0-9]+-\\d{3}$")
    private val isoDateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val appleOfficialUrlRegex = Regex(
        "^https://(?:[A-Za-z0-9-]+\\.)*apple\\.com(?:/.*)?$",
        RegexOption.IGNORE_CASE
    )
    private val supportedSourceStatuses = setOf(
        "CURRENT",
        "HISTORICAL",
        "ENDED",
        "CURRENT + HISTORICAL",
        "CURRENT + ENDED_CONTEXT"
    )

    fun validate(parts: List<AppleOfficialKnowledgeCardSeedPart>): AppleKnowledgeDatasetValidationResult {
        val errors = mutableListOf<AppleKnowledgeDatasetIssue>()
        val warnings = mutableListOf<AppleKnowledgeDatasetIssue>()

        if (parts.isEmpty()) {
            errors += error("cards", "card seed parts must not be empty")
            return result(errors, warnings)
        }

        parts.forEachIndexed { index, part ->
            if (!semVerRegex.matches(part.datasetVersion) || part.datasetVersion != "1.0.0") {
                errors += error("parts[$index].datasetVersion", "frozen v1 card dataset must use 1.0.0")
            }
            if (part.expectedPartCount != 5) {
                errors += error("parts[$index].expectedPartCount", "card dataset must declare five parts")
            }
        }

        if (parts.map { it.part }.sorted() != listOf(1, 2, 3, 4, 5)) {
            errors += error("parts", "card dataset parts must be exactly 1 through 5")
        }

        val records = parts.flatMap { it.records }
        if (records.size != 241) {
            errors += error("cards", "frozen v1 card count must be 241, got ${records.size}")
        }

        val ids = mutableSetOf<String>()
        records.forEachIndexed { index, card ->
            val path = "cards[$index]"
            if (!cardIdRegex.matches(card.id)) {
                errors += error("$path.id", "invalid card id '${card.id}'")
            }
            if (!ids.add(card.id)) {
                errors += error("$path.id", "duplicate card id '${card.id}'")
            }
            if (!appleOfficialUrlRegex.matches(card.primarySourceUrl)) {
                errors += error("$path.primarySourceUrl", "primary source must be HTTPS on an Apple-controlled apple.com host")
            }
            if (card.sourceStatusRaw !in supportedSourceStatuses) {
                errors += error("$path.sourceStatusRaw", "unsupported source status '${card.sourceStatusRaw}'")
            }
            if (card.detailLevelRaw != "DETAILED") {
                errors += error("$path.detailLevelRaw", "frozen v1 cards must be DETAILED")
            }
            if (card.rulePackEffectRaw != "NONE") {
                errors += error("$path.rulePackEffectRaw", "Apple Official Knowledge must have Rule Pack effect NONE")
            }
            if (card.stateRaw != "PILOT_READY") {
                errors += error("$path.stateRaw", "frozen v1 cards must be PILOT_READY")
            }
            if (!isoDateRegex.matches(card.verifiedAt)) {
                errors += error("$path.verifiedAt", "verifiedAt must use YYYY-MM-DD")
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
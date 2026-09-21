package com.example.appleknowledge.data

import com.example.appleknowledge.model.AppleCapability
import com.example.appleknowledge.model.AppleCapabilityAvailability
import com.example.appleknowledge.model.AppleModelCapability
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AppleKnowledgeDatasetExpectedCounts(
    val families: Int,
    val sources: Int,
    val cards: Int
)

data class AppleOfficialKnowledgeDatasetManifest(
    val schemaVersion: Int,
    val datasetVersion: String,
    val datasetId: String,
    val importStage: String,
    val scopeLowerBound: String,
    val scopeUpperBound: String,
    val scopeFrozen: Boolean,
    val expectedCounts: AppleKnowledgeDatasetExpectedCounts
)

data class AppleOfficialKnowledgeCapabilitiesSeed(
    val manifest: AppleOfficialKnowledgeDatasetManifest,
    val modelCapabilities: List<AppleModelCapability>
)

enum class AppleKnowledgeDatasetIssueType { ERROR, WARNING }

data class AppleKnowledgeDatasetIssue(
    val type: AppleKnowledgeDatasetIssueType,
    val path: String,
    val message: String
)

data class AppleKnowledgeDatasetValidationResult(
    val isValid: Boolean,
    val errors: List<AppleKnowledgeDatasetIssue>,
    val warnings: List<AppleKnowledgeDatasetIssue>
)

object AppleOfficialKnowledgeCapabilitiesJsonParser {
    fun parse(jsonString: String): AppleOfficialKnowledgeCapabilitiesSeed {
        val root = Json.parseToJsonElement(jsonString).jsonObject
        val manifestObject = root.requiredObject("manifest")
        val expectedCountsObject = manifestObject.requiredObject("expectedCounts")

        val manifest = AppleOfficialKnowledgeDatasetManifest(
            schemaVersion = manifestObject.requiredInt("schemaVersion"),
            datasetVersion = manifestObject.requiredString("datasetVersion"),
            datasetId = manifestObject.requiredString("datasetId"),
            importStage = manifestObject.requiredString("importStage"),
            scopeLowerBound = manifestObject.requiredString("scopeLowerBound"),
            scopeUpperBound = manifestObject.requiredString("scopeUpperBound"),
            scopeFrozen = manifestObject.requiredBoolean("scopeFrozen"),
            expectedCounts = AppleKnowledgeDatasetExpectedCounts(
                families = expectedCountsObject.requiredInt("families"),
                sources = expectedCountsObject.requiredInt("sources"),
                cards = expectedCountsObject.requiredInt("cards")
            )
        )

        val capabilities = root.requiredArray("modelCapabilities").mapIndexed { index, element ->
            val capability = element as? JsonObject
                ?: throw IllegalArgumentException("modelCapabilities[$index] must be an object")

            AppleModelCapability(
                family = capability.requiredString("family"),
                exactModels = capability.requiredArray("exactModels").requiredStrings("modelCapabilities[$index].exactModels"),
                releaseYears = capability.requiredString("releaseYears"),
                publicRepairManual = capability.requiredCapability("publicRepairManual"),
                diagnosticsSsr = capability.requiredCapability("diagnosticsSsr"),
                recoveryDiagnosticsMode = capability.requiredCapability("recoveryDiagnosticsMode"),
                repairAssistant = capability.requiredCapability("repairAssistant"),
                partsServiceHistoryCapabilities = capability.array("partsServiceHistoryCapabilities").strings(),
                troubleshooting = capability.requiredCapability("troubleshooting"),
                historicalProgramSourceIds = capability.array("historicalProgramSourceIds").strings(),
                primaryOfficialUrl = capability.requiredString("primaryOfficialUrl"),
                verifiedAt = capability.requiredString("verifiedAt"),
                notes = capability.nullableString("notes")
            )
        }

        return AppleOfficialKnowledgeCapabilitiesSeed(
            manifest = manifest,
            modelCapabilities = capabilities
        )
    }

    private fun JsonObject.requiredCapability(name: String): AppleCapability {
        val capability = requiredObject(name)
        val availabilityRaw = capability.requiredString("availability")
        val availability = runCatching { AppleCapabilityAvailability.valueOf(availabilityRaw) }
            .getOrElse {
                throw IllegalArgumentException("Invalid availability '$availabilityRaw' at $name.availability")
            }
        return AppleCapability(
            availability = availability,
            notes = capability.nullableString("notes")
        )
    }

    private fun JsonObject.requiredString(name: String): String =
        this[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required string '$name'")

    private fun JsonObject.requiredInt(name: String): Int =
        this[name]?.jsonPrimitive?.intOrNull
            ?: throw IllegalArgumentException("Missing required integer '$name'")

    private fun JsonObject.requiredBoolean(name: String): Boolean =
        this[name]?.jsonPrimitive?.booleanOrNull
            ?: throw IllegalArgumentException("Missing required boolean '$name'")

    private fun JsonObject.requiredObject(name: String): JsonObject =
        this[name] as? JsonObject
            ?: throw IllegalArgumentException("Missing required object '$name'")

    private fun JsonObject.requiredArray(name: String): JsonArray =
        this[name] as? JsonArray
            ?: throw IllegalArgumentException("Missing required array '$name'")

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.nullableString(name: String): String? {
        val value = this[name] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.contentOrNull
    }

    private fun JsonArray.strings(): List<String> =
        mapNotNull { element -> element.jsonPrimitive.contentOrNull }

    private fun JsonArray.requiredStrings(path: String): List<String> {
        val result = strings()
        if (result.size != size || result.any { it.isBlank() }) {
            throw IllegalArgumentException("$path must contain only non-blank strings")
        }
        return result
    }
}

object AppleOfficialKnowledgeCapabilitiesValidator {
    private val semVerRegex = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val sourceIdRegex = Regex("^AOK-\\d{3}$")
    private val isoDateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val appleOfficialUrlRegex = Regex(
        "^https://(?:[A-Za-z0-9-]+\\.)*apple\\.com(?:/|$)",
        RegexOption.IGNORE_CASE
    )

    fun validate(seed: AppleOfficialKnowledgeCapabilitiesSeed): AppleKnowledgeDatasetValidationResult {
        val errors = mutableListOf<AppleKnowledgeDatasetIssue>()
        val warnings = mutableListOf<AppleKnowledgeDatasetIssue>()
        val manifest = seed.manifest

        if (manifest.schemaVersion != 1) {
            errors += error("manifest.schemaVersion", "schemaVersion must be 1")
        }
        if (!semVerRegex.matches(manifest.datasetVersion)) {
            errors += error("manifest.datasetVersion", "datasetVersion must use MAJOR.MINOR.PATCH")
        }
        if (manifest.datasetId != "APPLE_OFFICIAL_KNOWLEDGE_V1") {
            errors += error("manifest.datasetId", "datasetId must be APPLE_OFFICIAL_KNOWLEDGE_V1")
        }
        if (manifest.importStage != "MODEL_CAPABILITIES") {
            errors += error("manifest.importStage", "I2A seed must declare MODEL_CAPABILITIES")
        }
        if (!manifest.scopeFrozen) {
            errors += error("manifest.scopeFrozen", "v1 dataset scope must remain frozen")
        }
        if (manifest.scopeLowerBound != "iPhone 5") {
            errors += error("manifest.scopeLowerBound", "v1 lower bound must remain iPhone 5")
        }
        if (manifest.expectedCounts.families != 19) {
            errors += error("manifest.expectedCounts.families", "frozen v1 family count must be 19")
        }
        if (manifest.expectedCounts.sources != 91) {
            errors += error("manifest.expectedCounts.sources", "frozen v1 source count must be 91")
        }
        if (manifest.expectedCounts.cards != 241) {
            errors += error("manifest.expectedCounts.cards", "frozen v1 card count must be 241")
        }
        if (seed.modelCapabilities.size != manifest.expectedCounts.families) {
            errors += error(
                "modelCapabilities",
                "capability count ${seed.modelCapabilities.size} does not match expected family count ${manifest.expectedCounts.families}"
            )
        }

        val families = mutableSetOf<String>()
        val exactModels = mutableSetOf<String>()

        seed.modelCapabilities.forEachIndexed { index, capability ->
            val path = "modelCapabilities[$index]"
            if (!families.add(capability.family.lowercase())) {
                errors += error("$path.family", "duplicate family '${capability.family}'")
            }

            val localModels = mutableSetOf<String>()
            capability.exactModels.forEach { model ->
                val normalized = model.trim().lowercase()
                if (!localModels.add(normalized)) {
                    errors += error("$path.exactModels", "duplicate exact model '$model' inside family")
                }
                if (!exactModels.add(normalized)) {
                    errors += error("$path.exactModels", "exact model '$model' appears in more than one family")
                }
            }

            if (!appleOfficialUrlRegex.matches(capability.primaryOfficialUrl)) {
                errors += error("$path.primaryOfficialUrl", "URL must be HTTPS on an Apple-controlled apple.com host")
            }
            if (!isoDateRegex.matches(capability.verifiedAt)) {
                errors += error("$path.verifiedAt", "verifiedAt must use YYYY-MM-DD")
            }
            capability.historicalProgramSourceIds.forEach { sourceId ->
                if (!sourceIdRegex.matches(sourceId)) {
                    errors += error("$path.historicalProgramSourceIds", "invalid AOK source id '$sourceId'")
                }
            }
            if (capability.partsServiceHistoryCapabilities.size != capability.partsServiceHistoryCapabilities.distinct().size) {
                errors += error("$path.partsServiceHistoryCapabilities", "duplicate Parts & Service History capability")
            }
        }

        return AppleKnowledgeDatasetValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun error(path: String, message: String) = AppleKnowledgeDatasetIssue(
        type = AppleKnowledgeDatasetIssueType.ERROR,
        path = path,
        message = message
    )
}
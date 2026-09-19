package com.example.rulepack

import com.example.domain.model.BitmaskPolicy
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DeviceModel
import com.example.domain.model.DeviceScope
import com.example.domain.model.DiagnosisDefinition
import com.example.domain.model.DiagnosticRule
import com.example.domain.model.PanicClassifier
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedRulePack
import com.example.domain.model.RepairFlow
import com.example.domain.model.RuleDiffItem
import com.example.domain.model.RulePackDiffSummary
import com.example.domain.model.RulePackOrigin
import com.example.domain.model.RulePackSource
import com.example.domain.model.RulePackValidationResult
import com.example.domain.model.SuspectedComponent
import com.example.domain.model.ValidationIssue
import com.example.domain.model.VerificationStatus
import com.example.platform.Sha256Hasher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object RulePackJsonParser {
    fun parse(
        jsonString: String,
        origin: RulePackOrigin = RulePackOrigin.USER_IMPORTED,
        hasher: Sha256Hasher
    ): ParsedRulePack {
        val root = Json.parseToJsonElement(jsonString).jsonObject
        val schemaVersion = root.int("schemaVersion", 1)
        val knowledgeBaseVersion = root.string("knowledgeBaseVersion", "1.0.0")
        val title = root.string("title", "PanicLab Rule Pack")
        val generatedAt = root.string("generatedAt", "")
        val locale = root.string("locale", "es-UY")

        val sources = root.array("sources").map { element ->
            val obj = element.jsonObject
            RulePackSource(
                id = obj.string("id"),
                title = obj.string("title"),
                publisher = obj.string("publisher"),
                url = obj.string("url"),
                checkedAt = obj.string("checkedAt"),
                trustLevel = obj.string("trustLevel", "COMMUNITY"),
                supports = obj.array("supports").strings()
            )
        }

        val deviceModels = root.array("deviceModels").map { element ->
            val obj = element.jsonObject
            DeviceModel(
                productCode = obj.requiredString("productCode"),
                marketingName = obj.requiredString("marketingName"),
                family = obj.requiredString("family"),
                variant = obj.string("variant"),
                diagnosticProfile = obj.requiredString("diagnosticProfile"),
                releaseYear = obj.int("releaseYear", 2020),
                sourceIds = obj.array("sourceIds").strings()
            )
        }

        val panicClassifiers = root.array("panicClassifiers").map { element ->
            val obj = element.jsonObject
            val match = obj.obj("match")
            PanicClassifier(
                id = obj.string("id"),
                family = obj.string("family"),
                priority = obj.int("priority", 100),
                anyTerms = match.array("anyTerms").strings(),
                allTerms = match.array("allTerms").strings(),
                regexAny = match.array("regexAny").strings(),
                notRegex = match.array("notRegex").strings(),
                notes = obj.nullableString("notes")
            )
        }

        val bitmaskPolicies = root.array("bitmaskPolicies").map { element ->
            val obj = element.jsonObject
            BitmaskPolicy(
                diagnosticProfile = obj.string("diagnosticProfile"),
                enabled = obj.boolean("enabled", false),
                knownBits = obj.array("knownBits").strings(),
                exactRulesAlwaysWin = obj.boolean("exactRulesAlwaysWin", true),
                notes = obj.nullableString("notes")
            )
        }

        val diagnosticRules = root.array("diagnosticRules").map { element ->
            val obj = element.jsonObject
            val deviceScope = obj.obj("deviceScope")
            val match = obj.obj("match")
            val diagnosis = obj.requiredObject("diagnosis")
            val repairFlow = obj.obj("repairFlow")

            val panicFamilies = match.array("panicFamiliesAny").strings().mapNotNull { raw ->
                runCatching { PanicFamily.valueOf(raw) }.getOrNull()
            }
            val suspectedComponents = diagnosis.array("suspectedComponents").map { suspected ->
                val component = suspected.jsonObject
                SuspectedComponent(
                    name = component.requiredString("name"),
                    role = component.string("role", "PRIMARY")
                )
            }

            DiagnosticRule(
                id = obj.requiredString("id"),
                title = obj.requiredString("title"),
                active = obj.boolean("active", true),
                priority = obj.int("priority", 100),
                deviceScope = DeviceScope(
                    diagnosticProfiles = deviceScope.array("diagnosticProfiles").strings(),
                    productCodes = deviceScope.array("productCodes").strings()
                ),
                panicFamilies = panicFamilies,
                sensorTokens = match.array("sensorTokensAny").strings(),
                sensorCodesExact = match.array("sensorCodesExactAny").strings(),
                requiredTermsAll = match.array("requiredTermsAll").strings(),
                rawTermsAny = match.array("rawTermsAny").strings(),
                diagnosis = DiagnosisDefinition(
                    label = diagnosis.requiredString("label"),
                    subsystem = diagnosis.string("subsystem", "GENERAL"),
                    suspectedComponents = suspectedComponents,
                    interpretation = diagnosis.string("interpretation")
                ),
                confidence = enumOrDefault(
                    raw = obj.string("confidence", "UNKNOWN"),
                    default = ConfidenceLevel.UNKNOWN,
                    parser = ConfidenceLevel::valueOf
                ),
                verificationStatus = enumOrDefault(
                    raw = obj.string("verificationStatus", "UNKNOWN"),
                    default = VerificationStatus.UNKNOWN,
                    parser = VerificationStatus::valueOf
                ),
                primaryEligible = obj.boolean("primaryEligible", true),
                exactCodeOnly = obj.boolean("exactCodeOnly", true),
                allowBitmaskDecomposition = obj.boolean("allowBitmaskDecomposition", false),
                repairFlow = RepairFlow(
                    firstChecks = repairFlow.array("firstChecks").strings(),
                    knownGoodTest = repairFlow.nullableString("knownGoodTest"),
                    boardLevelNextSteps = repairFlow.array("boardLevelNextSteps").strings(),
                    cautions = repairFlow.array("cautions").strings()
                ),
                sourceIds = obj.array("sourceIds").strings(),
                notes = obj.nullableString("notes"),
                version = knowledgeBaseVersion
            )
        }

        return ParsedRulePack(
            schemaVersion = schemaVersion,
            knowledgeBaseVersion = knowledgeBaseVersion,
            title = title,
            generatedAt = generatedAt,
            locale = locale,
            sources = sources,
            deviceModels = deviceModels,
            panicClassifiers = panicClassifiers,
            bitmaskPolicies = bitmaskPolicies,
            diagnosticRules = diagnosticRules,
            checksum = hasher.sha256(jsonString),
            origin = origin,
            rawJson = jsonString
        )
    }

    private fun JsonObject.string(name: String, default: String = ""): String =
        this[name]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.requiredString(name: String): String =
        this[name]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("Missing required string '$name'")

    private fun JsonObject.nullableString(name: String): String? {
        val element = this[name] ?: return null
        if (element is JsonNull) return null
        return element.jsonPrimitive.contentOrNull
    }

    private fun JsonObject.int(name: String, default: Int): Int =
        this[name]?.jsonPrimitive?.intOrNull ?: default

    private fun JsonObject.boolean(name: String, default: Boolean): Boolean =
        this[name]?.jsonPrimitive?.booleanOrNull ?: default

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.obj(name: String): JsonObject =
        this[name] as? JsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.requiredObject(name: String): JsonObject =
        this[name] as? JsonObject
            ?: throw IllegalArgumentException("Missing required object '$name'")

    private fun JsonArray.strings(): List<String> =
        mapNotNull { element -> (element as? JsonPrimitive)?.contentOrNull }

    private fun <T> enumOrDefault(raw: String, default: T, parser: (String) -> T): T =
        runCatching { parser(raw) }.getOrDefault(default)
}

object RulePackValidator {
    private val semVerRegex = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val productCodeRegex = Regex("^iPhone\\d+,\\d+$")

    fun validate(parsedPack: ParsedRulePack): RulePackValidationResult {
        val errors = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<ValidationIssue>()

        if (parsedPack.schemaVersion != 1) {
            errors += error(
                "schemaVersion",
                "Schema version ${parsedPack.schemaVersion} no es compatible. Se requiere schemaVersion = 1."
            )
        }

        if (!semVerRegex.matches(parsedPack.knowledgeBaseVersion)) {
            errors += error(
                "knowledgeBaseVersion",
                "knowledgeBaseVersion '${parsedPack.knowledgeBaseVersion}' debe seguir el formato semántico MAJOR.MINOR.PATCH (ej. 1.1.0)."
            )
        }

        val sourceIds = mutableSetOf<String>()
        parsedPack.sources.forEachIndexed { index, source ->
            if (source.id.isBlank()) {
                errors += error("sources[$index].id", "El ID de la fuente no puede estar vacío.")
            } else if (!sourceIds.add(source.id)) {
                errors += error("sources[$index].id", "ID de fuente duplicado: '${source.id}'.")
            }

            if (
                source.url.isNotBlank() &&
                !source.url.startsWith("http://") &&
                !source.url.startsWith("https://")
            ) {
                warnings += warning(
                    "sources[$index].url",
                    "URL de fuente potencialmente inválida: '${source.url}'."
                )
            }
        }

        val productCodes = mutableSetOf<String>()
        parsedPack.deviceModels.forEachIndexed { index, model ->
            if (!productCodeRegex.matches(model.productCode)) {
                errors += error(
                    "deviceModels[$index].productCode",
                    "productCode '${model.productCode}' inválido. Debe cumplir el patrón 'iPhoneXX,X'."
                )
            } else if (!productCodes.add(model.productCode)) {
                errors += error(
                    "deviceModels[$index].productCode",
                    "productCode duplicado: '${model.productCode}'."
                )
            }

            if (model.marketingName.isBlank()) {
                errors += error(
                    "deviceModels[$index].marketingName",
                    "marketingName no puede estar vacío para ${model.productCode}."
                )
            }

            model.sourceIds.forEach { sourceId ->
                if (!sourceIds.contains(sourceId)) {
                    warnings += warning(
                        "deviceModels[$index].sourceIds",
                        "Modelo ${model.productCode} referencia fuente inexistente: '$sourceId'."
                    )
                }
            }
        }

        val classifierIds = mutableSetOf<String>()
        parsedPack.panicClassifiers.forEachIndexed { index, classifier ->
            if (classifier.id.isBlank()) {
                errors += error(
                    "panicClassifiers[$index].id",
                    "ID de clasificador no puede estar vacío."
                )
            } else if (!classifierIds.add(classifier.id)) {
                errors += error(
                    "panicClassifiers[$index].id",
                    "ID de clasificador duplicado: '${classifier.id}'."
                )
            }

            val isRecognizedFamily = runCatching {
                PanicFamily.valueOf(classifier.family)
            }.isSuccess
            if (!isRecognizedFamily && classifier.family != "UNKNOWN") {
                warnings += warning(
                    "panicClassifiers[$index].family",
                    "Familia no estándar en clasificador: '${classifier.family}'."
                )
            }

            classifier.regexAny.forEach { regex ->
                try {
                    Regex(regex)
                } catch (exception: IllegalArgumentException) {
                    errors += error(
                        "panicClassifiers[$index].match.regexAny",
                        "Expresión regular inválida '$regex': ${exception.message}"
                    )
                }
            }

            classifier.notRegex.forEach { regex ->
                try {
                    Regex(regex)
                } catch (exception: IllegalArgumentException) {
                    errors += error(
                        "panicClassifiers[$index].match.notRegex",
                        "Expresión regular inválida '$regex': ${exception.message}"
                    )
                }
            }
        }

        val ruleIds = mutableSetOf<String>()
        parsedPack.diagnosticRules.forEachIndexed { index, rule ->
            if (rule.id.isBlank()) {
                errors += error("diagnosticRules[$index].id", "ID de regla no puede estar vacío.")
            } else if (!ruleIds.add(rule.id)) {
                errors += error(
                    "diagnosticRules[$index].id",
                    "ID de regla duplicado: '${rule.id}'."
                )
            }

            if (rule.title.isBlank()) {
                errors += error(
                    "diagnosticRules[$index].title",
                    "Título de regla ${rule.id} no puede estar vacío."
                )
            }

            if (rule.diagnosis.label.isBlank()) {
                errors += error(
                    "diagnosticRules[$index].diagnosis.label",
                    "Diagnóstico label no puede estar vacío en ${rule.id}."
                )
            }

            rule.sensorCodesExact.forEach { code ->
                val isHex = code.startsWith("0x", ignoreCase = true)
                val isValid = if (isHex) {
                    code.substring(2).toLongOrNull(16) != null
                } else {
                    code.toLongOrNull() != null
                }
                if (!isValid) {
                    errors += error(
                        "diagnosticRules[$index].match.sensorCodesExactAny",
                        "Código numérico/hexadecimal inválido '$code' en regla ${rule.id}."
                    )
                }
            }

            rule.sourceIds.forEach { sourceId ->
                if (!sourceIds.contains(sourceId)) {
                    errors += error(
                        "diagnosticRules[$index].sourceIds",
                        "Regla ${rule.id} referencia sourceId inexistente en el Rule Pack: '$sourceId'."
                    )
                }
            }

            when (rule.verificationStatus) {
                VerificationStatus.CONFLICTING_SOURCE -> warnings += warning(
                    "diagnosticRules[$index]",
                    "Regla ${rule.id} marcada con estado CONFLICTING_SOURCE."
                )

                VerificationStatus.COMMUNITY_SUPPORTED -> warnings += warning(
                    "diagnosticRules[$index]",
                    "Regla ${rule.id} respaldada por comunidad (COMMUNITY_SUPPORTED)."
                )

                VerificationStatus.EXPERIMENTAL -> warnings += warning(
                    "diagnosticRules[$index]",
                    "Regla ${rule.id} de estado EXPERIMENTAL."
                )

                else -> Unit
            }

            if (rule.confidence == ConfidenceLevel.LOW && rule.primaryEligible) {
                warnings += warning(
                    "diagnosticRules[$index].confidence",
                    "Regla ${rule.id} con LOW confidence está marcada como primaryEligible."
                )
            }
        }

        return RulePackValidationResult(
            isValid = errors.isEmpty(),
            schemaVersion = parsedPack.schemaVersion,
            knowledgeBaseVersion = parsedPack.knowledgeBaseVersion,
            rulesCount = parsedPack.diagnosticRules.size,
            modelsCount = parsedPack.deviceModels.size,
            classifiersCount = parsedPack.panicClassifiers.size,
            sourcesCount = parsedPack.sources.size,
            bitmaskCount = parsedPack.bitmaskPolicies.size,
            errors = errors,
            warnings = warnings,
            checksum = parsedPack.checksum
        )
    }

    private fun error(path: String, message: String) = ValidationIssue(
        type = ValidationIssue.IssueType.ERROR,
        path = path,
        message = message
    )

    private fun warning(path: String, message: String) = ValidationIssue(
        type = ValidationIssue.IssueType.WARNING,
        path = path,
        message = message
    )
}

object RulePackDiffCalculator {
    fun calculateDiff(
        currentPack: ParsedRulePack?,
        incomingPack: ParsedRulePack
    ): RulePackDiffSummary {
        if (currentPack == null) {
            return RulePackDiffSummary(
                currentVersion = "Ninguna",
                incomingVersion = incomingPack.knowledgeBaseVersion,
                addedRulesCount = incomingPack.diagnosticRules.size,
                modifiedRulesCount = 0,
                deactivatedRulesCount = 0,
                removedRulesCount = 0,
                addedModelsCount = incomingPack.deviceModels.size,
                modifiedModelsCount = 0,
                addedSourcesCount = incomingPack.sources.size,
                addedClassifiersCount = incomingPack.panicClassifiers.size,
                ruleDiffs = incomingPack.diagnosticRules.map { rule ->
                    RuleDiffItem(
                        ruleId = rule.id,
                        title = rule.title,
                        changeType = RuleDiffItem.ChangeType.ADDED,
                        previousSummary = null,
                        newSummary = "${rule.confidence} | ${rule.verificationStatus} | ${rule.diagnosis.label}",
                        confidenceChange = null,
                        statusChange = null,
                        details = listOf("Nueva regla en la base de conocimiento.")
                    )
                }
            )
        }

        val currentRuleMap = currentPack.diagnosticRules.associateBy { it.id }
        val incomingRuleMap = incomingPack.diagnosticRules.associateBy { it.id }
        val diffItems = mutableListOf<RuleDiffItem>()
        var addedRules = 0
        var modifiedRules = 0
        var deactivatedRules = 0
        var removedRules = 0

        incomingPack.diagnosticRules.forEach { newRule ->
            val oldRule = currentRuleMap[newRule.id]
            if (oldRule == null) {
                addedRules++
                diffItems += RuleDiffItem(
                    ruleId = newRule.id,
                    title = newRule.title,
                    changeType = RuleDiffItem.ChangeType.ADDED,
                    previousSummary = null,
                    newSummary = "${newRule.confidence} | ${newRule.verificationStatus} | ${newRule.diagnosis.label}",
                    confidenceChange = null,
                    statusChange = null,
                    details = listOf("Nueva regla añadida.")
                )
            } else {
                val details = mutableListOf<String>()
                var confidenceChange: String? = null
                var statusChange: String? = null

                if (oldRule.active && !newRule.active) {
                    deactivatedRules++
                    details += "Regla desactivada."
                }
                if (oldRule.confidence != newRule.confidence) {
                    confidenceChange = "${oldRule.confidence} → ${newRule.confidence}"
                    details += "Confianza: $confidenceChange"
                }
                if (oldRule.verificationStatus != newRule.verificationStatus) {
                    statusChange = "${oldRule.verificationStatus} → ${newRule.verificationStatus}"
                    details += "Estado de verificación: $statusChange"
                }
                if (oldRule.diagnosis.label != newRule.diagnosis.label) {
                    details += "Diagnóstico: '${oldRule.diagnosis.label}' → '${newRule.diagnosis.label}'"
                }
                if (oldRule.priority != newRule.priority) {
                    details += "Prioridad: ${oldRule.priority} → ${newRule.priority}"
                }
                if (oldRule.primaryEligible != newRule.primaryEligible) {
                    details += "Elegible como principal: ${oldRule.primaryEligible} → ${newRule.primaryEligible}"
                }

                if (details.isNotEmpty()) {
                    modifiedRules++
                    diffItems += RuleDiffItem(
                        ruleId = newRule.id,
                        title = newRule.title,
                        changeType = if (!newRule.active && oldRule.active) {
                            RuleDiffItem.ChangeType.DEACTIVATED
                        } else {
                            RuleDiffItem.ChangeType.MODIFIED
                        },
                        previousSummary = "${oldRule.confidence} | ${oldRule.verificationStatus}",
                        newSummary = "${newRule.confidence} | ${newRule.verificationStatus}",
                        confidenceChange = confidenceChange,
                        statusChange = statusChange,
                        details = details
                    )
                }
            }
        }

        currentPack.diagnosticRules.forEach { oldRule ->
            if (!incomingRuleMap.containsKey(oldRule.id)) {
                removedRules++
                diffItems += RuleDiffItem(
                    ruleId = oldRule.id,
                    title = oldRule.title,
                    changeType = RuleDiffItem.ChangeType.REMOVED,
                    previousSummary = "${oldRule.confidence} | ${oldRule.verificationStatus} | ${oldRule.diagnosis.label}",
                    newSummary = null,
                    confidenceChange = null,
                    statusChange = null,
                    details = listOf("Regla eliminada del Rule Pack.")
                )
            }
        }

        val currentModels = currentPack.deviceModels.associateBy { it.productCode }
        val addedModels = incomingPack.deviceModels.count { it.productCode !in currentModels }
        val modifiedModels = incomingPack.deviceModels.count { incoming ->
            val current = currentModels[incoming.productCode]
            current != null && (
                current.marketingName != incoming.marketingName ||
                    current.diagnosticProfile != incoming.diagnosticProfile
                )
        }

        val currentSourceIds = currentPack.sources.map { it.id }.toSet()
        val addedSources = incomingPack.sources.count { it.id !in currentSourceIds }
        val currentClassifierIds = currentPack.panicClassifiers.map { it.id }.toSet()
        val addedClassifiers = incomingPack.panicClassifiers.count { it.id !in currentClassifierIds }

        return RulePackDiffSummary(
            currentVersion = currentPack.knowledgeBaseVersion,
            incomingVersion = incomingPack.knowledgeBaseVersion,
            addedRulesCount = addedRules,
            modifiedRulesCount = modifiedRules,
            deactivatedRulesCount = deactivatedRules,
            removedRulesCount = removedRules,
            addedModelsCount = addedModels,
            modifiedModelsCount = modifiedModels,
            addedSourcesCount = addedSources,
            addedClassifiersCount = addedClassifiers,
            ruleDiffs = diffItems
        )
    }
}

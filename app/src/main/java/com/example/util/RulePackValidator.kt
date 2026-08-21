package com.example.util

import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedRulePack
import com.example.domain.model.RulePackValidationResult
import com.example.domain.model.ValidationIssue
import com.example.domain.model.VerificationStatus
import java.util.regex.Pattern

object RulePackValidator {

    private val SEMVER_REGEX = Pattern.compile("^\\d+\\.\\d+\\.\\d+$")
    private val PRODUCT_CODE_REGEX = Pattern.compile("^iPhone\\d+,\\d+$")

    fun validate(parsedPack: ParsedRulePack): RulePackValidationResult {
        val errors = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<ValidationIssue>()

        // 1. Schema Version Check
        if (parsedPack.schemaVersion != 1) {
            errors.add(
                ValidationIssue(
                    type = ValidationIssue.IssueType.ERROR,
                    path = "schemaVersion",
                    message = "Schema version ${parsedPack.schemaVersion} no es compatible. Se requiere schemaVersion = 1."
                )
            )
        }

        // 2. SemVer Check
        if (!SEMVER_REGEX.matcher(parsedPack.knowledgeBaseVersion).matches()) {
            errors.add(
                ValidationIssue(
                    type = ValidationIssue.IssueType.ERROR,
                    path = "knowledgeBaseVersion",
                    message = "knowledgeBaseVersion '${parsedPack.knowledgeBaseVersion}' debe seguir el formato semántico MAJOR.MINOR.PATCH (ej. 1.1.0)."
                )
            )
        }

        // 3. Source Uniqueness & Validation
        val sourceIds = mutableSetOf<String>()
        parsedPack.sources.forEachIndexed { idx, src ->
            if (src.id.isBlank()) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "sources[$idx].id",
                        message = "El ID de la fuente no puede estar vacío."
                    )
                )
            } else if (!sourceIds.add(src.id)) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "sources[$idx].id",
                        message = "ID de fuente duplicado: '${src.id}'."
                    )
                )
            }

            if (src.url.isNotBlank() && !src.url.startsWith("http://") && !src.url.startsWith("https://")) {
                warnings.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.WARNING,
                        path = "sources[$idx].url",
                        message = "URL de fuente potencialmente inválida: '${src.url}'."
                    )
                )
            }
        }

        // 4. Device Models Validation
        val productCodes = mutableSetOf<String>()
        parsedPack.deviceModels.forEachIndexed { idx, model ->
            if (!PRODUCT_CODE_REGEX.matcher(model.productCode).matches()) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "deviceModels[$idx].productCode",
                        message = "productCode '${model.productCode}' inválido. Debe cumplir el patrón 'iPhoneXX,X'."
                    )
                )
            } else if (!productCodes.add(model.productCode)) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "deviceModels[$idx].productCode",
                        message = "productCode duplicado: '${model.productCode}'."
                    )
                )
            }

            if (model.marketingName.isBlank()) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "deviceModels[$idx].marketingName",
                        message = "marketingName no puede estar vacío para ${model.productCode}."
                    )
                )
            }

            // Check source references
            model.sourceIds.forEach { sId ->
                if (!sourceIds.contains(sId)) {
                    warnings.add(
                        ValidationIssue(
                            type = ValidationIssue.IssueType.WARNING,
                            path = "deviceModels[$idx].sourceIds",
                            message = "Modelo ${model.productCode} referencia fuente inexistente: '$sId'."
                        )
                    )
                }
            }
        }

        // 5. Panic Classifiers Validation
        val classifierIds = mutableSetOf<String>()
        parsedPack.panicClassifiers.forEachIndexed { idx, classifier ->
            if (classifier.id.isBlank()) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "panicClassifiers[$idx].id",
                        message = "ID de clasificador no puede estar vacío."
                    )
                )
            } else if (!classifierIds.add(classifier.id)) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "panicClassifiers[$idx].id",
                        message = "ID de clasificador duplicado: '${classifier.id}'."
                    )
                )
            }

            // Check family validity
            val isRecognizedFamily = try {
                PanicFamily.valueOf(classifier.family)
                true
            } catch (e: Exception) {
                false
            }
            if (!isRecognizedFamily && classifier.family != "UNKNOWN") {
                warnings.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.WARNING,
                        path = "panicClassifiers[$idx].family",
                        message = "Familia no estándar en clasificador: '${classifier.family}'."
                    )
                )
            }

            // Compile regexes
            classifier.regexAny.forEach { reg ->
                try {
                    Pattern.compile(reg)
                } catch (e: Exception) {
                    errors.add(
                        ValidationIssue(
                            type = ValidationIssue.IssueType.ERROR,
                            path = "panicClassifiers[$idx].match.regexAny",
                            message = "Expresión regular inválida '$reg': ${e.message}"
                        )
                    )
                }
            }

            classifier.notRegex.forEach { reg ->
                try {
                    Pattern.compile(reg)
                } catch (e: Exception) {
                    errors.add(
                        ValidationIssue(
                            type = ValidationIssue.IssueType.ERROR,
                            path = "panicClassifiers[$idx].match.notRegex",
                            message = "Expresión regular inválida '$reg': ${e.message}"
                        )
                    )
                }
            }
        }

        // 6. Diagnostic Rules Validation
        val ruleIds = mutableSetOf<String>()
        parsedPack.diagnosticRules.forEachIndexed { idx, rule ->
            if (rule.id.isBlank()) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "diagnosticRules[$idx].id",
                        message = "ID de regla no puede estar vacío."
                    )
                )
            } else if (!ruleIds.add(rule.id)) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "diagnosticRules[$idx].id",
                        message = "ID de regla duplicado: '${rule.id}'."
                    )
                )
            }

            if (rule.title.isBlank()) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "diagnosticRules[$idx].title",
                        message = "Título de regla ${rule.id} no puede estar vacío."
                    )
                )
            }

            if (rule.diagnosis.label.isBlank()) {
                errors.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "diagnosticRules[$idx].diagnosis.label",
                        message = "Diagnóstico label no puede estar vacío en ${rule.id}."
                    )
                )
            }

            // Validate code formats (numeric or hex)
            rule.sensorCodesExact.forEach { code ->
                val isHex = code.startsWith("0x", ignoreCase = true)
                val isValidNumber = if (isHex) {
                    try { code.substring(2).toLong(16); true } catch (e: Exception) { false }
                } else {
                    try { code.toLong(); true } catch (e: Exception) { false }
                }
                if (!isValidNumber) {
                    errors.add(
                        ValidationIssue(
                            type = ValidationIssue.IssueType.ERROR,
                            path = "diagnosticRules[$idx].match.sensorCodesExactAny",
                            message = "Código numérico/hexadecimal inválido '$code' en regla ${rule.id}."
                        )
                    )
                }
            }

            // Validate source references
            rule.sourceIds.forEach { sId ->
                if (!sourceIds.contains(sId)) {
                    errors.add(
                        ValidationIssue(
                            type = ValidationIssue.IssueType.ERROR,
                            path = "diagnosticRules[$idx].sourceIds",
                            message = "Regla ${rule.id} referencia sourceId inexistente en el Rule Pack: '$sId'."
                        )
                    )
                }
            }

            // Check for warnings regarding verification status
            if (rule.verificationStatus == VerificationStatus.CONFLICTING_SOURCE) {
                warnings.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.WARNING,
                        path = "diagnosticRules[$idx]",
                        message = "Regla ${rule.id} marcada con estado CONFLICTING_SOURCE."
                    )
                )
            } else if (rule.verificationStatus == VerificationStatus.COMMUNITY_SUPPORTED) {
                warnings.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.WARNING,
                        path = "diagnosticRules[$idx]",
                        message = "Regla ${rule.id} respaldada por comunidad (COMMUNITY_SUPPORTED)."
                    )
                )
            } else if (rule.verificationStatus == VerificationStatus.EXPERIMENTAL) {
                warnings.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.WARNING,
                        path = "diagnosticRules[$idx]",
                        message = "Regla ${rule.id} de estado EXPERIMENTAL."
                    )
                )
            }

            // Low confidence as primary check warning
            if (rule.confidence == ConfidenceLevel.LOW && rule.primaryEligible) {
                warnings.add(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.WARNING,
                        path = "diagnosticRules[$idx].confidence",
                        message = "Regla ${rule.id} con LOW confidence está marcada como primaryEligible."
                    )
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
}

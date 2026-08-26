package com.example

import com.example.domain.model.*
import com.example.export.TextSummaryOptions
import com.example.export.TextSummaryReportGenerator
import org.junit.Assert.*
import org.junit.Test

class TextSummaryReportGeneratorTest {

    private fun createTestReport(): DiagnosticReport {
        return DiagnosticReport(
            id = "test-session-123",
            createdAt = 1711000000000L,
            sourceFilename = "panic-full-2024-03-21.ips",
            deviceModel = DeviceModel(
                productCode = "iPhone14,7",
                marketingName = "iPhone 14",
                family = "iPhone",
                variant = "Standard",
                diagnosticProfile = "SMC_14_BASE",
                releaseYear = 2022
            ),
            productCode = "iPhone14,7",
            osVersion = "17.3.1",
            build = "21D61",
            panicFamilies = listOf(PanicFamily.SMC_BSC_FAILURE),
            panicStringSummary = "SMC PANIC - BSC failure at address 0x400000",
            evidences = listOf(
                DiagnosticEvidence(
                    id = "ev-1",
                    type = "SMC_CODE",
                    title = "Sensor Array SMC",
                    rawValue = "4194304",
                    normalizedValue = "0x400000 (Decimal: 4194304)",
                    excerpt = "S.sensor array 0 - 5 is 0, 4194304, 0, 0, 0",
                    lineNumber = 42
                )
            ),
            primaryCandidate = DiagnosisCandidate(
                ruleId = "rule-smc-14-wireless-coil",
                label = "Bobina de Carga Inalámbrica / NFC",
                subsystem = "SMC_SENSOR",
                suspectedComponents = listOf(
                    SuspectedComponent(name = "Flex Bobina Carga Inalámbrica", role = "PRIMARY")
                ),
                interpretation = "Fallo de comunicación del sensor de temperatura en bobina de carga inalámbrica.",
                confidence = ConfidenceLevel.HIGH,
                verificationStatus = VerificationStatus.VERIFIED,
                isPrimary = true,
                repairFlow = RepairFlow(
                    firstChecks = listOf("Desconectar flex de bobina inalámbrica", "Inspeccionar conector FPC"),
                    knownGoodTest = "Probar con flex de carga inalámbrica original conocido en buen estado"
                )
            ),
            alternativeCandidates = emptyList(),
            confidence = ConfidenceLevel.HIGH,
            verificationStatus = VerificationStatus.VERIFIED,
            repairFlow = RepairFlow(
                firstChecks = listOf("Desconectar flex de bobina inalámbrica", "Inspeccionar conector FPC"),
                knownGoodTest = "Probar con flex de carga inalámbrica original conocido en buen estado"
            ),
            knowledgeBaseVersion = "1.0.0"
        )
    }

    @Test
    fun testGenerateCustomerSummary_containsAllKeySections() {
        val report = createTestReport()
        val options = TextSummaryOptions(
            customerName = "Carlos Mendoza",
            technicianOrShopName = "iFixLab Pro",
            customNotes = "Se recomienda sustitución de flex original.",
            includeRepairSteps = true,
            includeTechnicalEvidences = true
        )

        val summary = TextSummaryReportGenerator.generateCustomerSummary(report, options)

        // Check header and customer data
        assertTrue(summary.contains("PANICLAB - INFORME TÉCNICO DE DIAGNÓSTICO"))
        assertTrue(summary.contains("iFixLab Pro"))
        assertTrue(summary.contains("Carlos Mendoza"))

        // Check device info
        assertTrue(summary.contains("iPhone 14"))
        assertTrue(summary.contains("iPhone14,7"))
        assertTrue(summary.contains("iOS 17.3.1 (21D61)"))

        // Check verdict and components
        assertTrue(summary.contains("Bobina de Carga Inalámbrica / NFC"))
        assertTrue(summary.contains("Flex Bobina Carga Inalámbrica"))
        assertTrue(summary.contains("Principal causante"))

        // Check evidences
        assertTrue(summary.contains("0x400000 (Decimal: 4194304)"))
        assertTrue(summary.contains("S.sensor array 0 - 5 is 0, 4194304, 0, 0, 0"))

        // Check repair steps
        assertTrue(summary.contains("Desconectar flex de bobina inalámbrica"))
        assertTrue(summary.contains("Probar con flex de carga inalámbrica original"))

        // Check custom notes
        assertTrue(summary.contains("Se recomienda sustitución de flex original."))
    }

    @Test
    fun testGenerateCompactSummary() {
        val report = createTestReport()
        val compact = TextSummaryReportGenerator.generateCompactSummary(report)

        assertTrue(compact.contains("Diagnóstico PanicLab: iPhone 14"))
        assertTrue(compact.contains("Bobina de Carga Inalámbrica / NFC"))
        assertTrue(compact.contains("Flex Bobina Carga Inalámbrica"))
    }
}

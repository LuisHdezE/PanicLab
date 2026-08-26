package com.example

import com.example.data.remote.GeminiRepairGroundingService
import com.example.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepairGroundingServiceTest {

    private lateinit var service: GeminiRepairGroundingService

    @Before
    fun setUp() {
        service = GeminiRepairGroundingService()
    }

    private fun createSampleReport(
        productCode: String = "iPhone14,4",
        modelName: String = "iPhone 13 mini",
        diagnosisLabel: String = "Micrófono 2 / Flex de Carga",
        ruleId: String = "rule_mic2_0x1000"
    ): DiagnosticReport {
        return DiagnosticReport(
            id = "test_session_123",
            createdAt = System.currentTimeMillis(),
            sourceFilename = "panic_sample.ips",
            deviceModel = DeviceModel(
                productCode = productCode,
                marketingName = modelName,
                family = "iPhone",
                variant = "mini",
                diagnosticProfile = "IPHONE_13",
                releaseYear = 2021
            ),
            productCode = productCode,
            osVersion = "17.4",
            build = "21E236",
            panicFamilies = listOf(PanicFamily.SMC_BSC_FAILURE),
            panicStringSummary = "SMC PANIC - BSC failure at address 0x1000 - S.sensor array 0 - 6 is 0x0, 0x1000, 0x0, 0x0, 0x0, 0x0, 0x0",
            evidences = listOf(
                DiagnosticEvidence(
                    id = "ev_1",
                    type = "SMC_CODE",
                    title = "Sensor array 0x1000",
                    rawValue = "0x1000",
                    normalizedValue = "0x1000",
                    excerpt = "S.sensor array 0 - 6 is 0x0, 0x1000, 0x0, 0x0, 0x0, 0x0, 0x0",
                    lineNumber = 61
                )
            ),
            primaryCandidate = DiagnosisCandidate(
                ruleId = ruleId,
                label = diagnosisLabel,
                subsystem = "AUDIO_CHARGING",
                suspectedComponents = listOf(
                    SuspectedComponent("Flex de Carga / Mic 2", "PRIMARY"),
                    SuspectedComponent("Línea I2C3", "SECONDARY")
                ),
                interpretation = "Fallo en el bus de comunicación del sensor de micrófono inferior (mic2).",
                confidence = ConfidenceLevel.HIGH,
                verificationStatus = VerificationStatus.VERIFIED,
                isPrimary = true
            ),
            alternativeCandidates = emptyList(),
            confidence = ConfidenceLevel.HIGH,
            verificationStatus = VerificationStatus.VERIFIED,
            repairFlow = RepairFlow(
                firstChecks = listOf(
                    "Desconectar flex de carga y verificar si persiste reinicio a los 3 minutos",
                    "Probar con flex de carga conocido original"
                ),
                knownGoodTest = "Reemplazo temporal con módulo de carga OEM"
            ),
            knowledgeBaseVersion = "1.0.0"
        )
    }

    @Test
    fun testOfflineFallbackYieldsRichMicroSolderingAndDiodeModeSteps() = runBlocking {
        val report = createSampleReport()
        val result = service.fetchRepairSuggestions(report)

        assertTrue(result.isSuccess)
        val suggestion = result.getOrThrow()

        assertNotNull(suggestion)
        assertTrue(suggestion.detailedSteps.isNotEmpty())
        assertTrue(suggestion.suspectedComponents.isNotEmpty())
        assertTrue(suggestion.diodeModeReferenceTips.isNotEmpty())
        assertTrue(suggestion.cautions.isNotEmpty())
        assertTrue(suggestion.searchSources.isNotEmpty())

        // Verify specific repair content for mic2 / flex dock
        val hasFlexStep = suggestion.detailedSteps.any { it.contains("flex", ignoreCase = true) || it.contains("carga", ignoreCase = true) }
        assertTrue("Should contain flex/dock check step", hasFlexStep)

        val hasDiodeTip = suggestion.diodeModeReferenceTips.any { it.contains("I2C", ignoreCase = true) || it.contains("V", ignoreCase = true) }
        assertTrue("Should contain diode mode reference", hasDiodeTip)
    }

    @Test
    fun testThermalSensorFailureGeneratesTargetedThermalAndBatterySteps() = runBlocking {
        val report = createSampleReport(
            diagnosisLabel = "Sensor Térmico NTC / Batería",
            ruleId = "rule_thermal_batt_ntc"
        )
        val result = service.fetchRepairSuggestions(report)

        assertTrue(result.isSuccess)
        val suggestion = result.getOrThrow()

        val hasThermalRef = suggestion.detailedSteps.any { it.contains("térmico", ignoreCase = true) || it.contains("batería", ignoreCase = true) || it.contains("NTC", ignoreCase = true) }
        assertTrue("Should contain thermal / battery steps", hasThermalRef)
    }
}

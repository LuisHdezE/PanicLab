package com.example

import com.example.diagnostic.ExtractedSensors
import com.example.domain.model.DiagnosticEvidence
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedMetadata
import com.example.parser.EvidenceExtractor
import com.example.platform.UuidIdGenerator

/**
 * Keeps the frozen Android baseline test call shape while the production engine
 * is cut over to COMMON. The actual implementation executed is the shared one.
 */
fun EvidenceExtractor.extractEvidences(
    logText: String,
    metadata: ParsedMetadata,
    panicFamilies: List<PanicFamily>,
    extractedSensors: ExtractedSensors
): List<DiagnosticEvidence> = extractEvidences(
    logText = logText,
    metadata = metadata,
    panicFamilies = panicFamilies,
    extractedSensors = extractedSensors,
    idGenerator = UuidIdGenerator
)

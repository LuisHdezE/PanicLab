import AVFoundation
import CryptoKit
import Foundation
import Shared
import UIKit
import Vision
import XCTest
@testable import PanicLabIOS

final class DiagnosticBridgeTests: XCTestCase {
    func testSharedFacadeMatchesAndroidProvenCanonicalDiagnosis() throws {
        let (rulePack, checksum) = try canonicalRulePack()
        let rawLog = "{\"bug_type\":\"210\",\"product\":\"iPhone14,7\",\"os_version\":\"17.3\"}\npanic(cpu 1): \"SMC PANIC - ASSERTION FAILED: S.sensor array is 0x0, 0x500000, 0x0\""

        let result = try NativeDiagnosticFacade().analyze(
            rawLog: rawLog,
            rulePackJson: rulePack,
            rulePackChecksum: checksum
        )

        XCTAssertEqual(result.productCode, "iPhone14,7")
        XCTAssertEqual(result.deviceName, "iPhone 14")
        XCTAssertEqual(result.diagnosis, "Batería")
        XCTAssertEqual(result.confidence, "HIGH")
        XCTAssertTrue(result.panicFamiliesText.contains("SMC_ASSERTION"))
        XCTAssertTrue(result.isConclusive)
        XCTAssertGreaterThan(result.evidenceCount, 0)
    }

    func testSharedFacadeMatchesAndroidProvenCanonicalDecimalWirelessDiagnosis() throws {
        let (rulePack, checksum) = try canonicalRulePack()
        let rawLog = "\"product\":\"iPhone14,7\"\n\"panicString\":\"SMC PANIC - ASSERT: SMC BSC failure\\nS.sensor array 0 - 5 is 0, 4194304, 0, 0, 0\""

        let result = try NativeDiagnosticFacade().analyze(
            rawLog: rawLog,
            rulePackJson: rulePack,
            rulePackChecksum: checksum
        )

        XCTAssertEqual(result.productCode, "iPhone14,7")
        XCTAssertEqual(result.deviceName, "iPhone 14")
        XCTAssertEqual(result.diagnosis, "Bobina de carga inalámbrica")
        XCTAssertEqual(result.confidence, "HIGH")
        XCTAssertTrue(result.panicFamiliesText.contains("SMC_BSC_FAILURE"))
        XCTAssertTrue(result.isConclusive)
        XCTAssertGreaterThan(result.evidenceCount, 0)
    }

    func testSharedFacadeReturnsNonConclusiveUnknownCode() throws {
        let (rulePack, checksum) = try canonicalRulePack()
        let rawLog = "{\"bug_type\":\"210\",\"product\":\"iPhone14,7\"}\npanic(cpu 0): \"SMC PANIC - BSC failure at address 0x987654 - S.sensor array is 0x0, 0x987654\""

        let result = try NativeDiagnosticFacade().analyze(
            rawLog: rawLog,
            rulePackJson: rulePack,
            rulePackChecksum: checksum
        )

        XCTAssertEqual(result.productCode, "iPhone14,7")
        XCTAssertEqual(result.confidence, "UNKNOWN")
        XCTAssertFalse(result.isConclusive)
    }

    func testAppleOfficialKnowledgeEmbeddedCatalogIsAvailableFromSwift() throws {
        let facade = NativeEmbeddedAppleOfficialKnowledgeFacade()

        XCTAssertEqual(facade.exactModels().count, 48)

        let summary = try XCTUnwrap(facade.modelSummary(exactModel: "iPhone 12"))
        XCTAssertEqual(summary.family, "iPhone 12")
        XCTAssertEqual(summary.publicRepairManual, "SUPPORTED")
        XCTAssertEqual(summary.recoveryDiagnosticsMode, "NOT_SUPPORTED")

        XCTAssertEqual(facade.cards(exactModel: "iPhone 12", categoryName: nil).count, 13)
        XCTAssertEqual(facade.cards(exactModel: "iPhone 12 mini", categoryName: nil).count, 9)
        XCTAssertEqual(facade.cards(exactModel: "iPhone 12 Pro", categoryName: nil).count, 10)
        XCTAssertEqual(facade.cards(exactModel: "iPhone 12 Pro Max", categoryName: nil).count, 9)

        let card = try XCTUnwrap(facade.cardDetail(cardId: "AOKF-12-001"))
        XCTAssertTrue(card.sourceReferences.contains { $0.resolution == "UNMAPPED" })
        XCTAssertTrue(card.sourceReferences.contains { $0.resolution == "EXACT_SINGLE" })
    }

    func testSharedFacadeExportsMalformedRulePackAsSwiftError() {
        XCTAssertThrowsError(
            try NativeDiagnosticFacade().analyze(
                rawLog: "iPhone14,7\nSMC PANIC",
                rulePackJson: "{not-json",
                rulePackChecksum: String(repeating: "0", count: 64)
            )
        )
    }

    func testSharedFacadeHandlesLargePhysicalStylePanicLog() throws {
        let (rulePack, checksum) = try canonicalRulePack()
        let mailboxLine = "[RX] user01 0x0000000110b28abb 0x000000000008b000 0x0010230000000020\n"
        let mailboxTail = String(repeating: mailboxLine, count: 6_000)
        let rawLog = """
        {"bug_type":"210","timestamp":"2026-08-26 10:30:36.00 -0300","os_version":"iPhone OS 26.5 (23F77)","roots_installed":0}
        {
          "product":"iPhone14,7",
          "panicString":"SMC PANIC - ASSERT: target/d27/target.cpp:321: 0, SMC BSC failure, TAOJ ----\\nS.sensor array 0 - 5 is 0, 4194304, 0, 0, 0\\nF.sensor array 0 - 1 is 0"
        }
        \(mailboxTail)
        """

        XCTAssertGreaterThan(rawLog.utf8.count, 400_000)

        let result = try NativeDiagnosticFacade().analyze(
            rawLog: rawLog,
            rulePackJson: rulePack,
            rulePackChecksum: checksum
        )

        XCTAssertEqual(result.productCode, "iPhone14,7")
        XCTAssertEqual(result.deviceName, "iPhone 14")
        XCTAssertEqual(result.diagnosis, "Bobina de carga inalámbrica")
        XCTAssertEqual(result.confidence, "HIGH")
        XCTAssertTrue(result.panicFamiliesText.contains("SMC_BSC_FAILURE"))
        XCTAssertTrue(result.isConclusive)
    }

    @MainActor
    func testImportedFixtureMatchesPastePathDiagnosis() throws {
        let rawLog = "{\"bug_type\":\"210\",\"product\":\"iPhone14,7\",\"os_version\":\"17.3\"}\npanic(cpu 1): \"SMC PANIC - ASSERTION FAILED: S.sensor array is 0x0, 0x500000, 0x0\""
        let url = try temporaryFile(extension: "ips", data: Data(rawLog.utf8))
        defer { try? FileManager.default.removeItem(at: url) }

        let imported = DiagnosticViewModel()
        imported.importDocument(at: url)

        XCTAssertEqual(imported.logText, rawLog)
        XCTAssertEqual(imported.importedFileName, url.lastPathComponent)
        guard case .idle = imported.state else {
            return XCTFail("Successful import must return to idle state")
        }

        let pasted = DiagnosticViewModel()
        pasted.logText = rawLog

        let (rulePack, checksum) = try canonicalRulePack()
        let importedResult = try NativeDiagnosticFacade().analyze(
            rawLog: imported.logText,
            rulePackJson: rulePack,
            rulePackChecksum: checksum
        )
        let pastedResult = try NativeDiagnosticFacade().analyze(
            rawLog: pasted.logText,
            rulePackJson: rulePack,
            rulePackChecksum: checksum
        )

        XCTAssertEqual(importedResult.diagnosis, pastedResult.diagnosis)
        XCTAssertEqual(importedResult.confidence, pastedResult.confidence)
        XCTAssertEqual(importedResult.productCode, pastedResult.productCode)
        XCTAssertEqual(importedResult.evidenceCount, pastedResult.evidenceCount)
        XCTAssertEqual(importedResult.isConclusive, pastedResult.isConclusive)
    }

    @MainActor
    func testImportCancellationPreservesExistingInputAndReturnsIdle() {
        let viewModel = DiagnosticViewModel()
        viewModel.logText = "existing-log"

        viewModel.handleImportResult(.failure(CocoaError(.userCancelled)))

        XCTAssertEqual(viewModel.logText, "existing-log")
        guard case .idle = viewModel.state else {
            return XCTFail("Cancellation must be harmless")
        }
    }

    @MainActor
    func testImportRejectsUnsupportedExtension() throws {
        let url = try temporaryFile(extension: "pdf", data: Data("panic".utf8))
        defer { try? FileManager.default.removeItem(at: url) }

        let viewModel = DiagnosticViewModel()
        viewModel.importDocument(at: url)

        XCTAssertTrue(errorMessage(from: viewModel.state)?.contains("Formato no compatible") == true)
        XCTAssertTrue(viewModel.logText.isEmpty)
    }

    @MainActor
    func testImportRejectsEmptyText() throws {
        let url = try temporaryFile(extension: "txt", data: Data("  \n\t".utf8))
        defer { try? FileManager.default.removeItem(at: url) }

        let viewModel = DiagnosticViewModel()
        viewModel.importDocument(at: url)

        XCTAssertTrue(errorMessage(from: viewModel.state)?.contains("vacío") == true)
    }

    @MainActor
    func testImportRejectsOversizedFile() throws {
        let data = Data(repeating: 0x41, count: DiagnosticViewModel.maxImportBytes + 1)
        let url = try temporaryFile(extension: "log", data: data)
        defer { try? FileManager.default.removeItem(at: url) }

        let viewModel = DiagnosticViewModel()
        viewModel.importDocument(at: url)

        XCTAssertTrue(errorMessage(from: viewModel.state)?.contains("límite de 5 MiB") == true)
        XCTAssertTrue(viewModel.logText.isEmpty)
    }

    @MainActor
    func testImportAcceptsUtf16LittleEndianWithBom() throws {
        let expected = "panic(cpu 0): UTF16 fixture"
        var data = Data([0xFF, 0xFE])
        data.append(try XCTUnwrap(expected.data(using: .utf16LittleEndian)))
        let url = try temporaryFile(extension: "log", data: data)
        defer { try? FileManager.default.removeItem(at: url) }

        let viewModel = DiagnosticViewModel()
        viewModel.importDocument(at: url)

        XCTAssertEqual(viewModel.logText, expected)
        guard case .idle = viewModel.state else {
            return XCTFail("Valid BOM-marked UTF-16 must import successfully")
        }
    }

    @MainActor
    func testImportRejectsUndecodableText() throws {
        let url = try temporaryFile(extension: "json", data: Data([0xFF, 0xFF, 0xFF]))
        defer { try? FileManager.default.removeItem(at: url) }

        let viewModel = DiagnosticViewModel()
        viewModel.importDocument(at: url)

        XCTAssertTrue(errorMessage(from: viewModel.state)?.contains("UTF-8 o UTF-16 con BOM") == true)
        XCTAssertTrue(viewModel.logText.isEmpty)
    }

    @MainActor
    func testImportRejectsMissingFileAsUnreadable() {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("txt")

        let viewModel = DiagnosticViewModel()
        viewModel.importDocument(at: url)

        XCTAssertTrue(errorMessage(from: viewModel.state)?.contains("No se pudo leer") == true)
        XCTAssertTrue(viewModel.logText.isEmpty)
    }

    func testNativeOcrFacadePreservesCommonCleanupForSwift() {
        let raw = "iPhone14,7\nSMC PANIC\nS.sensor array is 0x0, 0x500000, 0x0"
        let result = NativeOcrFacade().process(rawText: raw)

        XCTAssertTrue(result.cleanedText.contains("0x500000"))
        XCTAssertTrue(result.hasValidPanicSignatures)
        XCTAssertTrue(result.keywordsText.contains("SMC"))
        XCTAssertEqual(result.detectedDeviceModel, "iPhone14,7")
    }

    func testPermissionStateMappingIsDeterministic() {
        XCTAssertEqual(CameraPermissionState.from(.notDetermined), .notDetermined)
        XCTAssertEqual(CameraPermissionState.from(.authorized), .authorized)
        XCTAssertEqual(CameraPermissionState.from(.denied), .denied)
        XCTAssertEqual(CameraPermissionState.from(.restricted), .restricted)
    }

    @MainActor
    func testReviewedOcrTextIsPreservedWithoutAutomaticDiagnosis() {
        let viewModel = DiagnosticViewModel()
        let edited = "iPhone14,7\nSMC PANIC\nS.sensor array is 0x0, 0x500000, 0x0\nTECHNICIAN EDIT"

        viewModel.acceptReviewedOcrText(edited)

        XCTAssertEqual(viewModel.logText, edited)
        XCTAssertNil(viewModel.importedFileName)
        guard case .idle = viewModel.state else {
            return XCTFail("Accepting reviewed OCR must not auto-diagnose")
        }
    }

    func testGalleryPipelineUsesInjectedRecognizerAndCommonCleanup() throws {
        let recognizer = StubRecognizer(result: .success("iPhone14,7\nSMC PANIC\nS.sensor array is 0x0, 0x500000, 0x0"))
        let controller = CameraScannerController(recognizer: recognizer, cleaner: SharedOcrCleaner())
        let image = solidImage()
        let expectation = expectation(description: "gallery OCR")

        controller.processGalleryImage(image) { result in
            switch result {
            case .success(let cleaned):
                XCTAssertTrue(cleaned.hasValidPanicSignatures)
                XCTAssertTrue(cleaned.cleanedText.contains("0x500000"))
            case .failure(let error):
                XCTFail("Unexpected failure: \(error)")
            }
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5)
    }

    func testGalleryPipelineSurfacesRecognizerFailure() {
        let recognizer = StubRecognizer(result: .failure(TestScannerError.expected))
        let controller = CameraScannerController(recognizer: recognizer, cleaner: SharedOcrCleaner())
        let image = solidImage()
        let expectation = expectation(description: "gallery OCR failure")

        controller.processGalleryImage(image) { result in
            if case .success = result {
                XCTFail("Expected OCR failure")
            }
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5)
    }

    func testRealVisionStillImageRecognizesStablePanicTokenAndCommonCleanupAcceptsIt() throws {
        let image = renderedPanicImage()
        let cgImage = try XCTUnwrap(image.cgImage)

        let raw = try VisionTextRecognizer().recognize(
            cgImage: cgImage,
            orientation: .up,
            recognitionLevel: .accurate
        )

        XCTAssertTrue(raw.uppercased().contains("SMC"), "Vision output: \(raw)")
        let cleaned = NativeOcrFacade().process(rawText: raw)
        XCTAssertFalse(cleaned.cleanedText.isEmpty)
        XCTAssertTrue(cleaned.keywordsText.contains("SMC"), "Cleaned OCR: \(cleaned.cleanedText)")
    }

    private func canonicalRulePack() throws -> (String, String) {
        let bundle = Bundle(for: DiagnosticBridgeTests.self)
        let rulePack = try RulePackLoader.loadCanonicalRulePack(bundle: bundle)
        let checksum = SHA256.hash(data: Data(rulePack.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        return (rulePack, checksum)
    }

    private func temporaryFile(extension fileExtension: String, data: Data) throws -> URL {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension(fileExtension)
        try data.write(to: url, options: .atomic)
        return url
    }

    private func solidImage() -> UIImage {
        UIGraphicsImageRenderer(size: CGSize(width: 100, height: 100)).image { context in
            UIColor.white.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 100, height: 100))
        }
    }

    private func renderedPanicImage() -> UIImage {
        let size = CGSize(width: 1600, height: 700)
        return UIGraphicsImageRenderer(size: size).image { context in
            UIColor.white.setFill()
            context.fill(CGRect(origin: .zero, size: size))

            let paragraph = NSMutableParagraphStyle()
            paragraph.lineSpacing = 18
            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.monospacedSystemFont(ofSize: 70, weight: .bold),
                .foregroundColor: UIColor.black,
                .paragraphStyle: paragraph
            ]
            let text = "iPhone14,7\nSMC PANIC\nS.sensor array 0x500000"
            text.draw(
                in: CGRect(x: 80, y: 80, width: 1440, height: 540),
                withAttributes: attributes
            )
        }
    }

    @MainActor
    private func errorMessage(from state: DiagnosticViewModel.State) -> String? {
        if case .error(let message) = state {
            return message
        }
        return nil
    }
}

private struct StubRecognizer: VisionTextRecognizing {
    let result: Result<String, Error>

    func recognize(
        pixelBuffer: CVPixelBuffer,
        orientation: CGImagePropertyOrientation,
        recognitionLevel: VNRequestTextRecognitionLevel
    ) throws -> String {
        try result.get()
    }

    func recognize(
        cgImage: CGImage,
        orientation: CGImagePropertyOrientation,
        recognitionLevel: VNRequestTextRecognitionLevel
    ) throws -> String {
        try result.get()
    }
}

private enum TestScannerError: Error {
    case expected
}

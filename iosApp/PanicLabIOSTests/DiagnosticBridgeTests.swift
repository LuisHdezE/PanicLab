import CryptoKit
import Foundation
import Shared
import XCTest
@testable import PanicLabIOS

final class DiagnosticBridgeTests: XCTestCase {
    func testSharedFacadeMatchesAndroidProvenCanonicalDiagnosis() throws {
        let (rulePack, checksum) = try canonicalRulePack()

        // Same canonical product-rule-pack case proven through the Android
        // repository -> shared engine -> Room integration path in TASK-KMP-051.
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

        // Same decimal-normalization product case proven by
        // AndroidSharedCutoverIntegrationTest in TASK-KMP-051.
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
    func testImportRejectsUndecodableText() throws {
        let url = try temporaryFile(extension: "json", data: Data([0xFF, 0xFF, 0xFF]))
        defer { try? FileManager.default.removeItem(at: url) }

        let viewModel = DiagnosticViewModel()
        viewModel.importDocument(at: url)

        XCTAssertTrue(errorMessage(from: viewModel.state)?.contains("UTF-8 o UTF-16") == true)
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

    @MainActor
    private func errorMessage(from state: DiagnosticViewModel.State) -> String? {
        if case .error(let message) = state {
            return message
        }
        return nil
    }
}

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

    private func canonicalRulePack() throws -> (String, String) {
        let bundle = Bundle(for: DiagnosticBridgeTests.self)
        let rulePack = try RulePackLoader.loadCanonicalRulePack(bundle: bundle)
        let checksum = SHA256.hash(data: Data(rulePack.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        return (rulePack, checksum)
    }
}

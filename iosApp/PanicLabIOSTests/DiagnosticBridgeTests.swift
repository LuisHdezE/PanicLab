import CryptoKit
import Foundation
import Shared
import XCTest
@testable import PanicLabIOS

final class DiagnosticBridgeTests: XCTestCase {
    func testSharedFacadeMatchesAndroidProvenCanonicalDiagnosis() throws {
        let bundle = Bundle(for: DiagnosticBridgeTests.self)
        let rulePack = try RulePackLoader.loadCanonicalRulePack(bundle: bundle)
        let checksum = SHA256.hash(data: Data(rulePack.utf8))
            .map { String(format: "%02x", $0) }
            .joined()

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

    func testSharedFacadeReturnsNonConclusiveUnknownCode() throws {
        let bundle = Bundle(for: DiagnosticBridgeTests.self)
        let rulePack = try RulePackLoader.loadCanonicalRulePack(bundle: bundle)
        let checksum = SHA256.hash(data: Data(rulePack.utf8))
            .map { String(format: "%02x", $0) }
            .joined()

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
}

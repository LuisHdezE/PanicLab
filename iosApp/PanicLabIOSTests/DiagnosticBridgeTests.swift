import CryptoKit
import Foundation
import Shared
import XCTest
@testable import PanicLabIOS

final class DiagnosticBridgeTests: XCTestCase {
    func testSharedFacadeReturnsFrozenKnownDiagnosis() throws {
        let bundle = Bundle(for: DiagnosticBridgeTests.self)
        let rulePack = try RulePackLoader.loadCanonicalRulePack(bundle: bundle)
        let checksum = SHA256.hash(data: Data(rulePack.utf8))
            .map { String(format: "%02x", $0) }
            .joined()

        let rawLog = "{\"bug_type\":\"210\",\"os_version\":\"iPhone OS 17.4 (21E236)\",\"product\":\"iPhone14,4\",\"build\":\"21E236\"}\npanic(cpu 0): \"SMC PANIC - BSC failure at address 0x1000 - S.sensor array 0 - 6 is 0x0, 0x1000, 0x0, 0x0\""

        let result = try NativeDiagnosticFacade().analyze(
            rawLog: rawLog,
            rulePackJson: rulePack,
            rulePackChecksum: checksum
        )

        XCTAssertEqual(result.productCode, "iPhone14,4")
        XCTAssertEqual(result.diagnosis, "Micrófono Inferior / Flex de Carga (Mic1 / Dock)")
        XCTAssertEqual(result.confidence, "HIGH")
        XCTAssertEqual(result.verificationStatus, "VERIFIED")
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

import CryptoKit
import Foundation
import Shared
import UIKit

@MainActor
final class DiagnosticViewModel: ObservableObject {
    enum State {
        case idle
        case loading
        case result(NativeDiagnosticResult)
        case nonConclusive(NativeDiagnosticResult)
        case error(String)
    }

    @Published var logText: String = ""
    @Published private(set) var state: State = .idle

    func pasteFromClipboard() {
        guard let clipboardText = UIPasteboard.general.string, !clipboardText.isEmpty else {
            state = .error("El portapapeles no contiene texto.")
            return
        }
        logText = clipboardText
        state = .idle
    }

    func clear() {
        logText = ""
        state = .idle
    }

    func analyze() {
        let rawLog = logText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawLog.isEmpty else {
            state = .error("Pega o escribe un Panic Full antes de analizar.")
            return
        }

        state = .loading

        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let rulePack = try RulePackLoader.loadCanonicalRulePack()
                let checksum = SHA256.hash(data: Data(rulePack.utf8))
                    .map { String(format: "%02x", $0) }
                    .joined()

                let result = try NativeDiagnosticFacade().analyze(
                    rawLog: rawLog,
                    rulePackJson: rulePack,
                    rulePackChecksum: checksum
                )

                DispatchQueue.main.async { [weak self] in
                    self?.state = result.isConclusive
                        ? .result(result)
                        : .nonConclusive(result)
                }
            } catch {
                DispatchQueue.main.async { [weak self] in
                    self?.state = .error(error.localizedDescription)
                }
            }
        }
    }
}

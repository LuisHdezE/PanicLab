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

    enum ImportError: LocalizedError {
        case unsupportedExtension(String)
        case tooLarge(Int)
        case unsupportedEncoding
        case empty
        case unreadable(String)

        var errorDescription: String? {
            switch self {
            case .unsupportedExtension(let ext):
                return "Formato no compatible: .\(ext). Usa .ips, .txt, .log o .json."
            case .tooLarge(let maxBytes):
                let maxMiB = maxBytes / (1024 * 1024)
                return "El archivo supera el límite de \(maxMiB) MiB."
            case .unsupportedEncoding:
                return "No se pudo decodificar el archivo como UTF-8 o UTF-16 con BOM."
            case .empty:
                return "El archivo está vacío o no contiene texto útil."
            case .unreadable(let detail):
                return "No se pudo leer el archivo: \(detail)"
            }
        }
    }

    static let maxImportBytes = 5 * 1024 * 1024
    static let supportedImportExtensions = Set(["ips", "txt", "log", "json"])

    @Published var logText: String = ""
    @Published private(set) var state: State = .idle
    @Published private(set) var importedFileName: String?

    func pasteFromClipboard() {
        guard let clipboardText = UIPasteboard.general.string, !clipboardText.isEmpty else {
            state = .error("El portapapeles no contiene texto.")
            return
        }
        logText = clipboardText
        importedFileName = nil
        state = .idle
    }

    func acceptReviewedOcrText(_ text: String) {
        logText = text
        importedFileName = nil
        state = .idle
    }

    func clear() {
        logText = ""
        importedFileName = nil
        state = .idle
    }

    func handleImportResult(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else {
                state = .idle
                return
            }
            importDocument(at: url)
        case .failure(let error):
            let nsError = error as NSError
            if nsError.domain == NSCocoaErrorDomain && nsError.code == NSUserCancelledError {
                state = .idle
                return
            }
            state = .error("No se pudo importar el archivo: \(error.localizedDescription)")
        }
    }

    func importDocument(at url: URL) {
        do {
            let text = try Self.readImportedText(from: url)
            logText = text
            importedFileName = url.lastPathComponent
            state = .idle
        } catch {
            importedFileName = nil
            state = .error(error.localizedDescription)
        }
    }

    static func readImportedText(from url: URL) throws -> String {
        let ext = url.pathExtension.lowercased()
        guard supportedImportExtensions.contains(ext) else {
            throw ImportError.unsupportedExtension(ext.isEmpty ? "sin extensión" : ext)
        }

        let accessedSecurityScope = url.startAccessingSecurityScopedResource()
        defer {
            if accessedSecurityScope {
                url.stopAccessingSecurityScopedResource()
            }
        }

        do {
            let values = try url.resourceValues(forKeys: [.fileSizeKey, .isRegularFileKey])
            if values.isRegularFile == false {
                throw ImportError.unreadable("la selección no es un archivo regular")
            }
            if let size = values.fileSize, size > maxImportBytes {
                throw ImportError.tooLarge(maxImportBytes)
            }

            let data = try Data(contentsOf: url, options: .mappedIfSafe)
            guard data.count <= maxImportBytes else {
                throw ImportError.tooLarge(maxImportBytes)
            }

            let decoded: String?
            if let utf8 = String(data: data, encoding: .utf8) {
                decoded = utf8
            } else if data.starts(with: [0xFF, 0xFE]) {
                decoded = String(data: Data(data.dropFirst(2)), encoding: .utf16LittleEndian)
            } else if data.starts(with: [0xFE, 0xFF]) {
                decoded = String(data: Data(data.dropFirst(2)), encoding: .utf16BigEndian)
            } else {
                decoded = nil
            }

            guard let text = decoded else {
                throw ImportError.unsupportedEncoding
            }
            guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                throw ImportError.empty
            }
            return text
        } catch let importError as ImportError {
            throw importError
        } catch {
            throw ImportError.unreadable(error.localizedDescription)
        }
    }

    func analyze() {
        let rawLog = logText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawLog.isEmpty else {
            state = .error("Pega, escribe, importa o escanea un Panic Full antes de analizar.")
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

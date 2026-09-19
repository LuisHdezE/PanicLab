import Foundation

enum RulePackLoader {
    static func loadCanonicalRulePack(bundle: Bundle = .main) throws -> String {
        guard let url = bundle.url(forResource: "paniclab_rules_v1", withExtension: "json") else {
            throw RulePackLoaderError.missingResource
        }
        return try String(contentsOf: url, encoding: .utf8)
    }
}

enum RulePackLoaderError: LocalizedError {
    case missingResource

    var errorDescription: String? {
        switch self {
        case .missingResource:
            return "No se encontró el Rule Pack incluido en la aplicación."
        }
    }
}

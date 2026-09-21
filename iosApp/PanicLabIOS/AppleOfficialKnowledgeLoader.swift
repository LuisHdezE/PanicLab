import Foundation
import Shared

enum AppleOfficialKnowledgeLoaderError: LocalizedError {
    case missingResource(String)
    case unreadableResource(String)

    var errorDescription: String? {
        switch self {
        case .missingResource(let name):
            return "No se encontró el recurso Apple Official Knowledge: \(name)."
        case .unreadableResource(let name):
            return "No se pudo leer el recurso Apple Official Knowledge: \(name)."
        }
    }
}

enum AppleOfficialKnowledgeLoader {
    private static let capabilitiesName = "apple_official_knowledge_capabilities_v1"
    private static let sourceNames = [
        "apple_official_knowledge_sources_v1_part1",
        "apple_official_knowledge_sources_v1_part2",
        "apple_official_knowledge_sources_v1_part3"
    ]
    private static let cardNames = [
        "apple_official_knowledge_cards_v1_part1",
        "apple_official_knowledge_cards_v1_part2a",
        "apple_official_knowledge_cards_v1_part2b",
        "apple_official_knowledge_cards_v1_part2c",
        "apple_official_knowledge_cards_v1_part2d",
        "apple_official_knowledge_cards_v1_part3a",
        "apple_official_knowledge_cards_v1_part3b",
        "apple_official_knowledge_cards_v1_part3c",
        "apple_official_knowledge_cards_v1_part3d",
        "apple_official_knowledge_cards_v1_part4a",
        "apple_official_knowledge_cards_v1_part4b",
        "apple_official_knowledge_cards_v1_part4c",
        "apple_official_knowledge_cards_v1_part4d",
        "apple_official_knowledge_cards_v1_part5a",
        "apple_official_knowledge_cards_v1_part5b",
        "apple_official_knowledge_cards_v1_part5c",
        "apple_official_knowledge_cards_v1_part5d"
    ]

    static func loadFacade(bundle: Bundle = .main) throws -> NativeAppleOfficialKnowledgeFacade {
        let capabilities = try read(capabilitiesName, bundle: bundle)
        let sources = try sourceNames.map { try read($0, bundle: bundle) }
        let cards = try cardNames.map { try read($0, bundle: bundle) }

        return NativeAppleOfficialKnowledgeFacade(
            capabilitiesJson: capabilities,
            sourceJsonParts: sources,
            cardJsonParts: cards
        )
    }

    private static func read(_ name: String, bundle: Bundle) throws -> String {
        guard let url = bundle.url(
            forResource: name,
            withExtension: "json",
            subdirectory: "appleknowledge"
        ) else {
            throw AppleOfficialKnowledgeLoaderError.missingResource(name)
        }
        guard let text = try? String(contentsOf: url, encoding: .utf8) else {
            throw AppleOfficialKnowledgeLoaderError.unreadableResource(name)
        }
        return text
    }
}

import Shared
import SwiftUI

final class AppleOfficialKnowledgeViewModel: ObservableObject {
    @Published var exactModels: [String] = []
    @Published var selectedModel: String = ""
    @Published var selectedCategory: String? = nil
    @Published var modelSummary: NativeAppleKnowledgeModelSummary?
    @Published var cards: [NativeAppleKnowledgeCardSummary] = []
    @Published var errorMessage: String?

    private var facade: NativeAppleOfficialKnowledgeFacade?

    init() {
        do {
            let facade = try AppleOfficialKnowledgeLoader.loadFacade()
            self.facade = facade
            exactModels = facade.exactModels()
            if let defaultModel = exactModels.last {
                selectedModel = defaultModel
                refresh()
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    var categories: [String] {
        Array(Set(cards.map(\.category))).sorted()
    }

    var filteredCards: [NativeAppleKnowledgeCardSummary] {
        guard let selectedCategory else { return cards }
        return cards.filter { $0.category == selectedCategory }
    }

    func selectModel(_ model: String) {
        selectedModel = model
        selectedCategory = nil
        refresh()
    }

    func selectCategory(_ category: String?) {
        selectedCategory = category
    }

    func detail(for cardId: String) -> NativeAppleKnowledgeCardDetail? {
        facade?.cardDetail(cardId: cardId)
    }

    private func refresh() {
        guard let facade, !selectedModel.isEmpty else { return }
        modelSummary = facade.modelSummary(exactModel: selectedModel)
        cards = facade.cards(exactModel: selectedModel, categoryName: nil)
    }
}

struct AppleOfficialKnowledgeView: View {
    @StateObject private var viewModel = AppleOfficialKnowledgeViewModel()

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    contextBanner
                    modelPicker

                    if let errorMessage = viewModel.errorMessage {
                        errorCard(errorMessage)
                    } else if let summary = viewModel.modelSummary {
                        capabilityCard(summary)
                        categoryFilters
                        Text("\(viewModel.filteredCards.count) fichas aplicables")
                            .font(.footnote)
                            .foregroundStyle(.secondary)

                        ForEach(viewModel.filteredCards, id: \.id) { card in
                            AppleOfficialKnowledgeCardRow(
                                card: card,
                                detailProvider: viewModel.detail(for:)
                            )
                        }
                    }
                }
                .padding(18)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Apple Oficial")
        }
        .navigationViewStyle(.stack)
        .accessibilityIdentifier("paniclab.appleOfficial.screen")
    }

    private var contextBanner: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "checkmark.seal.fill")
                .foregroundStyle(.cyan)
            VStack(alignment: .leading, spacing: 5) {
                Text("Contexto oficial Apple")
                    .font(.headline)
                Text("Documentación, capacidades y antecedentes auditados. Esta capa no cambia la causa, el score, la confianza ni el Rule Pack del diagnóstico Panic Full.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(15)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var modelPicker: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("Modelo exacto")
                .font(.caption.bold())
                .foregroundStyle(.secondary)
            Picker("Modelo exacto", selection: Binding(
                get: { viewModel.selectedModel },
                set: { viewModel.selectModel($0) }
            )) {
                ForEach(viewModel.exactModels, id: \.self) { model in
                    Text(model).tag(model)
                }
            }
            .pickerStyle(.menu)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityIdentifier("paniclab.appleOfficial.modelPicker")
        }
        .padding(14)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func capabilityCard(_ summary: NativeAppleKnowledgeModelSummary) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(summary.exactModel)
                .font(.title3.bold())
            Text("\(summary.family) · \(summary.releaseYears) · verificado \(summary.verifiedAt)")
                .font(.caption)
                .foregroundStyle(.secondary)

            Divider()
            capabilityRow("Repair Manual", summary.publicRepairManual)
            capabilityRow("Apple Diagnostics SSR", summary.diagnosticsSsr)
            capabilityRow("Diagnostics Mode", summary.recoveryDiagnosticsMode)
            capabilityRow("Repair Assistant", summary.repairAssistant)
            capabilityRow("Troubleshooting", summary.troubleshooting)
            capabilityRow("Historial de piezas", summary.partsServiceHistory)

            if !summary.notes.isEmpty {
                Text(summary.notes)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Link(destination: URL(string: summary.primaryOfficialUrl)!) {
                Label("Abrir referencia principal Apple", systemImage: "arrow.up.right.square")
                    .font(.footnote.bold())
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var categoryFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                filterButton(title: "Todas", category: nil)
                ForEach(viewModel.categories, id: \.self) { category in
                    filterButton(title: friendly(category), category: category)
                }
            }
        }
    }

    private func filterButton(title: String, category: String?) -> some View {
        let selected = viewModel.selectedCategory == category
        return Button {
            viewModel.selectCategory(category)
        } label: {
            Text(title)
                .font(.caption.bold())
                .padding(.horizontal, 11)
                .padding(.vertical, 7)
                .background(selected ? Color.accentColor.opacity(0.18) : Color(.secondarySystemGroupedBackground))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func capabilityRow(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 118, alignment: .leading)
            Text(friendly(value))
                .font(.caption.bold())
            Spacer(minLength: 0)
        }
    }

    private func errorCard(_ message: String) -> some View {
        Label(message, systemImage: "exclamationmark.triangle.fill")
            .font(.footnote)
            .foregroundStyle(.orange)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct AppleOfficialKnowledgeCardRow: View {
    let card: NativeAppleKnowledgeCardSummary
    let detailProvider: (String) -> NativeAppleKnowledgeCardDetail?

    @State private var expanded = false
    @State private var detail: NativeAppleKnowledgeCardDetail?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button {
                expanded.toggle()
                if expanded && detail == nil {
                    detail = detailProvider(card.id)
                }
            } label: {
                HStack(alignment: .top, spacing: 10) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(friendly(card.category))
                            .font(.caption2.bold())
                            .foregroundStyle(.cyan)
                        Text(card.subcategory)
                            .font(.headline)
                            .foregroundStyle(.primary)
                        Text(card.modelScope)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(card.sourceStatus == "CURRENT" ? "Actual" : "Histórico")
                        .font(.caption2.bold())
                        .padding(.horizontal, 7)
                        .padding(.vertical, 4)
                        .background((card.sourceStatus == "CURRENT" ? Color.green : Color.orange).opacity(0.12))
                        .clipShape(Capsule())
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .foregroundStyle(.secondary)
                }
            }
            .buttonStyle(.plain)

            if !card.symptoms.isEmpty {
                Text(card.symptoms)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            if expanded, let detail {
                Divider()
                detailBlock("Comprobaciones rápidas", detail.quickChecks)
                detailBlock("Diagnóstico Apple", detail.appleDiagnostics)
                detailBlock("Inspección / descarte", detail.inspectionOrDiscard)
                detailBlock("Acción Apple", detail.appleAction)
                detailBlock("Correlación con PanicLab", detail.panicLabCorrelation)
                detailBlock("Aplicabilidad", detail.applicabilityNotes)

                Text("FUENTES")
                    .font(.caption2.bold())
                    .foregroundStyle(.secondary)
                ForEach(Array(detail.sourceReferences.enumerated()), id: \.offset) { _, reference in
                    if let url = URL(string: reference.url) {
                        Link(destination: url) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text("\(friendly(reference.role)) · \(resolutionLabel(reference.resolution))")
                                    .font(.caption.bold())
                                Text(reference.url)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                                if !reference.exactSourceIds.isEmpty {
                                    Text(reference.exactSourceIds)
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                } else if !reference.candidateSourceIds.isEmpty {
                                    Text("Candidatos: \(reference.candidateSourceIds)")
                                        .font(.caption2)
                                        .foregroundStyle(.orange)
                                }
                            }
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(.secondarySystemGroupedBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                    }
                }

                Text("Verificado \(detail.verifiedAt) · Rule Pack effect: NONE")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(15)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityIdentifier("paniclab.appleOfficial.card.\(card.id)")
    }

    @ViewBuilder
    private func detailBlock(_ title: String, _ body: String) -> some View {
        if !body.isEmpty {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.caption2.bold())
                    .foregroundStyle(.secondary)
                Text(body)
                    .font(.footnote)
                    .textSelection(.enabled)
            }
        }
    }
}

private func friendly(_ raw: String) -> String {
    raw.replacingOccurrences(of: "_", with: " ")
        .lowercased()
        .capitalized
}

private func resolutionLabel(_ raw: String) -> String {
    switch raw {
    case "EXACT_SINGLE": return "Fuente exacta"
    case "EXACT_AMBIGUOUS": return "Coincidencia ambigua"
    case "LOCALE_PATH_CANDIDATE": return "Candidato por artículo/locale"
    case "UNMAPPED": return "Sin ID normalizado"
    default: return friendly(raw)
    }
}

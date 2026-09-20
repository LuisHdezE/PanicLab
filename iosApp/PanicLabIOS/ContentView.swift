import Shared
import SwiftUI
import UniformTypeIdentifiers

struct ContentView: View {
    @StateObject private var viewModel = DiagnosticViewModel()
    @State private var isFileImporterPresented = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    header
                    inputCard
                    stateSection
                }
                .padding(20)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("PanicLab")
        }
        .navigationViewStyle(.stack)
        .fileImporter(
            isPresented: $isFileImporterPresented,
            allowedContentTypes: importContentTypes,
            allowsMultipleSelection: false
        ) { result in
            viewModel.handleImportResult(result)
        }
    }

    private var importContentTypes: [UTType] {
        var types: [UTType] = [.plainText, .json]
        if let ips = UTType(filenameExtension: "ips") {
            types.append(ips)
        }
        if let log = UTType(filenameExtension: "log") {
            types.append(log)
        }
        return types
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Diagnóstico Panic Full", systemImage: "waveform.path.ecg.rectangle")
                .font(.title2.bold())
            Text("Pega, escribe o importa un log compatible. El análisis determinista se ejecuta con el motor KMP compartido y el Rule Pack incluido en la app.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var inputCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Log")
                .font(.headline)

            TextEditor(text: $viewModel.logText)
                .font(.system(.footnote, design: .monospaced))
                .frame(minHeight: 220)
                .padding(8)
                .background(Color(.secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.secondary.opacity(0.22), lineWidth: 1)
                )
                .accessibilityIdentifier("paniclab.logInput")
                .accessibilityLabel("Contenido del Panic Full")

            if let importedFileName = viewModel.importedFileName {
                Label("\(importedFileName) listo para analizar", systemImage: "doc.badge.checkmark")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier("paniclab.import.status")
            }

            HStack(spacing: 10) {
                Button {
                    isFileImporterPresented = true
                } label: {
                    Label("Importar", systemImage: "doc.badge.plus")
                }
                .buttonStyle(.bordered)
                .disabled(isLoading)
                .accessibilityIdentifier("paniclab.importButton")

                Button {
                    viewModel.pasteFromClipboard()
                } label: {
                    Label("Pegar", systemImage: "doc.on.clipboard")
                }
                .buttonStyle(.bordered)
                .disabled(isLoading)
                .accessibilityIdentifier("paniclab.pasteButton")

                Button("Limpiar") {
                    viewModel.clear()
                }
                .buttonStyle(.bordered)
                .disabled(viewModel.logText.isEmpty || isLoading)
            }

            Button {
                viewModel.analyze()
            } label: {
                Label("Analizar", systemImage: "stethoscope")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading)
            .accessibilityIdentifier("paniclab.analyzeButton")

            Text("Formatos: .ips, .txt, .log, .json · UTF-8/UTF-16 · máximo 5 MiB")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    @ViewBuilder
    private var stateSection: some View {
        switch viewModel.state {
        case .idle:
            Label("Listo para analizar", systemImage: "checkmark.circle")
                .foregroundStyle(.secondary)
                .accessibilityIdentifier("paniclab.state.idle")

        case .loading:
            HStack(spacing: 12) {
                ProgressView()
                Text("Analizando con el motor compartido…")
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .accessibilityIdentifier("paniclab.state.loading")

        case .error(let message):
            StatusCard(
                title: "No se pudo analizar",
                message: message,
                systemImage: "exclamationmark.triangle.fill"
            )
            .accessibilityIdentifier("paniclab.state.error")

        case .nonConclusive(let result):
            resultCard(result, nonConclusive: true)
                .accessibilityIdentifier("paniclab.state.nonconclusive")

        case .result(let result):
            resultCard(result, nonConclusive: false)
                .accessibilityIdentifier("paniclab.state.result")
        }
    }

    private var isLoading: Bool {
        if case .loading = viewModel.state { return true }
        return false
    }

    private func resultCard(_ result: NativeDiagnosticResult, nonConclusive: Bool) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(nonConclusive ? "Diagnóstico no concluyente" : "Resultado")
                        .font(.headline)
                    Text(result.diagnosis)
                        .font(.title3.bold())
                }
                Spacer()
                Text(result.confidence)
                    .font(.caption.bold())
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.accentColor.opacity(0.12))
                    .clipShape(Capsule())
            }

            VStack(alignment: .leading, spacing: 8) {
                infoRow("Dispositivo", result.deviceName)
                infoRow("Producto", result.productCode)
                infoRow("iOS", result.osVersion)
                infoRow("Build", result.build)
                infoRow("Subsistema", result.subsystem)
                infoRow("Verificación", result.verificationStatus)
                infoRow("Rule Pack", result.knowledgeBaseVersion)
            }

            if !result.interpretation.isEmpty {
                section("Interpretación", result.interpretation)
            }
            if !result.suspectedComponentsText.isEmpty {
                section("Componentes sospechosos", result.suspectedComponentsText)
            }
            if !result.firstChecksText.isEmpty {
                section("Primeras comprobaciones", result.firstChecksText)
            }
            if !result.knownGoodTest.isEmpty {
                section("Prueba conocida", result.knownGoodTest)
            }
            if !result.boardLevelNextStepsText.isEmpty {
                section("Siguientes pasos", result.boardLevelNextStepsText)
            }
            if !result.cautionsText.isEmpty {
                section("Precauciones", result.cautionsText)
            }
            if !result.alternativesText.isEmpty {
                section("Alternativas", result.alternativesText)
            }

            Text("Evidencias detectadas: \(result.evidenceCount)")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(label)
                .foregroundStyle(.secondary)
                .frame(width: 96, alignment: .leading)
            Text(value)
                .fontWeight(.medium)
                .textSelection(.enabled)
            Spacer(minLength: 0)
        }
    }

    private func section(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title)
                .font(.subheadline.bold())
            Text(body)
                .font(.subheadline)
                .textSelection(.enabled)
        }
    }
}

private struct StatusCard: View {
    let title: String
    let message: String
    let systemImage: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: systemImage)
                .foregroundStyle(.orange)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                Text(message).font(.subheadline)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

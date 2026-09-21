import SwiftUI

struct PanicLabRootView: View {
    var body: some View {
        TabView {
            ContentView()
                .tabItem {
                    Label("Diagnóstico", systemImage: "waveform.path.ecg.rectangle")
                }
                .accessibilityIdentifier("paniclab.tab.diagnostic")

            AppleOfficialKnowledgeView()
                .tabItem {
                    Label("Apple Oficial", systemImage: "checkmark.seal")
                }
                .accessibilityIdentifier("paniclab.tab.appleOfficial")
        }
    }
}

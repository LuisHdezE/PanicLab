import AVFoundation
import PhotosUI
import Shared
import SwiftUI
import UIKit

struct OcrScannerView: View {
    @ObservedObject var diagnosticViewModel: DiagnosticViewModel
    @StateObject private var scanner = CameraScannerController()
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase

    @State private var isPhotoPickerPresented = false
    @State private var isReviewPresented = false
    @State private var reviewText = ""
    @State private var reviewResult: NativeOcrScanResult?

    private var isUITestScanner: Bool {
        #if DEBUG
        ProcessInfo.processInfo.environment["PANICLAB_UI_TEST_SCANNER"] == "1"
        #else
        false
        #endif
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.black.ignoresSafeArea()

                scannerBody

                if scanner.isGalleryProcessing {
                    Color.black.opacity(0.6).ignoresSafeArea()
                    ProgressView("Reconociendo texto…")
                        .padding(20)
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
            .navigationTitle("Escáner OCR")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cerrar") {
                        scanner.stop()
                        dismiss()
                    }
                    .accessibilityIdentifier("paniclab.scanner.close")
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    if scanner.isTorchAvailable && scanner.permissionState == .authorized {
                        Button {
                            scanner.toggleTorch()
                        } label: {
                            Image(systemName: scanner.isTorchEnabled ? "bolt.fill" : "bolt.slash")
                        }
                        .accessibilityLabel("Linterna")
                        .accessibilityIdentifier("paniclab.scanner.torch")
                    }
                }
            }
        }
        .navigationViewStyle(.stack)
        .onAppear {
            guard !isUITestScanner else { return }
            scanner.refreshPermissionState()
            scanner.startIfAuthorized()
        }
        .onDisappear {
            scanner.stop()
        }
        .onChange(of: scenePhase) { phase in
            guard !isUITestScanner else { return }
            switch phase {
            case .active:
                if !isPhotoPickerPresented && !isReviewPresented {
                    scanner.startIfAuthorized()
                }
            case .inactive, .background:
                scanner.stop()
            @unknown default:
                scanner.stop()
            }
        }
        .sheet(isPresented: $isPhotoPickerPresented, onDismiss: resumeScannerIfNeeded) {
            PanicPhotoPicker { outcome in
                switch outcome {
                case .cancelled:
                    isPhotoPickerPresented = false
                case .failure(let error):
                    isPhotoPickerPresented = false
                    scanner.clearTransientError()
                    reviewResult = nil
                    reviewText = ""
                    presentPickerError(error.localizedDescription)
                case .image(let image):
                    scanner.processGalleryImage(image) { result in
                        isPhotoPickerPresented = false
                        if case .success(let cleaned) = result, !cleaned.cleanedText.isEmpty {
                            openReview(cleaned)
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $isReviewPresented, onDismiss: resumeScannerIfNeeded) {
            reviewSheet
        }
    }

    @ViewBuilder
    private var scannerBody: some View {
        if isUITestScanner {
            unavailableFallback(
                title: "Cámara simulada para pruebas",
                message: "La automatización usa un seam controlado y no simula hardware físico."
            )
        } else {
            switch scanner.permissionState {
            case .notDetermined:
                permissionRequest
            case .authorized:
                liveScanner
            case .denied, .restricted:
                unavailableFallback(
                    title: "Acceso a cámara no disponible",
                    message: "Puedes habilitar la cámara en Ajustes o seleccionar una imagen sin conceder acceso completo a Fotos."
                )
            case .unavailable:
                unavailableFallback(
                    title: "Cámara no disponible",
                    message: "Este dispositivo o sesión no ofrece una cámara utilizable. La galería sigue disponible."
                )
            }
        }
    }

    private var permissionRequest: some View {
        VStack(spacing: 18) {
            Image(systemName: "camera.viewfinder")
                .font(.system(size: 56))
                .foregroundStyle(.white)

            Text("Escanear Panic Full")
                .font(.title2.bold())
                .foregroundStyle(.white)

            Text("PanicLab usa la cámara únicamente para reconocer texto en el dispositivo. No guarda fotos ni envía imágenes a la red.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)

            Button("Permitir cámara") {
                scanner.requestCameraAccess()
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier("paniclab.scanner.requestPermission")

            galleryButton
        }
        .padding(28)
    }

    private func unavailableFallback(title: String, message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "camera.fill.badge.exclamationmark")
                .font(.system(size: 48))
                .foregroundStyle(.white)
            Text(title)
                .font(.title3.bold())
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier("paniclab.scanner.fallback")
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            if scanner.permissionState == .denied || scanner.permissionState == .restricted {
                Button("Abrir Ajustes") {
                    guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                    UIApplication.shared.open(url)
                }
                .buttonStyle(.bordered)
                .accessibilityIdentifier("paniclab.scanner.settings")
            }

            galleryButton

            #if DEBUG
            if isUITestScanner {
                Button("Cargar muestra OCR de prueba") {
                    let raw = "iPhone14,7\nSMC PANIC\nS.sensor array is 0x0, 0x500000, 0x0"
                    let result = scanner.processRecognizedText(raw)
                    openReview(result)
                }
                .buttonStyle(.bordered)
                .accessibilityIdentifier("paniclab.scanner.testInject")
            }
            #endif
        }
        .padding(28)
    }

    private var liveScanner: some View {
        ZStack {
            CameraPreview(session: scanner.session)
                .ignoresSafeArea()

            scannerReticle

            VStack(spacing: 12) {
                liveStatus
                Spacer()
                controls
            }
            .padding(16)
        }
    }

    private var scannerReticle: some View {
        RoundedRectangle(cornerRadius: 20)
            .strokeBorder(Color.cyan.opacity(0.9), lineWidth: 2)
            .frame(maxWidth: 520, maxHeight: 320)
            .padding(.horizontal, 24)
            .allowsHitTesting(false)
    }

    private var liveStatus: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: scanner.latestResult?.hasValidPanicSignatures == true ? "checkmark.circle.fill" : "viewfinder")
                .foregroundStyle(scanner.latestResult?.hasValidPanicSignatures == true ? .green : .cyan)
            VStack(alignment: .leading, spacing: 3) {
                Text(scanner.latestResult?.confidenceHint ?? "Apunta a las líneas del Panic Full")
                    .font(.subheadline.bold())
                if let result = scanner.latestResult, !result.keywordsText.isEmpty {
                    Text(result.keywordsText.replacingOccurrences(of: "\n", with: " · "))
                        .font(.caption)
                        .lineLimit(2)
                }
                if let error = scanner.errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundStyle(.orange)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityIdentifier("paniclab.scanner.status")
    }

    private var controls: some View {
        VStack(spacing: 10) {
            Button {
                guard let result = scanner.latestResult, !result.cleanedText.isEmpty else { return }
                scanner.stop()
                openReview(result)
            } label: {
                Label("Revisar texto reconocido", systemImage: "doc.text.magnifyingglass")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(scanner.latestResult?.cleanedText.isEmpty != false)
            .accessibilityIdentifier("paniclab.scanner.review")

            galleryButton
        }
        .padding(12)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var galleryButton: some View {
        Button {
            scanner.stop()
            isPhotoPickerPresented = true
        } label: {
            Label("Elegir imagen", systemImage: "photo.on.rectangle")
        }
        .buttonStyle(.bordered)
        .accessibilityIdentifier("paniclab.scanner.gallery")
    }

    private var reviewSheet: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 14) {
                if let result = reviewResult {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(result.confidenceHint)
                            .font(.subheadline.bold())
                        if !result.keywordsText.isEmpty {
                            Text(result.keywordsText.replacingOccurrences(of: "\n", with: " · "))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        if !result.panicCodesText.isEmpty {
                            Text("Códigos: \(result.panicCodesText.replacingOccurrences(of: "\n", with: ", "))")
                                .font(.caption.monospaced())
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                Text("Revisa y corrige el OCR antes de diagnosticar.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                TextEditor(text: $reviewText)
                    .font(.system(.footnote, design: .monospaced))
                    .padding(8)
                    .background(Color(.secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .accessibilityIdentifier("paniclab.scanner.reviewText")

                HStack(spacing: 10) {
                    Button("Cancelar") {
                        isReviewPresented = false
                    }
                    .buttonStyle(.bordered)
                    .accessibilityIdentifier("paniclab.scanner.reviewCancel")

                    Button {
                        let reviewed = reviewText.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !reviewed.isEmpty else { return }
                        diagnosticViewModel.acceptReviewedOcrText(reviewed)
                        isReviewPresented = false
                        dismiss()
                        diagnosticViewModel.analyze()
                    } label: {
                        Label("Diagnosticar", systemImage: "stethoscope")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(reviewText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityIdentifier("paniclab.scanner.diagnose")
                }
            }
            .padding(16)
            .navigationTitle("Revisar OCR")
            .navigationBarTitleDisplayMode(.inline)
        }
        .navigationViewStyle(.stack)
    }

    private func openReview(_ result: NativeOcrScanResult) {
        reviewResult = result
        reviewText = result.cleanedText
        isReviewPresented = true
    }

    private func resumeScannerIfNeeded() {
        guard !isUITestScanner,
              !isPhotoPickerPresented,
              !isReviewPresented,
              scenePhase == .active else { return }
        scanner.startIfAuthorized()
    }

    private func presentPickerError(_ message: String) {
        reviewResult = nil
        reviewText = ""
        _ = message
    }
}

private struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiViewController: PreviewView, context: Context) {
        uiViewController.previewLayer.session = session
    }
}

private final class PreviewView: UIView {
    override class var layerClass: AnyClass {
        AVCaptureVideoPreviewLayer.self
    }

    var previewLayer: AVCaptureVideoPreviewLayer {
        layer as! AVCaptureVideoPreviewLayer
    }
}

private enum PhotoPickerOutcome {
    case cancelled
    case image(UIImage)
    case failure(Error)
}

private struct PanicPhotoPicker: UIViewControllerRepresentable {
    let completion: (PhotoPickerOutcome) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(completion: completion)
    }

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .images
        configuration.selectionLimit = 1
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let completion: (PhotoPickerOutcome) -> Void

        init(completion: @escaping (PhotoPickerOutcome) -> Void) {
            self.completion = completion
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard let provider = results.first?.itemProvider else {
                completion(.cancelled)
                return
            }

            guard provider.canLoadObject(ofClass: UIImage.self) else {
                completion(.failure(ScannerError.unreadableImage))
                return
            }

            provider.loadObject(ofClass: UIImage.self) { object, error in
                DispatchQueue.main.async {
                    if let error {
                        self.completion(.failure(error))
                    } else if let image = object as? UIImage {
                        self.completion(.image(image))
                    } else {
                        self.completion(.failure(ScannerError.unreadableImage))
                    }
                }
            }
        }
    }
}

import AVFoundation
import Combine
import CoreImage
import Foundation
import ImageIO
import PhotosUI
import Shared
import UIKit
import Vision

enum CameraPermissionState: Equatable {
    case notDetermined
    case authorized
    case denied
    case restricted
    case unavailable

    static func from(_ status: AVAuthorizationStatus) -> CameraPermissionState {
        switch status {
        case .notDetermined:
            return .notDetermined
        case .authorized:
            return .authorized
        case .denied:
            return .denied
        case .restricted:
            return .restricted
        @unknown default:
            return .restricted
        }
    }
}

protocol VisionTextRecognizing {
    func recognize(
        pixelBuffer: CVPixelBuffer,
        orientation: CGImagePropertyOrientation,
        recognitionLevel: VNRequestTextRecognitionLevel
    ) throws -> String

    func recognize(
        cgImage: CGImage,
        orientation: CGImagePropertyOrientation,
        recognitionLevel: VNRequestTextRecognitionLevel
    ) throws -> String
}

struct VisionTextRecognizer: VisionTextRecognizing {
    func recognize(
        pixelBuffer: CVPixelBuffer,
        orientation: CGImagePropertyOrientation,
        recognitionLevel: VNRequestTextRecognitionLevel
    ) throws -> String {
        let request = makeRequest(level: recognitionLevel)
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: orientation, options: [:])
        try handler.perform([request])
        return recognizedText(from: request)
    }

    func recognize(
        cgImage: CGImage,
        orientation: CGImagePropertyOrientation,
        recognitionLevel: VNRequestTextRecognitionLevel
    ) throws -> String {
        let request = makeRequest(level: recognitionLevel)
        let handler = VNImageRequestHandler(cgImage: cgImage, orientation: orientation, options: [:])
        try handler.perform([request])
        return recognizedText(from: request)
    }

    private func makeRequest(level: VNRequestTextRecognitionLevel) -> VNRecognizeTextRequest {
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = level
        request.usesLanguageCorrection = false
        request.recognitionLanguages = ["en-US"]
        return request
    }

    private func recognizedText(from request: VNRecognizeTextRequest) -> String {
        (request.results ?? [])
            .compactMap { $0.topCandidates(1).first?.string }
            .joined(separator: "\n")
    }
}

protocol OcrCleaning {
    func clean(rawText: String) -> NativeOcrScanResult
}

struct SharedOcrCleaner: OcrCleaning {
    func clean(rawText: String) -> NativeOcrScanResult {
        NativeOcrFacade().process(rawText: rawText)
    }
}

final class CameraScannerController: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    let session = AVCaptureSession()

    @Published private(set) var permissionState: CameraPermissionState
    @Published private(set) var latestResult: NativeOcrScanResult?
    @Published private(set) var errorMessage: String?
    @Published private(set) var isTorchAvailable = false
    @Published private(set) var isTorchEnabled = false
    @Published private(set) var isSessionRunning = false
    @Published private(set) var isGalleryProcessing = false

    private let recognizer: VisionTextRecognizing
    private let cleaner: OcrCleaning
    private let captureQueue = DispatchQueue(label: "com.luishdeze.paniclab.camera.capture", qos: .userInitiated)
    private let processingQueue = DispatchQueue(label: "com.luishdeze.paniclab.camera.ocr", qos: .userInitiated)
    private var isConfigured = false
    private var activeDevice: AVCaptureDevice?
    private var isProcessingFrame = false
    private var lastAnalysisTime = Date.distantPast
    private let analysisInterval: TimeInterval = 0.6

    init(
        recognizer: VisionTextRecognizing = VisionTextRecognizer(),
        cleaner: OcrCleaning = SharedOcrCleaner()
    ) {
        self.recognizer = recognizer
        self.cleaner = cleaner
        self.permissionState = CameraPermissionState.from(AVCaptureDevice.authorizationStatus(for: .video))
        super.init()
    }

    func refreshPermissionState() {
        publish { controller in
            controller.permissionState = CameraPermissionState.from(
                AVCaptureDevice.authorizationStatus(for: .video)
            )
        }
    }

    func requestCameraAccess() {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        guard status == .notDetermined else {
            refreshPermissionState()
            if status == .authorized {
                startIfAuthorized()
            }
            return
        }

        AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
            guard let self else { return }
            self.publish { controller in
                controller.permissionState = granted ? .authorized : .denied
            }
            if granted {
                self.startIfAuthorized()
            }
        }
    }

    func startIfAuthorized() {
        guard AVCaptureDevice.authorizationStatus(for: .video) == .authorized else {
            refreshPermissionState()
            return
        }

        captureQueue.async { [weak self] in
            guard let self else { return }
            do {
                try self.configureSessionIfNeeded()
                guard !self.session.isRunning else {
                    self.publish { $0.isSessionRunning = true }
                    return
                }
                self.session.startRunning()
                self.publish { $0.isSessionRunning = self.session.isRunning }
            } catch {
                self.publish { controller in
                    controller.errorMessage = "No se pudo iniciar la cámara: \(error.localizedDescription)"
                    controller.permissionState = .unavailable
                    controller.isSessionRunning = false
                }
            }
        }
    }

    func stop() {
        captureQueue.async { [weak self] in
            guard let self else { return }
            if self.session.isRunning {
                self.session.stopRunning()
            }
            self.publish { $0.isSessionRunning = false }
        }
    }

    func toggleTorch() {
        captureQueue.async { [weak self] in
            guard let self, let device = self.activeDevice, device.hasTorch else { return }
            do {
                try device.lockForConfiguration()
                let newValue = !device.isTorchActive
                if newValue {
                    try device.setTorchModeOn(level: AVCaptureDevice.maxAvailableTorchLevel)
                } else {
                    device.torchMode = .off
                }
                device.unlockForConfiguration()
                self.publish { controller in
                    controller.isTorchEnabled = newValue
                    controller.errorMessage = nil
                }
            } catch {
                self.publish { controller in
                    controller.errorMessage = "No se pudo cambiar la linterna: \(error.localizedDescription)"
                    controller.isTorchEnabled = false
                }
            }
        }
    }

    func clearTransientError() {
        publish { $0.errorMessage = nil }
    }

    @discardableResult
    func processRecognizedText(_ rawText: String) -> NativeOcrScanResult {
        cleaner.clean(rawText: rawText)
    }

    func processGalleryImage(
        _ image: UIImage,
        completion: @escaping (Result<NativeOcrScanResult, Error>) -> Void
    ) {
        guard let cgImage = image.cgImage else {
            completion(.failure(ScannerError.unreadableImage))
            return
        }

        publish { controller in
            controller.isGalleryProcessing = true
            controller.errorMessage = nil
        }

        let orientation = CGImagePropertyOrientation(image.imageOrientation)
        processingQueue.async { [weak self] in
            guard let self else { return }
            do {
                let rawText = try self.recognizer.recognize(
                    cgImage: cgImage,
                    orientation: orientation,
                    recognitionLevel: .accurate
                )
                let result = self.cleaner.clean(rawText: rawText)
                self.publish { controller in
                    controller.latestResult = result
                    controller.isGalleryProcessing = false
                    controller.errorMessage = result.cleanedText.isEmpty
                        ? "Vision no detectó texto útil en la imagen seleccionada."
                        : nil
                }
                DispatchQueue.main.async {
                    completion(.success(result))
                }
            } catch {
                self.publish { controller in
                    controller.isGalleryProcessing = false
                    controller.errorMessage = "No se pudo reconocer texto en la imagen: \(error.localizedDescription)"
                }
                DispatchQueue.main.async {
                    completion(.failure(error))
                }
            }
        }
    }

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        let now = Date()
        guard !isProcessingFrame,
              now.timeIntervalSince(lastAnalysisTime) >= analysisInterval,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            return
        }

        isProcessingFrame = true
        lastAnalysisTime = now
        defer { isProcessingFrame = false }

        do {
            let rawText = try recognizer.recognize(
                pixelBuffer: pixelBuffer,
                orientation: .right,
                recognitionLevel: .fast
            )
            let result = cleaner.clean(rawText: rawText)
            publish { controller in
                controller.latestResult = result
                controller.errorMessage = nil
            }
        } catch {
            publish { controller in
                controller.errorMessage = "OCR temporalmente no disponible: \(error.localizedDescription)"
            }
        }
    }

    private func configureSessionIfNeeded() throws {
        if isConfigured { return }

        guard let camera = AVCaptureDevice.default(
            .builtInWideAngleCamera,
            for: .video,
            position: .back
        ) else {
            throw ScannerError.cameraUnavailable
        }

        let input = try AVCaptureDeviceInput(device: camera)
        let output = AVCaptureVideoDataOutput()
        output.alwaysDiscardsLateVideoFrames = true
        output.videoSettings = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA
        ]
        output.setSampleBufferDelegate(self, queue: processingQueue)

        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .high

        guard session.canAddInput(input), session.canAddOutput(output) else {
            throw ScannerError.sessionConfigurationFailed
        }

        session.addInput(input)
        session.addOutput(output)

        activeDevice = camera
        isConfigured = true
        publish { controller in
            controller.permissionState = .authorized
            controller.isTorchAvailable = camera.hasTorch
        }
    }

    private func publish(_ mutation: @escaping (CameraScannerController) -> Void) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            mutation(self)
        }
    }
}

enum ScannerError: LocalizedError {
    case cameraUnavailable
    case sessionConfigurationFailed
    case unreadableImage

    var errorDescription: String? {
        switch self {
        case .cameraUnavailable:
            return "No hay una cámara trasera disponible."
        case .sessionConfigurationFailed:
            return "No se pudo configurar la sesión de cámara."
        case .unreadableImage:
            return "La imagen seleccionada no se pudo leer."
        }
    }
}

extension CGImagePropertyOrientation {
    init(_ orientation: UIImage.Orientation) {
        switch orientation {
        case .up: self = .up
        case .upMirrored: self = .upMirrored
        case .down: self = .down
        case .downMirrored: self = .downMirrored
        case .left: self = .left
        case .leftMirrored: self = .leftMirrored
        case .right: self = .right
        case .rightMirrored: self = .rightMirrored
        @unknown default: self = .up
        }
    }
}

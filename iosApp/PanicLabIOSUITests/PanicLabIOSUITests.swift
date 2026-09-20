import XCTest

final class PanicLabIOSUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testNativeSliceExposesAccessibleInputImportScannerAndErrorState() throws {
        let app = XCUIApplication()
        app.launch()

        let input = app.textViews["paniclab.logInput"]
        XCTAssertTrue(input.waitForExistence(timeout: 10))

        let scannerButton = app.buttons["paniclab.scannerButton"]
        XCTAssertTrue(scannerButton.exists)
        XCTAssertTrue(scannerButton.isEnabled)

        let importButton = app.buttons["paniclab.importButton"]
        XCTAssertTrue(importButton.exists)
        XCTAssertTrue(importButton.isEnabled)

        let analyze = app.buttons["paniclab.analyzeButton"]
        XCTAssertTrue(analyze.exists)
        analyze.tap()

        let errorState = app.descendants(matching: .any)["paniclab.state.error"]
        XCTAssertTrue(errorState.waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["No se pudo analizar"].waitForExistence(timeout: 5))
    }

    func testScannerTestSeamRequiresReviewBeforeDiagnosis() throws {
        let app = XCUIApplication()
        app.launchEnvironment["PANICLAB_UI_TEST_SCANNER"] = "1"
        app.launch()

        let scannerButton = app.buttons["paniclab.scannerButton"]
        XCTAssertTrue(scannerButton.waitForExistence(timeout: 10))
        scannerButton.tap()

        let fallback = app.descendants(matching: .any)["paniclab.scanner.fallback"]
        XCTAssertTrue(fallback.waitForExistence(timeout: 5))

        let inject = app.buttons["paniclab.scanner.testInject"]
        XCTAssertTrue(inject.waitForExistence(timeout: 5))
        inject.tap()

        let reviewText = app.textViews["paniclab.scanner.reviewText"]
        XCTAssertTrue(reviewText.waitForExistence(timeout: 5))
        XCTAssertTrue(reviewText.isEnabled)

        let diagnose = app.buttons["paniclab.scanner.diagnose"]
        XCTAssertTrue(diagnose.exists)
        diagnose.tap()

        let result = app.descendants(matching: .any)["paniclab.state.result"]
        let nonConclusive = app.descendants(matching: .any)["paniclab.state.nonconclusive"]
        XCTAssertTrue(
            result.waitForExistence(timeout: 10) || nonConclusive.waitForExistence(timeout: 2),
            "Explicit scanner diagnosis should reach the existing diagnostic flow"
        )
    }

    func testScannerCancellationLeavesExistingInputUntouched() throws {
        let app = XCUIApplication()
        app.launchEnvironment["PANICLAB_UI_TEST_SCANNER"] = "1"
        app.launch()

        let input = app.textViews["paniclab.logInput"]
        XCTAssertTrue(input.waitForExistence(timeout: 10))
        input.tap()
        input.typeText("existing-log")

        app.buttons["paniclab.scannerButton"].tap()
        XCTAssertTrue(app.buttons["paniclab.scanner.close"].waitForExistence(timeout: 5))
        app.buttons["paniclab.scanner.close"].tap()

        XCTAssertTrue(input.waitForExistence(timeout: 5))
        XCTAssertEqual(input.value as? String, "existing-log")
    }
}

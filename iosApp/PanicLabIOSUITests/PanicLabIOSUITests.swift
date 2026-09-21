import XCTest

final class PanicLabIOSUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testNativeSliceExposesAccessibleInputImportAndErrorState() throws {
        let app = XCUIApplication()
        app.launch()

        let input = app.textViews["paniclab.logInput"]
        XCTAssertTrue(input.waitForExistence(timeout: 10))

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

    func testAppleOfficialKnowledgeIsSeparatedFromDiagnosticTab() throws {
        let app = XCUIApplication()
        app.launch()

        let diagnosticInput = app.textViews["paniclab.logInput"]
        XCTAssertTrue(diagnosticInput.waitForExistence(timeout: 10))

        let appleTab = app.tabBars.buttons["Apple Oficial"]
        XCTAssertTrue(appleTab.waitForExistence(timeout: 5))
        appleTab.tap()

        let appleScreen = app.descendants(matching: .any)["paniclab.appleOfficial.screen"]
        XCTAssertTrue(appleScreen.waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Contexto oficial Apple"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["iPhone 17e"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["paniclab.analyzeButton"].exists)
    }
}

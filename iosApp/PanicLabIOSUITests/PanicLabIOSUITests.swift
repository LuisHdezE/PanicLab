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
}

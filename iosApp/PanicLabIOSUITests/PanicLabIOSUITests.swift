import XCTest

final class PanicLabIOSUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testNativeSliceExposesAccessibleInputAndErrorState() throws {
        let app = XCUIApplication()
        app.launch()

        let input = app.textViews["paniclab.logInput"]
        XCTAssertTrue(input.waitForExistence(timeout: 10))

        let analyze = app.buttons["paniclab.analyzeButton"]
        XCTAssertTrue(analyze.exists)
        analyze.tap()

        XCTAssertTrue(app.otherElements["paniclab.state.error"].waitForExistence(timeout: 5))
    }
}

package com.aistudio.paniclab.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ScaffoldProbeTest {
  @Test
  fun blankLabelFallsBackToPanicLab() {
    assertEquals("paniclab", ScaffoldProbe.normalizeLabel("   "))
  }

  @Test
  fun nonBlankLabelIsTrimmed() {
    assertEquals("diagnostic", ScaffoldProbe.normalizeLabel("  diagnostic  "))
  }
}

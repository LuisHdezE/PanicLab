import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kover)
}

kotlin {
  jvm {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_11
    }
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { target ->
    target.binaries.framework {
      baseName = "Shared"
      isStatic = true
    }
  }

  android {
    namespace = "com.aistudio.paniclab.shared"
    compileSdk = 36
    minSdk = 24

    compilerOptions {
      jvmTarget = JvmTarget.JVM_11
    }

    withHostTest {
      isIncludeAndroidResources = false
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.kotlinx.serialization.json)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}

kover {
  reports {
    filters {
      includes {
        classes(
          "com.example.util.HexUtils*",
          "com.example.parser.LogNormalizer*",
          "com.example.platform.JvmSha256Hasher*",
          "com.example.platform.AndroidSha256Hasher*",
          "com.example.parser.MetadataExtractor*",
          "com.example.parser.PanicClassifier*",
          "com.example.parser.DeviceResolver*",
          "com.example.parser.EvidenceExtractor*",
          "com.example.diagnostic.SensorExtractor*",
          "com.example.diagnostic.DiagnosticRulesEngine*",
          "com.example.diagnostic.CandidateRanker*",
          "com.example.diagnostic.DiagnosticReportBuilder*",
          "com.example.ocr.OcrLogExtractor*"
        )
      }
    }
    verify {
      rule("I3 deterministic shared thresholds") {
        minBound(90)
        minBound(85, CoverageUnit.BRANCH)
      }
    }
    total {
      xml {
        onCheck = true
        xmlFile = layout.buildDirectory.file("reports/kover/report.xml")
      }
      html {
        onCheck = true
        htmlDir = layout.buildDirectory.dir("reports/kover/html")
      }
    }
  }
}

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kover)
}

val appleKnowledgeGeneratedDir = layout.buildDirectory.dir("generated/appleknowledge/commonMain/kotlin")
val appleKnowledgeGeneratedFile = appleKnowledgeGeneratedDir.map {
  it.file("com/example/appleknowledge/runtime/AppleOfficialKnowledgeEmbeddedResources.kt")
}
val appleKnowledgeResourceDir = layout.projectDirectory.dir("src/commonMain/resources/appleknowledge")
val appleKnowledgeGeneratorScript = rootProject.layout.projectDirectory.file(
  "scripts/generate-apple-official-knowledge-embedded.py"
)

val generateAppleOfficialKnowledgeEmbeddedResources = tasks.register<Exec>(
  "generateAppleOfficialKnowledgeEmbeddedResources"
) {
  inputs.dir(appleKnowledgeResourceDir)
  inputs.file(appleKnowledgeGeneratorScript)
  outputs.file(appleKnowledgeGeneratedFile)

  commandLine(
    "python3",
    appleKnowledgeGeneratorScript.asFile.absolutePath,
    appleKnowledgeResourceDir.asFile.absolutePath,
    appleKnowledgeGeneratedFile.get().asFile.absolutePath
  )
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
    commonMain {
      kotlin.srcDir(appleKnowledgeGeneratedDir)
      dependencies {
        implementation(libs.kotlinx.serialization.json)
      }
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}

tasks.matching {
  it.name.startsWith("compile") || it.name.startsWith("link") || it.name.endsWith("Test")
}.configureEach {
  dependsOn(generateAppleOfficialKnowledgeEmbeddedResources)
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
          "com.example.ocr.OcrLogExtractor*",
          "com.example.rulepack.RulePackJsonParser*",
          "com.example.rulepack.RulePackValidator*",
          "com.example.rulepack.RulePackDiffCalculator*"
        )
      }
    }
    verify {
      rule("I4 deterministic shared thresholds") {
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

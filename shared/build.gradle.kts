import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kover)
}

val appleKnowledgeGeneratedDir = layout.buildDirectory.dir("generated/appleknowledge/commonMain/kotlin")
val appleKnowledgeResourceDir = layout.projectDirectory.dir("src/commonMain/resources/appleknowledge")

val generateAppleOfficialKnowledgeEmbeddedResources = tasks.register("generateAppleOfficialKnowledgeEmbeddedResources") {
  inputs.dir(appleKnowledgeResourceDir)
  outputs.dir(appleKnowledgeGeneratedDir)

  doLast {
    val sourceDir = appleKnowledgeResourceDir.asFile
    val outputFile = appleKnowledgeGeneratedDir.get()
      .file("com/example/appleknowledge/runtime/AppleOfficialKnowledgeEmbeddedResources.kt")
      .asFile

    val capabilityName = "apple_official_knowledge_capabilities_v1.json"
    val sourceNames = listOf(
      "apple_official_knowledge_sources_v1_part1.json",
      "apple_official_knowledge_sources_v1_part2.json",
      "apple_official_knowledge_sources_v1_part3.json"
    )
    val cardNames = listOf(
      "apple_official_knowledge_cards_v1_part1.json",
      "apple_official_knowledge_cards_v1_part2a.json",
      "apple_official_knowledge_cards_v1_part2b.json",
      "apple_official_knowledge_cards_v1_part2c.json",
      "apple_official_knowledge_cards_v1_part2d.json",
      "apple_official_knowledge_cards_v1_part3a.json",
      "apple_official_knowledge_cards_v1_part3b.json",
      "apple_official_knowledge_cards_v1_part3c.json",
      "apple_official_knowledge_cards_v1_part3d.json",
      "apple_official_knowledge_cards_v1_part4a.json",
      "apple_official_knowledge_cards_v1_part4b.json",
      "apple_official_knowledge_cards_v1_part4c.json",
      "apple_official_knowledge_cards_v1_part4d.json",
      "apple_official_knowledge_cards_v1_part5a.json",
      "apple_official_knowledge_cards_v1_part5b.json",
      "apple_official_knowledge_cards_v1_part5c.json",
      "apple_official_knowledge_cards_v1_part5d.json"
    )

    fun rawLiteral(fileName: String): String {
      val text = sourceDir.resolve(fileName).readText()
      require(!text.contains("\"\"\"")) { "$fileName contains an unsupported triple quote" }
      require(!text.contains('$')) { "$fileName contains an unsupported dollar sign" }
      return "\"\"\"$text\"\"\""
    }

    outputFile.parentFile.mkdirs()
    outputFile.writeText(
      buildString {
        appendLine("package com.example.appleknowledge.runtime")
        appendLine()
        appendLine("internal object AppleOfficialKnowledgeEmbeddedResources {")
        appendLine("    fun bundle(): AppleOfficialKnowledgeResourceBundle = AppleOfficialKnowledgeResourceBundle(")
        appendLine("        capabilitiesJson = ${rawLiteral(capabilityName)},")
        appendLine("        sourceJsonParts = listOf(")
        sourceNames.forEachIndexed { index, name ->
          append("            ${rawLiteral(name)}")
          appendLine(if (index == sourceNames.lastIndex) "" else ",")
        }
        appendLine("        ),")
        appendLine("        cardJsonParts = listOf(")
        cardNames.forEachIndexed { index, name ->
          append("            ${rawLiteral(name)}")
          appendLine(if (index == cardNames.lastIndex) "" else ",")
        }
        appendLine("        )")
        appendLine("    )")
        appendLine("}")
      }
    )
  }
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

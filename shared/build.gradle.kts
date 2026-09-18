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
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}

kover {
  reports {
    verify {
      rule("I1 shared scaffold thresholds") {
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

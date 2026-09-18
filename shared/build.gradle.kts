import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kover)
}

kotlin {
  android {
    namespace = "com.aistudio.paniclab.shared"
    compileSdk = 36
    minSdk = 24

    withHostTestBuilder {}.configure {}

    compilerOptions.configure {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  val iosTargets = listOf(
    iosArm64(),
    iosSimulatorArm64()
  )

  iosTargets.forEach { target ->
    target.binaries.framework {
      baseName = "Shared"
      isStatic = true
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
  }
}

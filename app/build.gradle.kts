import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

val envFile = rootProject.file(".env")
val envProperties = Properties()
if (envFile.exists()) {
  envFile.forEachLine { line ->
    val trimmed = line.trim()
    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
      val key = trimmed.substringBefore("=").trim()
      val value = trimmed.substringAfter("=").trim()
      envProperties.setProperty(key, value)
    }
  }
}

android {
  namespace = "com.lx.tracklist"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.lx.tracklist"
    minSdk = 26
    targetSdk = 36
    versionCode = 200
    versionName = "0.2.0"

    val tmdbReadToken = envProperties.getProperty("TMDB_READ_ACCESS_TOKEN")
      ?: envProperties.getProperty("TMDB_API_KEY")
      ?: ""
    val tmdbApiKey = envProperties.getProperty("TMDB_API_KEY") ?: ""
    val malClientId = envProperties.getProperty("MAL_CLIENT_ID") ?: ""
    buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"$tmdbReadToken\"")
    buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    buildConfigField("String", "MAL_CLIENT_ID", "\"$malClientId\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: project.findProperty("KEYSTORE_PATH") as? String
      val keystoreFile = if (keystorePath != null) {
        val f = file(keystorePath)
        if (f.exists()) f else rootProject.file(keystorePath)
      } else {
        val localApp = file("release.jks")
        val localRoot = rootProject.file("release.jks")
        if (localApp.exists()) localApp else if (localRoot.exists()) localRoot else null
      }

      if (keystoreFile != null && keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("KEYSTORE_PASSWORD")
          ?: project.findProperty("KEYSTORE_PASSWORD") as? String
          ?: "lxtracklist123"
        keyAlias = System.getenv("KEY_ALIAS")
          ?: project.findProperty("KEY_ALIAS") as? String
          ?: "lxtracklist"
        keyPassword = System.getenv("KEY_PASSWORD")
          ?: System.getenv("KEYSTORE_PASSWORD")
          ?: project.findProperty("KEY_PASSWORD") as? String
          ?: project.findProperty("KEYSTORE_PASSWORD") as? String
          ?: "lxtracklist123"
        enableV1Signing = true
        enableV2Signing = true
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseKeystore = signingConfigs.getByName("release")
      signingConfig = if (releaseKeystore.storeFile != null && releaseKeystore.storeFile!!.exists()) {
        releaseKeystore
      } else {
        signingConfigs.getByName("debug")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Coil (Image Loading)
  implementation(libs.coil.compose)

  // Gson (JSON Import/Export)
  implementation(libs.gson)

  // OkHttp (Networking & Remote Metadata Sync)
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")

  // Custom Tabs & Security for OAuth
  implementation("androidx.browser:browser:1.8.0")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}

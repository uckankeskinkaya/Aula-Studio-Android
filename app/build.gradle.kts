plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()

android {
    namespace = "com.aulaf75.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aulaf75.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 100 + buildNumber
        versionName = "0.2.$buildNumber"
        // Ship only the languages the app is translated into.
        resourceConfigurations += listOf("en", "tr", "zh-rCN")
    }

    // Fixed sideload signing key so every CI build installs over the previous one.
    // It is NOT a Play Store key; maintainers publishing elsewhere should use their own.
    signingConfigs {
        create("sideload") {
            storeFile = file("aula-f75-sideload.jks")
            storePassword = "aulaf75android"
            keyAlias = "aulaf75"
            keyPassword = "aulaf75android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("sideload")
        }
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                // Screenshot tests write PNGs only when explicitly asked (CI release job).
                it.systemProperty("roborazzi.test.record", System.getProperty("roborazzi.test.record") ?: "false")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation(bom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

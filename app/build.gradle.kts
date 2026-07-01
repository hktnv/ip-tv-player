import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? {
    return localProperties.getProperty(name)
        ?: providers.environmentVariable(name).orNull
}

val hasPersonalReleaseSigning = listOf(
    "IPTVBOX_PERSONAL_STORE_FILE",
    "IPTVBOX_PERSONAL_STORE_PASSWORD",
    "IPTVBOX_PERSONAL_KEY_ALIAS",
    "IPTVBOX_PERSONAL_KEY_PASSWORD",
).all { !signingValue(it).isNullOrBlank() }

android {
    namespace = "com.hktnv.iptvbox"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hktnv.iptvbox"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 26
        versionName = "0.1.25"
    }

    signingConfigs {
        if (hasPersonalReleaseSigning) {
            create("personalRelease") {
                storeFile = file(signingValue("IPTVBOX_PERSONAL_STORE_FILE")!!)
                storePassword = signingValue("IPTVBOX_PERSONAL_STORE_PASSWORD")
                keyAlias = signingValue("IPTVBOX_PERSONAL_KEY_ALIAS")
                keyPassword = signingValue("IPTVBOX_PERSONAL_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("personal") {
            dimension = "distribution"
            applicationIdSuffix = ".personal"
            versionNameSuffix = "-personal"
            if (hasPersonalReleaseSigning) {
                signingConfig = signingConfigs.getByName("personalRelease")
            }
        }
        create("play") {
            dimension = "distribution"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:player"))
    implementation(project(":core:security"))
    implementation(project(":data:playlist"))
    implementation(project(":domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

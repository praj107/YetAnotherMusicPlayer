import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val versionPropertiesFile = rootProject.file("version.properties")
val versionProperties = Properties().apply {
    versionPropertiesFile.inputStream().use { load(it) }
}

fun Properties.requiredInt(key: String): Int =
    getProperty(key)?.toIntOrNull()
        ?: error("Missing or invalid integer for $key in ${versionPropertiesFile.path}")

val versionMajor = versionProperties.requiredInt("VERSION_MAJOR")
val versionMinor = versionProperties.requiredInt("VERSION_MINOR")
val versionPatch = versionProperties.requiredInt("VERSION_PATCH")

// Load keystore properties if available
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(propertyName: String, envName: String): String? =
    providers.environmentVariable(envName).orNull
        ?: keystoreProperties.getProperty(propertyName)

val releaseStoreFile = signingValue("storeFile", "YAMP_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "YAMP_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "YAMP_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "YAMP_KEY_PASSWORD")
val releaseStoreFileHandle = releaseStoreFile?.let { file(it) }
val hasReleaseSigningConfig = listOf(
    releaseStoreFileHandle?.takeIf { it.exists() }?.path,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.yamp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yamp"
        minSdk = 26
        targetSdk = 35
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = checkNotNull(releaseStoreFileHandle)
                storePassword = checkNotNull(releaseStorePassword)
                keyAlias = checkNotNull(releaseKeyAlias)
                keyPassword = checkNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image Loading
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.room.testing)
}

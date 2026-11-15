import io.sentry.android.gradle.extensions.InstrumentationFeature
import java.util.EnumSet

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms.googleServices)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.realm)
    alias(libs.plugins.apollo)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.sentry)
}

apollo {
    service("service") {
        packageName.set("com.austinhodak.thehideout")
        codegenModels.set("responseBased")
        introspection {
            endpointUrl.set("https://api.tarkov.dev/graphql")
            schemaFile.set(file("src/main/graphql/com/austinhodak/thehideout/schema.graphqls"))
        }
    }
}

android {
    namespace = "com.austinhodak.thehideout"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.austinhodak.thehideout"
        minSdk = 24
        targetSdk = 36

        versionName = System.getenv("VERSION") ?: "1.6.7"
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 17020
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../TheHideoutKeystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") //?: gradleLocalProperties(rootDir).getProperty("storePassword")
            keyAlias = System.getenv("KEYSTORE_ALIAS") //?: gradleLocalProperties(rootDir).getProperty("keyAlias")
            keyPassword = System.getenv("KEYSTORE_ALIAS_PASSWORD") //?: gradleLocalProperties(rootDir).getProperty("keyPassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        named("debug").configure {
            // applicationIdSuffix = ".debug"
        }
        named("release").configure {
            isDebuggable = false
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions.add("version")
    productFlavors {
        create("beta") {
            dimension = "version"
            versionNameSuffix = " Beta"
        }
        create("full") {
            dimension = "version"
        }
    }
}

sentry {
    // Mapping upload needs the CI secrets; skip it for local release builds.
    autoUploadProguardMapping.set(System.getenv("SENTRY_AUTH_TOKEN") != null)
    tracingInstrumentation {
        enabled.set(true)
        features.set(EnumSet.allOf(InstrumentationFeature::class.java) - InstrumentationFeature.DATABASE)
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(androidx.compose.bom)
    implementation(androidx.browser)
    implementation(project(":tarkovapi"))
    implementation(platform(androidx.compose.bom))
    implementation(platform(libs.kotlin.bom))

    implementation("com.google.firebase:firebase-appcheck-debug")

    api(libs.kotlinx.datetime)

    implementation(libs.kpermissions)
    implementation(libs.kpermissions.coroutines)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    implementation(androidx.bundles.camera)
    implementation(androidx.bundles.lifecycle)

    implementation(androidx.tracing)

    implementation(libs.realm)

    implementation(libs.sentry)

    implementation(libs.oss.licenses)

    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)

    implementation(androidx.compose.ui.tooling.preview)

    implementation(libs.gson)
    implementation(libs.glide)

    // Tests
    testImplementation(libs.junit4)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.espresso.core)
    androidTestImplementation(androidx.compose.ui.test.junit4)

    // Core
    implementation(androidx.core)

    // Hilt
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(androidx.hilt.work)
    ksp(androidx.hilt.compiler)

    implementation(androidx.compose.ui)
    implementation(androidx.compose.material)
    implementation(androidx.compose.ui.tooling)
    implementation(androidx.activity.compose)
    implementation(androidx.compose.runtime.livedata)

    implementation(androidx.appcompat)
    implementation(androidx.recyclerview)
    implementation(androidx.constraintlayout)
    implementation(androidx.annotation)
    implementation(androidx.navigation.fragment)
    implementation(androidx.navigation.compose)
    implementation(androidx.navigation.ui)
    implementation(androidx.fragment)

    api(platform(libs.firebase.bom))
    api(libs.bundles.firebase)
    implementation(libs.mlkit.text)

    implementation(libs.firebase.auth.ui)
    // Drawer

    implementation(libs.drawer)

    // END Drawer

    // Dialogs
    implementation(libs.bundles.afollestad.dialogs)
    // END Dialogs

    implementation(libs.skydoves.only)

    api(libs.timber)

    implementation(libs.lottie.compose)

    implementation(libs.bundles.google.play)

    implementation(libs.stfalcon.imageviewer)

    implementation(libs.bundles.maps)

    implementation(libs.compose.markdown)

    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")

    implementation(androidx.compose.material.icons.extended)

    implementation(libs.qonversion)
    implementation(libs.bundles.mavericks)
    implementation(androidx.core.splashscreen)
    implementation(androidx.compose.ui.text.googleFonts)
    implementation(androidx.compose.animation.graphics)

    implementation(androidx.compose.material3)

    // screen modules
    implementation(libs.bundles.preferences.screen)
    implementation(libs.bundles.compose.dialogs)
}

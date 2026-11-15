plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlin.compose).apply(false)
    alias(libs.plugins.dagger.hilt.android).apply(false)
    alias(libs.plugins.ksp).apply(false)
    alias(libs.plugins.gms.googleServices).apply(false)
    alias(libs.plugins.firebase.crashlytics).apply(false)
    alias(libs.plugins.realm).apply(false)
    alias(libs.plugins.firebase.perf).apply(false)
    alias(libs.plugins.room).apply(false)
    alias(libs.plugins.apollo).apply(false)
    alias(libs.plugins.sentry).apply(false)
    alias(libs.plugins.ktlint)
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                // Avoid having to stutter experimental annotations all over the codebase
                "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
                "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
                "-opt-in=androidx.compose.runtime.ExperimentalComposeApi",
                "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
                "-opt-in=kotlin.ExperimentalUnsignedTypes",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.InternalCoroutinesApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            )
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        // Existing violations live in config/ktlint/baseline.xml; only new code is checked.
        baseline.set(rootProject.file("config/ktlint/baseline-${project.name}.xml"))
    }
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// La versione sta in version.properties: la aggiorna lo script di pubblicazione.
val version = Properties().apply { rootProject.file("version.properties").inputStream().use(::load) }

android {
    namespace = "com.funcouple.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.funcouple.app"
        minSdk = 26
        targetSdk = 36
        versionCode = version.getProperty("VERSION_CODE").toInt()
        versionName = version.getProperty("VERSION_NAME")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Firmata con la chiave di debug di questo computer: basta per installarla e
            // aggiornarla a mano, non per pubblicarla su uno store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}

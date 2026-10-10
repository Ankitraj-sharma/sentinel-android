import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val releaseSigning = Properties().apply {
    val config = rootProject.file("keystore.properties")
    if (config.isFile) config.inputStream().use { load(it) }
}
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !releaseSigning.getProperty(it).isNullOrBlank() }
if (gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }) {
    require(hasReleaseSigning) { "Release signing requires local keystore.properties; see docs/release-distribution.md" }
    require(providers.gradleProperty("isolatedValidation").orNull != "true") {
        "Distribution releases must use the standard application ID"
    }
}
android {
    namespace = "com.phishshield.sentinel"
    compileSdk = 35
    defaultConfig {
        val isolated = providers.gradleProperty("isolatedValidation").orNull == "true"
        val suffix = providers.gradleProperty("validationSuffix").orNull ?: "validation"
        require(suffix.matches(Regex("[a-z][a-z0-9]*")))
        applicationId = if (isolated) "com.phishshield.sentinel.$suffix" else "com.phishshield.sentinel"
        resValue("string", "build_app_name", "Sentinel")
        buildConfigField("String", "DEMO_PACKAGE", "\"com.phishshield.sentinel.fixture${if (isolated) ".$suffix" else ""}\"")
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.7.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        if (hasReleaseSigning) create("distribution") {
            storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("distribution")
        }
    }
}
dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation(project(":engine"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

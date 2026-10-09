plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.phishshield.sentinel.fixture"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.phishshield.sentinel.fixture" + if (providers.gradleProperty("isolatedValidation").orNull == "true") ".validation" else ""
        minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
androidComponents { beforeVariants(selector().withBuildType("release")) { it.enable = false } }

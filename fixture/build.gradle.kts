plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.phishshield.sentinel.fixture"
    compileSdk = 35
    defaultConfig {
        val suffix = providers.gradleProperty("validationSuffix").orNull ?: "validation"
        require(suffix.matches(Regex("[a-z][a-z0-9]*")))
        applicationId = "com.phishshield.sentinel.fixture" + if (providers.gradleProperty("isolatedValidation").orNull == "true") ".$suffix" else ""
        minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
androidComponents { beforeVariants(selector().withBuildType("release")) { it.enable = false } }

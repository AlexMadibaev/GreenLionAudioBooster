plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.mgs.greenlionbooster"
 compileSdk = 35
 defaultConfig { applicationId = "com.mgs.greenlionbooster"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "0.3" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}

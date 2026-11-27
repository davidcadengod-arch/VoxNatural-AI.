plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.voxnatural.app"
 compileSdk = 35
 androidResources { noCompress.addAll(listOf("onnx", "bin")) }
 defaultConfig {
  applicationId = "com.voxnatural.app"
  minSdk = 26
  targetSdk = 35
  versionCode = 2
  versionName = "3.0-beta1"
  ndk { abiFilters += "arm64-v8a" }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation(files("libs/sherpa.aar")); implementation("com.tom-roush:pdfbox-android:2.0.27.0") }

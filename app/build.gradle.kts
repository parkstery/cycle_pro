import java.util.Properties
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    kotlin("android")
}

val targetApplicationId = "com.liveonsoft.cyclepro"
val googleServicesFile = file("google-services.json")
val hasMatchingGoogleServicesClient = googleServicesFile.exists() &&
    googleServicesFile.readText().contains("\"package_name\": \"$targetApplicationId\"")

if (hasMatchingGoogleServicesClient) {
    apply(plugin = "com.google.gms.google-services")
}

val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val authWebClientId = localProps.getProperty("rtw.auth.webClientId", "TODO_WEB_CLIENT_ID")
val authFirebaseProjectId = localProps.getProperty("rtw.auth.firebaseProjectId", "TODO_FIREBASE_PROJECT_ID")
val mapApiKey = localProps.getProperty("rtw.map.apiKey", "TODO_MAPS_API_KEY")
val hasGoogleServicesJson = hasMatchingGoogleServicesClient
val buildGitSha = runCatching {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    stdout.toString().trim()
}.getOrDefault("unknown")

android {
    namespace = "com.liveonsoft.cyclepro"
    compileSdk = 35

    defaultConfig {
        applicationId = targetApplicationId
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AUTH_WEB_CLIENT_ID", "\"$authWebClientId\"")
        buildConfigField("String", "AUTH_FIREBASE_PROJECT_ID", "\"$authFirebaseProjectId\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapApiKey\"")
        buildConfigField("boolean", "HAS_GOOGLE_SERVICES_JSON", hasGoogleServicesJson.toString())
        buildConfigField("String", "BUILD_GIT_SHA", "\"$buildGitSha\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.1")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

tasks.register("printRuntimeIntegrationStatus") {
    group = "verification"
    description = "Print runtime integration readiness and blockers"
    doLast {
        val authConfigured = !authWebClientId.contains("TODO", ignoreCase = true) &&
            !authFirebaseProjectId.contains("TODO", ignoreCase = true) &&
            authWebClientId.isNotBlank() &&
            authFirebaseProjectId.isNotBlank()
        val mapConfigured = !mapApiKey.contains("TODO", ignoreCase = true) && mapApiKey.isNotBlank()
        val blockers = mutableListOf<String>()
        if (!googleServicesFile.exists()) {
            blockers += "missing app/google-services.json"
        } else if (!hasMatchingGoogleServicesClient) {
            blockers += "google-services.json package mismatch (expected $targetApplicationId)"
        }
        if (!authConfigured) blockers += "auth config placeholder (rtw.auth.webClientId / rtw.auth.firebaseProjectId)"
        if (!mapConfigured) blockers += "map api key placeholder (rtw.map.apiKey)"

        println("=== RTW Runtime Integration Status ===")
        println("googleServicesJsonPresent: $hasGoogleServicesJson")
        println("authConfigured: $authConfigured")
        println("mapApiKeyConfigured: $mapConfigured")
        if (blockers.isEmpty()) {
            println("integrationGatePassed: true")
            println("blockers: (none)")
        } else {
            println("integrationGatePassed: false")
            println("blockers:")
            blockers.forEach { println(" - $it") }
        }
    }
}

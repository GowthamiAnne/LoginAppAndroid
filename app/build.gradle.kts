plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    jacoco
}

jacoco {
    toolVersion = "0.8.10" // latest stable
}


android {
    namespace = "com.dvt.login"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dvt.login"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest") // Run unit tests first

    reports {
        xml.required.set(true)      // for CI tools
        html.required.set(true)     // human-readable report
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoHtml"))
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*"
    )

    val debugTree = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin"
        )
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec")
        }
    )
}




dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.3") // latest stable



    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.3")

        // Unit testing
        testImplementation("junit:junit:4.13.2")
        testImplementation("org.mockito:mockito-core:5.11.0")

        // AndroidX Test - JUnit Runner
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test:runner:1.5.2")
        androidTestImplementation("androidx.test:rules:1.5.0")
        androidTestImplementation("androidx.test:core:1.5.0")

        // Espresso
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
        androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
        androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

    // Espresso core for Android UI tests
    androidTestImplementation ("androidx.test:runner:1.5.2")


    // Compose UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.1")

    // Optional: coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")




}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("com.google.protobuf") version "0.9.6"
}

android {
    namespace = "com.tomcvt.goready"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tomcvt.goready"
        minSdk = 24
        targetSdk = 36
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"

    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "env"

    productFlavors {

        create("prod") {
            dimension = "env"
            applicationId = "com.tomcvt.goready"
            versionNameSuffix = ""

            buildConfigField(
                "Boolean",
                "IS_ALARM_TEST",
                "false"
            )
        }

        create("alarmTest1") {
            dimension = "env"
            applicationId = "com.tomcvt.goready.alarmtest1"
            versionNameSuffix = "-alarmtest1"

            buildConfigField(
                "Boolean",
                "IS_ALARM_TEST",
                "true"
            )
        }

        create("alarmTest2") {
            dimension = "env"
            applicationId = "com.tomcvt.goready.alarmtest2"
            versionNameSuffix = "-alarmtest2"

            buildConfigField(
                "Boolean",
                "IS_ALARM_TEST",
                "true"
            )
        }

        protobuf {
            protoc {
                artifact = "com.google.protobuf:protoc:3.25.1"
            }

            generateProtoTasks {
                all().forEach { task ->
                    task.builtins {
                        create("java") {
                            option("lite")
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.places)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.emoji2)
    kapt(libs.androidx.room.compiler)
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
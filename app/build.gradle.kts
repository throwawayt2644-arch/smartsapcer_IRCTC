plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.meilluer.smartspacer_irctc"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.meilluer.smartspacer_irctc"
        minSdk = 32
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "GEMINI_API_KEY", "\"\"") // Placeholder
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {

            val keystorePath = System.getenv("CM_KEYSTORE_PATH")
            val keystorePassword = System.getenv("CM_KEYSTORE_PASSWORD")
            val keyAliasEnv = System.getenv("CM_KEY_ALIAS")
            val keyPasswordEnv = System.getenv("CM_KEY_PASSWORD")

            if (
                keystorePath != null &&
                keystorePassword != null &&
                keyAliasEnv != null &&
                keyPasswordEnv != null
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.jakarta.mail)
    implementation(libs.jakarta.activation)
    implementation(libs.jsoup)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.generativeai)

    implementation("com.kieronquinn.smartspacer:sdk-plugin:1.1")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
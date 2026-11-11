plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "tn.mizan.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "tn.mizan.app"
        minSdk = 26
        targetSdk = 35
        // Numéro de compilation GitHub Actions : chaque APK publié a un code plus grand que le précédent.
        // Le tag publié « v0.13.N » correspond au code 100 + N (voir data/Updates.kt).
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
        versionCode = 100 + run
        versionName = "0.13.$run"
    }

    // Signature fixe : sans elle, chaque compilation aurait une clé différente et Android refuserait
    // d'installer la mise à jour par-dessus l'app existante. La clé n'est pas dans le dépôt :
    // la compilation GitHub la reçoit par les secrets MIZAN_KEYSTORE_B64 et MIZAN_KEYSTORE_PASSWORD.
    val keystore = file("signing/al-mizan.jks")
    val keystorePassword = System.getenv("MIZAN_KEYSTORE_PASSWORD")
    val fixedSigning = keystore.exists() && !keystorePassword.isNullOrEmpty()
    signingConfigs {
        if (fixedSigning) {
            create("almizan") {
                storeFile = keystore
                storePassword = keystorePassword
                keyAlias = "almizan"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        debug {
            if (fixedSigning) signingConfig = signingConfigs.getByName("almizan")
        }
        release {
            // Version publiée : non débogable (Play Protect se méfie des APK de test).
            // Sans la clé fixe (secrets absents), on signe avec la clé de test pour que l'APK reste installable.
            signingConfig = if (fixedSigning) signingConfigs.getByName("almizan") else signingConfigs.getByName("debug")
            isDebuggable = false
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
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.documentfile:documentfile:1.0.1")

    // Lecture du texte des tickets, sur le téléphone (modèle latin embarqué, fonctionne hors ligne)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

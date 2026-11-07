import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 비밀값·서명 정보는 리포에 커밋하지 않는 local.properties 에서만 읽는다 (CLAUDE.md Security / DEV_GUIDE §11.4).
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String): String = localProps.getProperty(key)?.trim().orEmpty()

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val hasReleaseKeystore = localProp("RELEASE_STORE_FILE").isNotEmpty()

android {
    namespace = "com.nolleo.app"
    compileSdk {
        version = release(37)
    }
    // AAOS Car API (CarPropertyManager, CarUxRestrictionsManager) — platforms/android-37.0/optional/android.car.jar
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.nolleo.app"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Pleos Playground TTS/STT 서버 자격증명. 비어 있으면 Mode.ON_DEVICE 로 동작한다.
        buildConfigField("String", "PLEOS_CLIENT_ID", localProp("PLEOS_CLIENT_ID").asBuildConfigString())
        buildConfigField("String", "PLEOS_CLIENT_SECRET", localProp("PLEOS_CLIENT_SECRET").asBuildConfigString())
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = file(localProp("RELEASE_STORE_FILE"))
                storePassword = localProp("RELEASE_STORE_PASSWORD")
                keyAlias = localProp("RELEASE_KEY_ALIAS")
                keyPassword = localProp("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            // 서명 키는 리포 밖에 두고 경로만 local.properties 로 지정한다. 없으면 debug 키로 서명(로컬 확인용).
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Pleos Playground SDKs
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navihelper)
    implementation(libs.pleos.tts)
    implementation(libs.pleos.stt)
    implementation(libs.pleos.fusedlocation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // android.jar 의 org.json 은 JVM 단위 테스트에서 stub 이므로 실제 구현을 테스트 클래스패스에만 둔다.
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

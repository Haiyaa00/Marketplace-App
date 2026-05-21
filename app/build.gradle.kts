plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.marketplace"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.marketplace"
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // - Firebase BoM (Tự động quản lý version cho các thư viện Firebase)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // - Navigation Component (Dùng cho kiến trúc 1 Activity + Nhiều Fragment)
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-fragment:$navVersion")
    implementation("androidx.navigation:navigation-ui:$navVersion")

    // - Glide (Thư viện load ảnh)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // - Shimmer (Hiệu ứng khung loading Skeleton lúc chờ load data)
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // - SwipeRefreshLayout (Vuốt từ trên xuống để Refresh Feed)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.cloudinary:cloudinary-android:2.5.0")
}
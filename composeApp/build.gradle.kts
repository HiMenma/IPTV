import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    jvm("desktop")
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                // Networking
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                
                // Navigation
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenmodel)
                implementation(libs.voyager.transitions)
                implementation(libs.voyager.koin)
                
                // DI
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                
                // Image Loading
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                
                // Coroutines
                implementation(libs.kotlinx.coroutines.core)
                
                // Database
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                
                // DataStore
                implementation(libs.androidx.datastore.preferences)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.koin.android)
                
                // Video Player
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.ui)
                implementation(libs.androidx.media3.common)
                implementation(libs.androidx.media3.exoplayer.hls)
                implementation(libs.androidx.media3.exoplayer.dash)
                implementation(libs.androidx.media3.exoplayer.smoothstreaming)
                
                // Database Driver
                implementation(libs.sqldelight.android.driver)
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.okhttp) // Use OkHttp for desktop too or CIO
                
                // Video Player
                implementation(libs.vlcj)
                
                // Database Driver
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotest.property)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.framework.engine)
            }
        }
        
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
                implementation(libs.kotest.runner.junit5)
            }
        }
    }
}

sqldelight {
    databases {
        create("IptvDatabase") {
            packageName.set("com.menmapro.iptv.data.database")
        }
    }
}

android {
    namespace = "com.menmapro.iptv"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.menmapro.iptv"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "IPTV-Player"
            packageVersion = "1.0.0"
            description = "IPTV Player - 支持M3U和Xtream的跨平台IPTV播放器"
            copyright = "© 2024 IPTV Player"
            vendor = "IPTV Player"
            
            macOS {
                bundleID = "com.menmapro.iptv"
                iconFile.set(project.file("src/desktopMain/resources/app_icon.icns"))
            }
            
            windows {
                iconFile.set(project.file("src/desktopMain/resources/app_icon_windows.png"))
                menuGroup = "IPTV Player"
                upgradeUuid = "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d"
            }
            
            linux {
                iconFile.set(project.file("src/desktopMain/resources/app_icon.png"))
            }
        }
    }
}

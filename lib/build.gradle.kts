plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

version = findProperty("uvccamera.version") as String? ?: "0.0.0-SNAPSHOT"

android {
    namespace = "org.uvccamera.lib"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters += mutableSetOf("armeabi-v7a", "arm64-v8a")
            // NDK r28+ defaults to 16 KB page alignment — required for Android 15+
            // https://developer.android.com/ndk/downloads/release_history#28-0
            version = "28.0.13004108"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.support.test.runner)
    androidTestImplementation(libs.support.espresso.core)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }

            groupId = "org.uvccamera"
            artifactId = project.name
            version = project.version.toString()

            pom {
                name = "org.uvccamera:${project.name}"
                description = "USB Video (UVC) Camera Library for Android"
                url = "https://uvccamera.org"

                licenses {
                    license {
                        name = "Apache License Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "saki"
                        email = "t_saki@serenegiant.com"
                    }
                    developer {
                        name = "Alexey Pelykh"
                        email = "alexey.pelykh@gmail.com"
                        organization = "The UVCCamera Project"
                        organizationUrl = "https://uvccamera.org"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/cptskippy/UVCCamera.git"
                    developerConnection = "scm:git:ssh://github.com:cptskippy/UVCCamera.git"
                    url = "https://github.com/cptskippy/UVCCamera"
                }
            }
        }
    }

    repositories {
        maven {
            name = "StagingDeploy"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/cptskippy/UVCCamera")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

plugins {
    kotlin("multiplatform") version "1.4.21"
}

repositories {
    mavenCentral()
}

val isJava8 = JavaVersion.current() == JavaVersion.VERSION_1_8
// if we are using jdk 8 we need to added it to the end of the version
// the default used by mockk is jdk11
val mockkVersion = if (isJava8) "1.10.3-jdk8" else "1.10.3"
kotlin {
    jvm ()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.mockk:mockk:${mockkVersion}")
                implementation("io.mockk:mockk:${mockkVersion}")
            }
        }
        val jvmMain by getting { /* ... */ }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
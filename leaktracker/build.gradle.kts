/*
 * Copyright 2021 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

apply<JacocoPlugin>()

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()

    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Tier 1
    // Apple macOS hosts only:
    macosArm64() // Running tests
    iosSimulatorArm64() // Running tests
    iosArm64()

    // Tier 2
    linuxX64() // Running tests
    linuxArm64()
    // Apple macOS hosts only:
    watchosSimulatorArm64() // Running tests
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64() // Running tests
    tvosArm64()

    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64() // Running tests
    // Apple macOS hosts only:
    watchosDeviceArm64()
    iosX64() // Running tests

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            compileOnly(libs.androidx.annotation)
        }
        jvmTest.dependencies {
            implementation(libs.junit4)
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.SHORT
    }
}

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    val jvmTest = tasks.named<Test>("jvmTest")
    dependsOn(jvmTest)
    executionData(jvmTest.get())

    val jvmCompilation = kotlin.jvm().compilations.getByName("main")
    classDirectories.setFrom(jvmCompilation.output.classesDirs)
    sourceDirectories.setFrom(jvmCompilation.allKotlinSourceSets.flatMap { it.kotlin.srcDirs })

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.named("jvmTest") {
    finalizedBy(jacocoTestReport)
}

tasks.named("check") {
    dependsOn(rootProject.tasks.named("markdownlint"))
}

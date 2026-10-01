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

import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    id("com.appmattus.markdown") version Versions.markdownlintGradlePlugin
    id("com.vanniktech.maven.publish") version Versions.gradleMavenPublishPlugin apply false
    id("org.jetbrains.dokka") version Versions.dokkaPlugin
}

subprojects {
    repositories {
        google()
        mavenCentral()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            allWarningsAsErrors.set(true)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
    }

    version = (System.getenv("GITHUB_REF") ?: System.getProperty("GITHUB_REF"))
        ?.replaceFirst("refs/tags/", "") ?: "unspecified"

    plugins.withType<DokkaPlugin> {
        tasks.withType<DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    skipDeprecated.set(true)

                    if (name.startsWith("ios")) {
                        displayName.set("ios")
                    }

                    sourceLink {
                        localDirectory.set(rootDir)
                        remoteUrl.set(java.net.URL("https://github.com/appmattus/leaktracker/blob/main"))
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }

    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

apply(from = "$rootDir/gradle/scripts/detekt.gradle.kts")
apply(from = "$rootDir/gradle/scripts/dependencyUpdates.gradle.kts")

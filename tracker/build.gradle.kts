import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")
    id("jacoco")
    id("com.github.kt3k.coveralls")

    id("com.novoda.bintray-release")

    id("io.gitlab.arturbosch.detekt") version "1.0.1"
}

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    compileOnly("androidx.annotation:annotation:1.1.0")

    testImplementation("junit:junit:4.12")
}

detekt {
    input = files("$projectDir")
}

tasks.withType<Test> {
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.SHORT
    }
}

tasks.getByName("test").finalizedBy(tasks.getByName("jacocoTestReport"))

tasks.getByName("check").dependsOn(rootProject.tasks.getByName("markdownlint"))

tasks.withType<JacocoReport> {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

coveralls {
    sourceDirs = sourceSets.main.get().allSource.srcDirs.map { it.path }
    jacocoReportPath = "${buildDir}/reports/jacoco/test/jacocoTestReport.xml"
}

tasks.getByName("jacocoTestReport").finalizedBy(tasks.getByName("coveralls"))

tasks.getByName("coveralls").onlyIf { System.getenv("CI")?.isNotEmpty() == true }

publish {
    bintrayUser = System.getenv("BINTRAY_USER") ?: System.getProperty("BINTRAY_USER") ?: "unknown"
    bintrayKey = System.getenv("BINTRAY_KEY") ?: System.getProperty("BINTRAY_KEY") ?: "unknown"

    userOrg = "appmattus"
    groupId = "com.appmattus"
    artifactId = "leaktracker"
    publishVersion = System.getenv("TRAVIS_TAG") ?: System.getProperty("TRAVIS_TAG") ?: "unknown"
    desc = "A memory leak tracking library for Android and Java"
    website = "https://github.com/appmattus/leaktracker"

    dryRun = false
}

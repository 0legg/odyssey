import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.gradle.api.plugins.quality.CheckstyleExtension

import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

buildscript {
    repositories {
        google()
        jcenter()
        maven {
            url = uri("https://maven.fabric.io/public")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.0.1")
        classpath("io.fabric.tools:gradle:1.25.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${deps.versions.kotlin}")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.17.0"
    checkstyle
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("checkstyle")
    }

    tasks {
        val checkstyle by creating(Checkstyle::class) {
            configFile = file("$rootDir/config/checkstyle/checkstyle.xml")
            classpath = files()
            source("src")
        }

        tasks.findByName("check")?.dependsOn(checkstyle)
    }

    extensions.configure(CheckstyleExtension::class.java) {
        isIgnoreFailures = false
        toolVersion = "8.7"
    }

    afterEvaluate {
        // BaseExtension is common parent for application, library and test modules
        extensions.configure(BaseExtension::class.java) {
            compileSdkVersion(deps.android.compileSdkVersion)
            buildToolsVersion(deps.android.buildToolsVersion)
            defaultConfig {
                minSdkVersion(deps.android.minSdkVersion)
                targetSdkVersion(deps.android.targetSdkVersion)
                multiDexEnabled = true
            }
            lintOptions {
                isAbortOnError = true
                disable("UnusedResources") // https://issuetracker.google.com/issues/63150366
                disable("InvalidPackage")
            }
            dexOptions {
                dexInProcess = true
            }
            compileOptions {
                setSourceCompatibility(JavaVersion.VERSION_1_8)
                setTargetCompatibility(JavaVersion.VERSION_1_8)
            }
        }

        extensions.configure(KotlinProjectExtension::class.java) {
            experimental.coroutines = Coroutines.ENABLE
        }
    }

    configurations {
        all {
            exclude(group = "com.google.code.findbugs", module = "jsr305")
        }
    }
}

configurations {
    maybeCreate("ktlint")
}

dependencies {
    "ktlint"("com.github.shyiko:ktlint:0.15.0")
}

tasks {
    "clean"(Delete::class) {
        delete(buildDir)
    }
    "lintKotlin"(JavaExec::class) {
        main = "com.github.shyiko.ktlint.Main"
        classpath = configurations["ktlint"]
        args.addAll(listOf("*/src/**/*.kt"))
    }
}

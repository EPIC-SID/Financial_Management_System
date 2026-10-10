buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.0")
    }
}

plugins {
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
}

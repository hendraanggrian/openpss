import org.gradle.kotlin.dsl.kotlin

plugins {
    `java-library`
    kotlin("jvm")
}

java.sourceSets {
    getByName("main").java.srcDir("src")
}

dependencies {
    compile(kotlin("stdlib", kotlinVersion))
    compile(wp("data", dataVersion))
    compile(hendraanggrian("kotfx", kotfxVersion))
    compile(commonsValidator(commonsValidatorVersion))
}
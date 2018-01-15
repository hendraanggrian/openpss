import org.gradle.kotlin.dsl.kotlin

plugins {
    `java-library`
    kotlin("jvm")
}

java.sourceSets {
    getByName("main").java.srcDir("src")
}

dependencies {
    compile(project(":common"))

    compile(kotlin("stdlib", kotlinVersion))

    compile(joda(jodaVersion))
    compile(hendraanggrian("kotfx", kotfxVersion))
    compile(commonsValidator(commonsValidatorVersion))
}
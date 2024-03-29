plugins {
    `java-library`
    kotlin("jvm")
    idea
    hendraanggrian("r")
    hendraanggrian("buildconfig")
}

group = RELEASE_GROUP
version = RELEASE_VERSION

sourceSets {
    named("main") {
        java.srcDir("src")
        resources.srcDir("res")
    }
    named("test") {
        java.srcDir("tests/src")
        resources.srcDir("tests/res")
    }
}

ktlint()

dependencies {
    implementation(project(":$RELEASE_ARTIFACT"))

    implementation(hendraanggrian("prefy", "prefy", VERSION_PREFY))
    implementation(ktor("client-okhttp"))
    implementation(ktor("client-gson"))
    implementation(arrow("core"))

    implementation(androidx("annotation", version = "1.0.1"))

    testImplementation(kotlin("test-junit", VERSION_KOTLIN))
}

tasks {
    named<com.hendraanggrian.buildconfig.BuildConfigTask>("generateBuildConfig") {
        className = "BuildConfig2"
        appName = RELEASE_NAME
        debug = RELEASE_DEBUG
        artifactId = RELEASE_ARTIFACT
        email = "$RELEASE_USER@gmail.com"
        website = RELEASE_WEBSITE
        addField("USER", RELEASE_USER)
        addField("FULL_NAME", RELEASE_FULL_NAME)
    }

    named<com.hendraanggrian.r.RTask>("generateR") {
        className = "R2"
        resourcesDirectory = "res"
        properties {
            isWriteResourceBundle = true
        }
    }
}

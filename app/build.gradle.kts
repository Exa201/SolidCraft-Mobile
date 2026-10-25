plugins {
    id("com.android.application")
}

android {
    namespace = "org.solidcraft.mobile"
    compileSdk = 37
    ndkVersion = "30.0.16248370"

    defaultConfig {
        applicationId = "org.solidcraft.mobile"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    sourceSets.getByName("main").jniLibs.directories.add("src/main/jniLibs")
}

dependencies {
    implementation("androidx.games:games-activity:4.4.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

val buildRustAndroid = tasks.register<Exec>("buildRustAndroid") {
    workingDir = rootProject.projectDir
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-o", "app/src/main/jniLibs",
        "build", "-p", "solidcraft-android", "--profile", "android",
    )
    inputs.files(
        rootProject.file("Cargo.toml"),
        rootProject.file("Cargo.lock"),
        rootProject.fileTree("apps/solidcraft-android"),
        rootProject.fileTree("crates"),
        rootProject.fileTree("vendor"),
    )
    outputs.dir("src/main/jniLibs/arm64-v8a")
    outputs.dir("src/main/jniLibs/x86_64")
}

tasks.named("preBuild").configure {
    dependsOn(buildRustAndroid)
}

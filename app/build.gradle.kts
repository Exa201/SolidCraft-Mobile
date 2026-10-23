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
    }

    sourceSets.getByName("main").jniLibs.directories.add("src/main/jniLibs")
}

dependencies {
    implementation("androidx.games:games-activity:4.4.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    testImplementation("junit:junit:4.13.2")
}

val buildRustAndroid = tasks.register<Exec>("buildRustAndroid") {
    workingDir = rootProject.projectDir
    commandLine("cargo", "ndk", "-t", "arm64-v8a", "-o", "app/src/main/jniLibs", "build", "-p", "solidcraft-android", "--profile", "android")
    inputs.files(
        rootProject.file("Cargo.toml"),
        rootProject.file("Cargo.lock"),
        rootProject.fileTree("apps/solidcraft-android"),
        rootProject.fileTree("crates"),
        rootProject.fileTree("vendor"),
    )
    outputs.dir("src/main/jniLibs/arm64-v8a")
}

tasks.named("preBuild").configure {
    dependsOn(buildRustAndroid)
}

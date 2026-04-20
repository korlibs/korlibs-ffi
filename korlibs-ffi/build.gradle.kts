plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    // KSP in MPP is target-specific; wire the processor to common metadata and each target.
    add("kspCommonMainMetadata", project(":korlibs-ffi-ksp"))
    listOf(
        "jvm",
        "js",
        "wasmJs",
        "android",
        "linuxX64",
        "linuxArm64",
        "tvosArm64",
        "tvosX64",
        "tvosSimulatorArm64",
        "macosX64",
        "macosArm64",
        "iosArm64",
        "iosSimulatorArm64",
        "iosX64",
        "watchosArm64",
        "watchosArm32",
        "watchosDeviceArm64",
        "watchosSimulatorArm64",
        "mingwX64",
    ).forEach { target ->
        add("ksp${target.replaceFirstChar(Char::uppercaseChar)}", project(":korlibs-ffi-ksp"))
    }
}

tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}
tasks.getByName("sourcesJar").dependsOn("kspCommonMainKotlinMetadata")
//tasks.all {
//    if (this.name.contains("ksp")) {
//        println("task=$this :: ${this::class}")
//    }
//}
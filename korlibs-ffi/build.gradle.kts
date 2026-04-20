plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    // Metadata pass generates common expect declarations.
    add("kspCommonMainMetadata", project(":korlibs-ffi-ksp"))
    // JVM-based targets: user-facing configs are required to enable the KSP task.
    add("kspJvm", project(":korlibs-ffi-ksp"))
    add("kspAndroid", project(":korlibs-ffi-ksp"))
    // JS / WasmJs
    add("kspJs", project(":korlibs-ffi-ksp"))
    add("kspWasmJs", project(":korlibs-ffi-ksp"))
}

val kspProcessorProject = project(":korlibs-ffi-ksp")

// Native targets are enabled via kspKotlin*ProcessorClasspath (no user-facing config exists for them).
configurations.matching {
    it.name.startsWith("kspKotlin") && it.name.endsWith("ProcessorClasspath")
}.configureEach {
    dependencies.add(kspProcessorProject.dependencies.create(kspProcessorProject))
}

tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

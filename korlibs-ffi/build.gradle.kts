plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    // Metadata pass generates common expect declarations.
    add("kspCommonMainMetadata", project(":korlibs-ffi-ksp"))
}

val kspProcessorProject = project(":korlibs-ffi-ksp")

// Kotlin 2.3 + KSP tasks consume kspKotlin*ProcessorClasspath.
configurations.matching {
    it.name.startsWith("kspKotlin") && it.name.endsWith("ProcessorClasspath")
}.configureEach {
    dependencies.add(kspProcessorProject.dependencies.create(kspProcessorProject))
}

tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

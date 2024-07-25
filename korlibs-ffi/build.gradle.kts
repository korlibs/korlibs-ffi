plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":korlibs-ffi-ksp"))
}

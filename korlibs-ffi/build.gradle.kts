import com.google.devtools.ksp.gradle.*

plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":korlibs-ffi-ksp"))
}

tasks.withType(KspTask::class) {
    //println("task=$this")
    if (this.name != "kspCommonMainKotlinMetadata") {
        this.dependsOn("kspCommonMainKotlinMetadata")
    }
}
//tasks.all {
//    if (this.name.contains("ksp")) {
//        println("task=$this :: ${this::class}")
//    }
//}
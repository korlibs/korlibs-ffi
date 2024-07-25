package korlibs.ffi.ksp

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class FFIBuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return FFIBuilderProcessor(environment)
    }
}

private interface MetadataPlatformInfo : PlatformInfo {
    companion object : MetadataPlatformInfo
    override val platformName: String get() = "Metadata"
}

private class FFIBuilderProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    val codeGenerator: CodeGenerator = environment.codeGenerator
    val logger: KSPLogger = environment.logger
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val isCommon = environment.platforms.size >= 2
        val mainPlatform = if (isCommon) MetadataPlatformInfo else environment.platforms.first()
        val isJvm = if (!isCommon && mainPlatform is JvmPlatformInfo) true else false
        val isNative = if (!isCommon && mainPlatform is NativePlatformInfo) true else false
        val isJs = if (!isCommon && mainPlatform is JsPlatformInfo) true else false

        val casts = when {
            isJvm -> jnaCasts
            isNative -> knativeCasts
            isJs -> denoCasts
            else -> defaultCasts
        }

        for (sym in resolver.getSymbolsWithAnnotation("korlibs.ffi.FFI")) {
            if (sym is KSClassDeclaration) {
                if (sym.parentDeclaration != null) {
                    logger.error("Only FFI top-level declarations are supported: ${sym.qualifiedName?.asString()}")
                }
                val packageName = sym.packageName.asString()
                val classNameIfc = sym.sname
                val classNameIfcQualified = sym.qualifiedName!!.asString()
                val classNameImpl = "${classNameIfc}_FFIImpl"
                codeGenerator.createNewFile(Dependencies(false, sym.containingFile!!), packageName, "$classNameImpl.${mainPlatform.platformName}").use {
                    val visibility = sym.getVisibility().name.lowercase()
                    it.bufferedWriter().use {
                        if (isNative) {
                            it.appendLine("@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)")
                        }
                        it.appendLine("package $packageName")
                        if (isNative) {
                            it.appendLine()
                            it.appendLine("import kotlinx.cinterop.*")
                        }
                        it.appendLine()
                        if (isCommon) {
                            it.appendLine("$visibility fun ${sym.sname}(): $classNameIfcQualified = $classNameImpl()")
                        }
                        val expectActual = if (isCommon) "expect" else "actual"
                        val onlyActual = if (isCommon) "" else "actual"
                        it.appendLine("$visibility $expectActual class $classNameImpl${if (isCommon) "()" else " actual constructor()"} : $classNameIfcQualified {")
                        if (isJs) {
                            it.appendLine("  private val __all__$classNameImpl = __load_$classNameImpl()")
                            it.appendLine("  private val __$classNameImpl = __all__$classNameImpl.symbols")
                        }
                        for (func in sym.getDeclaredFunctions()) {
                            val params = func.parameters.asString()
                            val paramsCall = func.parameters.asCallString(casts)
                            val body = when {
                                isCommon -> ""
                                isJvm || isNative || isJs -> {
                                    " = ${casts.cast("__$classNameImpl.${func.sname}($paramsCall)", func.returnType)}"
                                }
                                else -> " = TODO()"
                            }
                            it.appendLine("  $onlyActual override fun ${func.sname}($params): ${func.returnType.asString()}$body")
                        }
                        when {
                            isCommon -> it.appendLine("  $onlyActual override fun close()")
                            isJs -> it.appendLine("  $onlyActual override fun close() { __all__$classNameImpl.close() }")
                            else -> it.appendLine("  $onlyActual override fun close() = Unit")
                        }
                        it.appendLine("}")

                        val ffiAnnotation = sym.annotations.firstOrNull { it.shortName.getShortName() == "FFI" } ?: error("ERROR: FFI annotation not found")
                        val libs = ffiAnnotation.arguments.associate { it.sname to it.value.toString().takeIf { it.isNotBlank() } }
                        val libraryNameWin = libs["windowsLib"] ?: libs["commonLib"] ?: "msvcrt"
                        val libraryNameMac = libs["macosLib"] ?: libs["commonLib"] ?: "/usr/lib/libSystem.dylib"
                        val libraryNameLinux = libs["linuxLib"] ?: libs["commonLib"] ?: "libc"

                        val callbackTypes = arrayListOf<KSTypeReference>()
                        for (func in sym.getDeclaredFunctions()) {
                            for (param in func.parameters) {
                                if (param.type.asString().startsWith("FFIFunctionRef<")) {
                                    callbackTypes += param.type
                                }
                            }
                        }

                        when {
                            isJvm -> {
                                it.appendLine("private inline fun com.sun.jna.Pointer.toFFIPointer() = FFIPointer(com.sun.jna.Pointer.nativeValue(this))")
                                it.appendLine("private inline fun FFIPointer.toPointer() = com.sun.jna.Pointer.createConstant(this.address)")
                                it.appendLine("private object __$classNameImpl : com.sun.jna.Library {")
                                for (func in sym.getDeclaredFunctions()) {
                                    val params = func.parameters.asString(casts)
                                    it.appendLine("  external fun ${func.sname}($params): ${casts.typeProcessor(func.returnType)}")
                                }
                                it.appendLine("  init {")
                                it.appendLine("    when {")
                                it.appendLine("      com.sun.jna.Platform.isWindows() -> com.sun.jna.Native.register(\"$libraryNameWin\")")
                                it.appendLine("      com.sun.jna.Platform.isMac() -> com.sun.jna.Native.register(\"$libraryNameMac\")")
                                it.appendLine("      else -> com.sun.jna.Native.register(\"$libraryNameLinux\")")
                                it.appendLine("    }")
                                it.appendLine("  }")
                                it.appendLine("}")
                            }
                            isNative -> {
                                it.appendLine("fun COpaquePointer?.toFFIPointer(): FFIPointer = FFIPointer(this.rawValue.toLong())")
                                it.appendLine("fun FFIPointer.toPointer(): COpaquePointer? = address.toCPointer()")

                                it.appendLine("private object __$classNameImpl {")
                                it.appendLine("  val __LIB__ = platform.windows.LoadLibraryW(\"$libraryNameWin\")")

                                for (func in sym.getDeclaredFunctions()) {
                                    it.appendLine("  val ${func.sname} by lazy { val funcName = \"${func.sname}\"; platform.windows.GetProcAddress(__LIB__, funcName)?.reinterpret<CFunction<(${func.parameters.asTypeString(casts)}) -> ${func.returnType.asString(casts)}>>() ?: error(\"Can't find ${'$'}funcName\") }")
                                }
                                it.appendLine("}")
                            }
                            isJs -> {
                                it.appendLine("fun DenoPointer_to_FFIPointer(v: dynamic): FFIPointer = FFIPointer(js(\"Deno.UnsafePointer.value(v)\").toString().toLong())")
                                it.appendLine("fun FFIPointer_to_DenoPointer(v: FFIPointer): dynamic { val vv = v.address.toString(); return js(\"Deno.UnsafePointer.create(BigInt(vv))\") }")

                                it.appendLine("private fun  __load_$classNameImpl() = js(\"\"\"")
                                it.appendLine("  (typeof Deno === 'undefined') ? {} : Deno.dlopen(Deno.build.os === 'windows' ? '$libraryNameWin' : Deno.build.os === 'darwin' ? '$libraryNameMac' : '$libraryNameLinux', {")
                                for (func in sym.getDeclaredFunctions()) {
                                    it.appendLine("    \"${func.sname}\": { parameters: [${func.parameters.joinToString(", ") {
                                        "\"" + jsType(it.type) + "\""
                                    }}], result: \"${jsType(func.returnType)}\" },")
                                }
                                it.appendLine("  })")
                                it.appendLine("\"\"\")")
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
        return emptyList()
    }

    // `void`, `bool`, `u8`, `i8`, `u16`, `i16`, `u32`, `i32`, `u64`, `i64`, `usize`, `isize`, `f32`, `f64`, `pointer`, `buffer`, `function`, `struct`
    private fun jsType(type: KSTypeReference?): String = when (type.asString()) {
        "Unit" -> "void"
        "Boolean" -> "bool"
        "UByte" -> "u8"
        "Byte" -> "i8"
        "UShort" -> "u16"
        "Char" -> "u16"
        "Short" -> "i16"
        "UInt" -> "u32"
        "Int" -> "i32"
        "ULong" -> "u64"
        "Long" -> "i64"
        "Float" -> "f32"
        "Double" -> "f64"
        "FFIPointer" -> "pointer"
        "String" -> "pointer"
        else -> type.asString()
    }

    val defaultCasts = object : PlatformCasts {}
    val denoCasts = object : PlatformCasts {
        override fun cast(str: String, type: KSTypeReference?): String = type.asString().let { if (it == "FFIPointer") "DenoPointer_to_FFIPointer($str)" else str }
        override fun revCast(str: String, type: KSTypeReference?): String = type.asString().let { if (it == "FFIPointer") "FFIPointer_to_DenoPointer($str)" else str }
    }
    val jnaCasts = object : PlatformCasts {
        override fun typeProcessor(type: KSTypeReference?): String = type.asString().let {
            when {
                it.startsWith("FFIFunctionRef<") -> "com.sun.jna.Pointer"
                it == "FFIPointer" -> "com.sun.jna.Pointer"
                else -> it
            }
        }
        override fun cast(str: String, type: KSTypeReference?): String = type.asString().let { if (it == "FFIPointer") "$str.toFFIPointer()" else str }
        override fun revCast(str: String, type: KSTypeReference?): String = type.asString().let { if (it == "FFIPointer") "$str.toPointer()" else str }
    }
    val knativeCasts = object : PlatformCasts {
        override fun typeProcessor(type: KSTypeReference?): String = type.asString().let { if (it == "FFIPointer") "COpaquePointer?" else it }
        override fun cast(str: String, type: KSTypeReference?): String = type.asString().let { if (it == "FFIPointer") "$str.toFFIPointer()" else str }
        override fun revCast(str: String, type: KSTypeReference?): String = type.asString().let { if (it == "FFIPointer") "$str.toPointer()" else str }
    }
}

private interface PlatformCasts {
    companion object : PlatformCasts
    fun typeProcessor(type: KSTypeReference?): String = type.asString()
    fun cast(str: String, type: KSTypeReference?): String = str
    fun revCast(str: String, type: KSTypeReference?): String = str
}

private val KSValueArgument.sname get() = name?.asString() ?: "<ERROR>"
private val KSDeclaration.sname get() = simpleName.asString()
private val KSDeclaration.qname get() = qualifiedName?.asString() ?: "<ERROR>"
private fun KSTypeReference?.asString(): String = this?.resolve()?.toString() ?: "<ERROR>"
private fun KSTypeReference?.asString(casts: PlatformCasts = PlatformCasts): String = casts.typeProcessor(this)
private fun List<KSValueParameter>.asString(casts: PlatformCasts = PlatformCasts): String = joinToString(", ") { "${it.name?.asString()}: ${casts.typeProcessor(it.type)}" }
private fun List<KSValueParameter>.asTypeString(casts: PlatformCasts = PlatformCasts): String = joinToString(", ") { casts.typeProcessor(it.type) }
private fun List<KSValueParameter>.asCallString(casts: PlatformCasts = PlatformCasts): String = joinToString(", ") {
    casts.revCast("${it.name?.asString()}", it.type)
}

/*

val __TestMathFFI_FFIImpl = js("""
    Deno.dlopen("libc", {
        "cosf": { parameters: ["float"], result: "float" },
        "malloc": { parameters: ["int"], result: "pointer" },
    })
""".trimIndent())

 */
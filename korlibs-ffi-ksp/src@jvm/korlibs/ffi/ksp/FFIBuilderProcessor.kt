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
class FFIBuilderProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    val codeGenerator: CodeGenerator = environment.codeGenerator
    val logger: KSPLogger = environment.logger
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val isCommon = environment.platforms.size >= 2
        val mainPlatform = environment.platforms.first()
        val isJvm = if (!isCommon && mainPlatform is JvmPlatformInfo) true else false
        val isNative = if (!isCommon && mainPlatform is NativePlatformInfo) true else false
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
                        for (func in sym.getDeclaredFunctions()) {
                            val params = func.parameters.asString()
                            val paramsCall = func.parameters.asCallString()
                            val body = when {
                                isCommon -> ""
                                isJvm -> " = __$classNameImpl.${func.sname}(${func.parameters.asCallString(::jnaCastRev)})${jnaCast("", func.returnType)}"
                                isNative -> " = __$classNameImpl.${func.sname}($paramsCall)"
                                else -> " = TODO()"
                            }
                            it.appendLine("  $onlyActual override fun ${func.sname}($params): ${func.returnType.asString()}$body")
                        }
                        it.appendLine("}")

                        val ffiAnnotation = sym.annotations.firstOrNull { it.shortName.getShortName() == "FFI" } ?: error("ERROR: FFI annotation not found")
                        val libs = ffiAnnotation.arguments.associate { it.sname to it.value.toString().takeIf { it.isNotBlank() } }
                        val libraryNameWin = libs["windowsLib"] ?: libs["commonLib"] ?: "msvcrt"
                        val libraryNameMac = libs["macosLib"] ?: libs["commonLib"] ?: "/usr/lib/libSystem.dylib"
                        val libraryNameLinux = libs["linuxLib"] ?: libs["commonLib"] ?: "libc"

                        when {
                            isJvm -> {
                                it.appendLine("private inline fun com.sun.jna.Pointer.toFFIPointer() = FFIPointer(com.sun.jna.Pointer.nativeValue(this))")
                                it.appendLine("private inline fun FFIPointer.toPointer() = com.sun.jna.Pointer.createConstant(this.address)")
                                it.appendLine("private object __$classNameImpl : com.sun.jna.Library {")
                                for (func in sym.getDeclaredFunctions()) {
                                    val params = func.parameters.asString(::jnaTypeProcessor)
                                    it.appendLine("  external fun ${func.sname}($params): ${jnaTypeProcessor(func.returnType)}")
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
                                it.appendLine("private object __$classNameImpl {")
                                it.appendLine("  val __LIB__ = platform.windows.LoadLibraryW(\"$libraryNameWin\")")

                                for (func in sym.getDeclaredFunctions()) {
                                    it.appendLine("  val ${func.sname} by lazy { val funcName = \"${func.sname}\"; platform.windows.GetProcAddress(__LIB__, funcName)?.reinterpret<CFunction<(${func.parameters.asTypeString()}) -> ${func.returnType.asString()}>>() ?: error(\"Can't find ${'$'}funcName\") }")
                                }
                                it.appendLine("}")
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
        return emptyList()
    }

    val KSValueArgument.sname get() = name?.asString() ?: "<ERROR>"
    val KSDeclaration.sname get() = simpleName.asString()
    val KSDeclaration.qname get() = qualifiedName?.asString() ?: "<ERROR>"
    fun KSTypeReference?.asString(): String = this?.resolve()?.toString() ?: "<ERROR>"
    fun List<KSValueParameter>.asString(processor: (KSTypeReference) -> String = ::defaultTypeProcessor): String = joinToString(", ") { "${it.name?.asString()}: ${processor(it.type)}" }
    fun List<KSValueParameter>.asTypeString(processor: (KSTypeReference) -> String = ::defaultTypeProcessor): String = joinToString(", ") { processor(it.type) }
    fun List<KSValueParameter>.asCallString(processor: (str: String, type: KSTypeReference?) -> String = ::defaultCast): String = joinToString(", ") {
        processor("${it.name?.asString()}", it.type)
    }
    fun defaultTypeProcessor(type: KSTypeReference): String = type.asString()
    fun jnaTypeProcessor(type: KSTypeReference?): String = type.asString().let {
        if (it == "FFIPointer") "com.sun.jna.Pointer" else it
    }
    fun defaultCast(str: String, type: KSTypeReference?): String = str
    fun jnaCast(str: String, type: KSTypeReference?): String = type.asString().let {
        if (it == "FFIPointer") "$str.toFFIPointer()" else str
    }
    fun jnaCastRev(str: String, type: KSTypeReference?): String = type.asString().let {
        if (it == "FFIPointer") "$str.toPointer()" else str
    }
}
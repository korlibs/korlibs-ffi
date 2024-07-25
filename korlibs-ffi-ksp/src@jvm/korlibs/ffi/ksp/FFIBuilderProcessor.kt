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
        val isJvm = if (mainPlatform is JvmPlatformInfo) true else false
        for (sym in resolver.getSymbolsWithAnnotation("korlibs.ffi.FFI")) {
            if (sym is KSClassDeclaration) {
                if (sym.parentDeclaration != null) {
                    logger.error("Only FFI top-level declarations are supported: ${sym.qualifiedName?.asString()}")
                }
                val packageName = sym.packageName.asString()
                val classNameIfc = sym.sname
                val classNameIfcQualified = sym.qualifiedName!!.asString()
                val classNameImpl = "${classNameIfc}_FFIImpl"
                codeGenerator.createNewFile(Dependencies(false, sym.containingFile!!), packageName, classNameImpl).use {
                    val visibility = sym.getVisibility().name.lowercase()
                    it.bufferedWriter().use {
                        it.appendLine("package $packageName")
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
                            it.appendLine("  $onlyActual override fun ${func.sname}($params): ${func.returnType.asString()}" + (if (isCommon) "" else " {"))
                            if (!isCommon) {
                                when {
                                    isJvm -> it.appendLine("    return __$classNameImpl.${func.sname}($paramsCall)")
                                    else -> it.appendLine("    TODO()")
                                }
                                it.appendLine("  }")
                            }
                        }
                        it.appendLine("}")
                        if (isJvm) {
                            val libraryName = "msvcrt"
                            it.appendLine("private object __$classNameImpl : com.sun.jna.Library {")
                            for (func in sym.getDeclaredFunctions()) {
                                val params = func.parameters.asString()
                                it.appendLine("  external fun ${func.simpleName.asString()}($params): ${func.returnType.asString()}")
                            }
                            it.appendLine("  init { com.sun.jna.Native.register(\"$libraryName\") }")
                            it.appendLine("}")
                            /*
                            private object Demo : com.sun.jna.Library {
                                external fun cosf(v: Float): Float
                                init {
                                    com.sun.jna.Native.load("m", Demo::class.java)
                                }
                            }
                             */
                        }
                    }
                }
            }
        }
        return emptyList()
    }

    val KSDeclaration.sname get() = simpleName.asString()
    val KSDeclaration.qname get() = qualifiedName?.asString() ?: "<ERROR>"
    fun KSTypeReference?.asString(): String = this?.resolve()?.toString() ?: "<ERROR>"
    fun List<KSValueParameter>.asString(): String = joinToString(", ") { "${it.name?.asString()}: ${it.type.asString()}" }
    fun List<KSValueParameter>.asCallString(): String = joinToString(", ") { "${it.name?.asString()}" }
}
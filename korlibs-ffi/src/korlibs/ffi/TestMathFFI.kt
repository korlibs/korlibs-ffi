package korlibs.ffi

@FFI(commonLib = "libc", macosLib = "/usr/lib/libSystem.dylib", windowsLib = "msvcrt")
internal interface TestMathFFI : AutoCloseable {
    fun cosf(v: Float): Float
    fun malloc(size: Int): FFIPointer
    fun free(ptr: FFIPointer)
    //fun qsort(base: FFIPointer, number: Int, width: Int, compare: FFIFunctionRef<(FFIPointer, FFIPointer) -> Int>)
    //fun puts(str: String): Int
    //fun fputs(str: String, file: FFIPointer): Int

    companion object : TestMathFFI by TestMathFFI() {
        operator fun invoke(): TestMathFFI = TODO()
    }
}

package korlibs.ffi

@FFI(commonLib = "libc", macosLib = "/usr/lib/libSystem.dylib", windowsLib = "msvcrt")
internal interface TestMathFFI {
    fun cosf(v: Float): Float
    fun malloc(size: Int): FFIPointer
    fun free(ptr: FFIPointer)

    companion object : TestMathFFI by TestMathFFI() {
        operator fun invoke(): TestMathFFI = TODO()
    }
}

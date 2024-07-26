package korlibs.ffi.api

annotation class FFI(
    val commonLib: String = "",
    val windowsLib: String = "",
    val linuxLib: String = "",
    val macosLib: String = "",
)

annotation class FFINativeInt

annotation class FFIWideString

inline class FFIPointer(val address: Long) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "FFIPointer(address=0x${address.toHexString()})"
}

class FFIFunctionRef<T : Function<*>>(val func: T) : AutoCloseable {
    var address: Long? = null

    override fun close() {
    }
}

expect val isSupportedFFI: Boolean

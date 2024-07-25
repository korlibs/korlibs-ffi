package korlibs.ffi

inline class FFIPointer(val address: Long) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "FFIPointer(address=0x${address.toHexString()})"
}

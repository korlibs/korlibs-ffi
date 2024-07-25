package korlibs.ffi

@FFI()
internal interface DemoFFI {
    fun cosf(v: Float): Float

    companion object : DemoFFI by DemoFFI() {
        operator fun invoke(): DemoFFI = TODO()
    }
}

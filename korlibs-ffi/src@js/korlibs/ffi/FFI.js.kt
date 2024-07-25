package korlibs.ffi

actual val isSupportedFFI: Boolean get() = js("(typeof Deno != 'undefined')")

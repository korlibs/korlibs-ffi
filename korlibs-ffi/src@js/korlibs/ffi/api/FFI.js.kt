package korlibs.ffi.api

actual val isSupportedFFI: Boolean get() = js("(typeof Deno != 'undefined')")

package korlibs.ffi.api

actual fun FFIDLOpen(name: String): COpaquePointer? = dlopen(name, platform.posix.RTLD_LAZY or 1)
actual fun FFIDLClose(lib: COpaquePointer?): Unit { dlclose(lib) }
actual fun FFIDLSym(lib: COpaquePointer?, name: String): COpaquePointer? = dlsym(lib, name)

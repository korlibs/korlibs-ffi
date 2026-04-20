@file:OptIn(ExperimentalForeignApi::class)

package korlibs.ffi.api

import kotlinx.cinterop.*
import platform.posix.*

actual fun FFIDLOpen(name: String): COpaquePointer? = platform.posix.dlopen(name, platform.posix.RTLD_LAZY or 1)
actual fun FFIDLClose(lib: COpaquePointer?): Unit { platform.posix.dlclose(lib) }
actual fun FFIDLSym(lib: COpaquePointer?, name: String): COpaquePointer? = platform.posix.dlsym(lib, name)

internal actual fun transferMemory(address: Long, data: ByteArray, offset: Int, size: Int, toPointer: Boolean) {
    if (size == 0) return
    data.usePinned {
        val arrayPtr = it.addressOf(offset)
        val pointerPtr = address.toCPointer<ByteVar>()
        if (toPointer) {
            memcpy(arrayPtr, pointerPtr, size.convert())
        } else {
            memcpy(pointerPtr, arrayPtr, size.convert())
        }
    }
}

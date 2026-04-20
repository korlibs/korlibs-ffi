@file:OptIn(ExperimentalForeignApi::class)

package korlibs.ffi.api

import kotlinx.cinterop.*
import platform.posix.memcpy
import platform.windows.*

actual fun FFIDLOpen(name: String): COpaquePointer? = LoadLibraryW(name)?.reinterpret()
actual fun FFIDLClose(lib: COpaquePointer?): Unit { FreeLibrary(lib?.reinterpret()) }
actual fun FFIDLSym(lib: COpaquePointer?, name: String): COpaquePointer? = GetProcAddress(lib?.reinterpret(), name)?.reinterpret()

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

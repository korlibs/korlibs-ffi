@file:OptIn(ExperimentalForeignApi::class)

package korlibs.ffi.api

import kotlinx.cinterop.*

actual val isSupportedFFI: Boolean = true

expect fun FFIDLOpen(name: String): COpaquePointer?
expect fun FFIDLClose(lib: COpaquePointer?): Unit
expect fun FFIDLSym(lib: COpaquePointer?, name: String): COpaquePointer?

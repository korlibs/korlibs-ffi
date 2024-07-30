@file:OptIn(ExperimentalForeignApi::class)

package korlibs.ffi.api

import kotlinx.cinterop.*

actual val isSupportedFFI: Boolean = true

@OptIn(ExperimentalForeignApi::class)
expect fun FFIDLOpen(name: String): COpaquePointer?
@OptIn(ExperimentalForeignApi::class)
expect fun FFIDLClose(lib: COpaquePointer?): Unit
@OptIn(ExperimentalForeignApi::class)
expect fun FFIDLSym(lib: COpaquePointer?, name: String): COpaquePointer?

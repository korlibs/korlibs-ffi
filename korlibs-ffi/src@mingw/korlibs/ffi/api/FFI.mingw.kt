@file:OptIn(ExperimentalForeignApi::class)

package korlibs.ffi.api

import kotlinx.cinterop.*
import platform.windows.*

actual fun FFIDLOpen(name: String): COpaquePointer? = LoadLibraryW(name)?.reinterpret()
actual fun FFIDLClose(lib: COpaquePointer?): Unit { FreeLibrary(lib?.reinterpret()) }
actual fun FFIDLSym(lib: COpaquePointer?, name: String): COpaquePointer? = GetProcAddress(lib?.reinterpret(), name)?.reinterpret()

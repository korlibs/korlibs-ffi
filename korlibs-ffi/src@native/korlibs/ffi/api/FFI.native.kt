@file:OptIn(ExperimentalForeignApi::class)

package korlibs.ffi.api

import kotlinx.cinterop.*
import kotlin.experimental.*

actual val isSupportedFFI: Boolean = true

expect fun FFIDLOpen(name: String): COpaquePointer?
expect fun FFIDLClose(lib: COpaquePointer?): Unit
expect fun FFIDLSym(lib: COpaquePointer?, name: String): COpaquePointer?
@OptIn(ExperimentalNativeApi::class)
fun FFIDLOpenPlatform(
    common: String? = null,
    linux: String? = null,
    macos: String? = null,
    windows: String? = null,
): COpaquePointer? = FFIDLOpen(when (Platform.osFamily) {
    OsFamily.MACOSX, OsFamily.IOS, OsFamily.TVOS, OsFamily.WATCHOS -> macos ?: common
    OsFamily.LINUX -> linux ?: common
    OsFamily.WINDOWS -> windows ?: common
    OsFamily.WASM, OsFamily.UNKNOWN, OsFamily.ANDROID -> common
} ?: error("Can't find library name"))

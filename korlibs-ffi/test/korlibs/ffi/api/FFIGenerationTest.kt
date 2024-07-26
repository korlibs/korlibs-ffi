package korlibs.ffi.api

import kotlin.test.*

class FFIGenerationTest {
    @Test
    fun test() {
        if (!isSupportedFFI) return

        TestMathFFI().use { lib ->
            assertEquals(1f, lib.cosf(0f))
            //lib.puts("hello world\r\n")
            val ptr = lib.malloc(40)
            //lib.qsort(ptr, 10, 4, FFIFunctionRef { l, r -> 0 })
            println(ptr)
            lib.free(ptr)
            println("AFTER FREE: $ptr")
        }
    }
}

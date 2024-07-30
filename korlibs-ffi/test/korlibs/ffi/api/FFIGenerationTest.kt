package korlibs.ffi.api

import kotlin.test.*

class FFIGenerationTest {
    @Test
    fun test() {
        if (!isSupportedFFI) {
            println("Skipping FFIGenerationTest.test since FFI is not supported in this target")
            return
        }

        TestMathFFI().use { lib ->
            assertEquals(1f, lib.cosf(0f))
            //lib.puts("hello world\r\n")
            val ptr = lib.malloc(40)
            //lib.qsort(ptr, 10, 4, FFIFunctionRef { l, r -> 0 })

            lib.fopen("TEST.BIN", "wb").also { file ->
                if (file != FFIPointer.NULL) {
                    lib.fclose(file)
                }
            }
            lib.remove("TEST.BIN")

            println(ptr)
            lib.free(ptr)
            println("AFTER FREE: $ptr")
        }
    }
}

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
            var old = 0
            for (n in 0 until 10) {
                ptr.setI32(n * 4, old)
                old = old xor (n * 777777777)
            }
            lib.qsort(ptr, 10, 4, FFIFunctionRef { l, r -> l.getI32().compareTo(r.getI32()) })
            assertEquals(
                listOf(-2027935736, -1487162319, -1317266793, -167789696, -100679232, 0, 0, 777777777, 1083181060, 1928074899),
                (0 until 10).map { ptr.getI32(it * 4) }
            )
            println(ptr)
            lib.free(ptr)

            lib.fopen("TEST.BIN", "wb").also { file ->
                if (file != FFIPointer.NULL) {
                    lib.fclose(file)
                }
            }
            lib.remove("TEST.BIN")

            println("AFTER FREE: $ptr")
        }
    }
}

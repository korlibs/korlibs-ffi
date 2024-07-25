package korlibs.ffi

import kotlin.test.*

class FFIGenerationTest {
    @Test
    fun test() {
        assertEquals(1f, TestMathFFI.cosf(0f))
        val ptr = TestMathFFI.malloc(100)
        println(ptr)
        TestMathFFI.free(ptr)
    }
}

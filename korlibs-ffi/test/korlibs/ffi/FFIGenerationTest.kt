package korlibs.ffi

import kotlin.test.*

class FFIGenerationTest {
    @Test
    fun test() {
        assertEquals(1f, DemoFFI.cosf(0f))
    }
}

package corpus.unknown

import io.kotest.core.spec.style.FunSpec

class NoAssertionSpec : FunSpec({
    test("no assertion") {
        val a = 1
        val b = a + 1
    }
})
package corpus.magic

import io.kotest.core.spec.style.FunSpec

class NonAssertionCallSpec : FunSpec({
    test("passes a number to a domain helper") {
        val ok = verifyInput(3)
        doSomething(ok)
    }
})
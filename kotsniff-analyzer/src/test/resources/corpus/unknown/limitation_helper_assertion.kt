package corpus.unknown

import io.kotest.core.spec.style.FunSpec

class HelperAssertionSpec : FunSpec({
    test("delegates assertion to a helper") {
        val a = compute()
        assertResult(a)
    }
})
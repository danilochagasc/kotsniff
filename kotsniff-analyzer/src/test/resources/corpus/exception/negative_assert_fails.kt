package corpus.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow

class AssertFailsSpec : FunSpec({
    test("asserts failure idiomatically") {
        shouldThrow<IllegalStateException> { risky() }
    }
})
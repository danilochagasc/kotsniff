package corpus.unknown

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WithAssertionSpec : FunSpec({
    test("with assertion") {
        val a = 1
        a shouldBe 1
    }
})
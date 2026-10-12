package corpus.conditional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValueIfSpec : FunSpec({
    test("builds a value") {
        val n = if (flag()) 1 else 2
        n shouldBe 2
    }
})
package corpus.duplicate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DistinctSpec : FunSpec({
    test("distinct") {
        val a = 1
        val b = 2
        a shouldBe 1
        b shouldBe 2
    }
})
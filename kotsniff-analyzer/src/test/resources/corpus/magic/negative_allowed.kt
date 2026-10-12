package corpus.magic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AllowedNumbersSpec : FunSpec({
    test("uses only allowed numbers") {
        val a = 0
        val b = 1
        a shouldBe 0
        b shouldBe 1
    }
})
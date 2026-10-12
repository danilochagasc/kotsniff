package corpus.magic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UnderscoreMagicSpec : FunSpec({
    test("has a magic number with underscore") {
        val t = 1_000
        t shouldBe 1_000
    }
})
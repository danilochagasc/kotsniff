package corpus.magic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IntMagicSpec : FunSpec({
    test("has a magic number") {
        val code = 404
        code shouldBe 404
    }
})
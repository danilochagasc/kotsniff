package corpus.duplicate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DuplicateSpec : FunSpec({
    test("duplicates") {
        val a = 1
        a shouldBe 1
        a shouldBe 1
    }
})
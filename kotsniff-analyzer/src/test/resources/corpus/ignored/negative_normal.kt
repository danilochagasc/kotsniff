package corpus.ignored

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NormalSpec : FunSpec({
    test("runs") {
        1 shouldBe 1
    }
})
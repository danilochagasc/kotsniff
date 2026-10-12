package corpus.roulette

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FewAssertionsSpec : FunSpec({
    test("few assertions") {
        val list = listOf(1, 2)
        list.size shouldBe 2
        list[0] shouldBe 1
    }
})
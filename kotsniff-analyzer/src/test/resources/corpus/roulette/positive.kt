package corpus.roulette

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RouletteSpec : FunSpec({
    test("many assertions") {
        val list = listOf(1, 2, 3)
        list.size shouldBe 3
        list[0] shouldBe 1
        list[1] shouldBe 2
        list[2] shouldBe 3
    }
})
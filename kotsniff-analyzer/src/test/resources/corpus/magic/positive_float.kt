package corpus.magic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FloatMagicSpec : FunSpec({
    test("has a magic float") {
        val price = 10.5
        price shouldBe 10.5
    }
})
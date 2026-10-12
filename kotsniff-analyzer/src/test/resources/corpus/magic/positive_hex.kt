package corpus.magic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HexMagicSpec : FunSpec({
    test("has a hexadecimal magic number") {
        val v = 0xFF
        v shouldBe 0xFF
    }
})
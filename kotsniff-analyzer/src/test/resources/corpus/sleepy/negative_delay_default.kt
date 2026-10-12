package corpus.sleepy

import io.kotest.core.spec.style.FunSpec

class DelaySpec : FunSpec({
    test("delays") {
        delay(500)
    }
})
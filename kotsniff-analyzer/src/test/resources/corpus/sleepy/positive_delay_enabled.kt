package corpus.sleepy

import io.kotest.core.spec.style.FunSpec

class DelayEnabledSpec : FunSpec({
    test("delays") {
        delay(500)
    }
})
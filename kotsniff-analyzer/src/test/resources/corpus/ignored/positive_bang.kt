package corpus.ignored

import io.kotest.core.spec.style.StringSpec

class BangSpec : StringSpec({
    "!disabled test" {
        println("never runs")
    }
})
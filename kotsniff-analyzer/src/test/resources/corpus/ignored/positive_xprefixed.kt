package corpus.ignored

import io.kotest.core.spec.style.FunSpec

class XPrefixedSpec : FunSpec({
    xtest("disabled") {
        println("never runs")
    }
})
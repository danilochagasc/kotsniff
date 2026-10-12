package corpus.conditional

import io.kotest.core.spec.style.FunSpec

class IfStatementSpec : FunSpec({
    test("branches") {
        if (System.currentTimeMillis() > 0) {
            println("after")
        }
    }
})
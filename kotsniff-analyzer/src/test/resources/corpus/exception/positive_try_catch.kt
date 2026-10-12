package corpus.exception

import io.kotest.core.spec.style.FunSpec

class TryCatchSpec : FunSpec({
    test("handles exceptions") {
        try {
            risky()
        } catch (e: Exception) {
            println(e)
        }
    }
})
package corpus.unknown

import io.kotest.core.spec.style.StringSpec
import io.mockk.coVerify

class WithVerifySpec : StringSpec({
    "verifies a mock" {
        coVerify { say("hi") }
    }
})
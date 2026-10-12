package br.com.kotsniff.cli.config

/**
 * Resolution and validation of user-configurable numeric settings, shared by
 * the CLI and the JSON configuration file.
 *
 * Precedence is **CLI > file > default**. A value below [minimum] is rejected
 * with an actionable message; the command turns it into exit code `2`. Values
 * are validated before the scan starts.
 *
 * Rationale for the minima:
 * - `maxAssertions` must be `>= 1`: `0` would report every test that contains
 *   any assertion as Assertion Roulette, which is not a meaningful setting.
 * - `reportMaxLines` must be `>= 1`: `0` would embed no source at all.
 */
object NumericOptions {
    const val MIN_MAX_ASSERTIONS = 1
    const val DEFAULT_MAX_ASSERTIONS = 3
    const val MIN_REPORT_MAX_LINES = 1
    const val DEFAULT_REPORT_MAX_LINES = 2000

    /**
     * Resolves an effective value from CLI and file inputs and validates it.
     *
     * @throws IllegalArgumentException when the resolved value is below [minimum].
     */
    fun resolve(
        label: String,
        cliValue: Int?,
        configValue: Int?,
        default: Int,
        minimum: Int,
    ): Int {
        val value = cliValue ?: configValue ?: default
        require(value >= minimum) { "Invalid $label: $value (must be >= $minimum)." }
        return value
    }

    /** Resolves `maxAssertions` with the documented precedence and validation. */
    fun resolveMaxAssertions(
        cliValue: Int?,
        configValue: Int?,
    ): Int = resolve("maxAssertions", cliValue, configValue, DEFAULT_MAX_ASSERTIONS, MIN_MAX_ASSERTIONS)

    /** Resolves `reportMaxLines` with the documented precedence and validation. */
    fun resolveReportMaxLines(
        cliValue: Int?,
        configValue: Int?,
    ): Int = resolve("reportMaxLines", cliValue, configValue, DEFAULT_REPORT_MAX_LINES, MIN_REPORT_MAX_LINES)
}

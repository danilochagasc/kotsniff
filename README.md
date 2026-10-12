# KotSniff

KotSniff is a static analyser that detects **test smells** in Kotlin test suites
written with **Kotest** and **MockK**. It parses Kotlin source into PSI (no type
resolution needed) and reports actionable diagnostics in several formats.

> Status: research/engineering tool. The detectors are evaluated against a
> hand-annotated reference corpus (see [Reference corpus](#reference-corpus)).
> It is **not** a compiler and does not perform whole-program analysis.

## Requirements

- JDK 17 (the build pins the toolchain to 17)
- No other runtime dependencies; Gradle is provided through the wrapper

## Build and test

```bash
./gradlew check          # compile + tests + lint (kotlinter)
./gradlew test           # tests only
./gradlew lintKotlin     # style checks
./gradlew formatKotlin   # auto-format
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Install

```bash
./gradlew :kotsniff-cli:installDist        # creates kotsniff-cli/build/install/kotsniff
./gradlew :kotsniff-cli:distZip            # creates kotsniff-cli/build/distributions/kotsniff-<version>.zip
./gradlew :kotsniff-cli:installCli         # copies installDist to ~/.kotsniff and updates PATH
```

## Usage

```bash
kotsniff -p <directory> [options]
```

| Option | Description | Default |
|---|---|---|
| `-p, --path <dir>` | Directory to analyse | `.` |
| `-f, --format <fmt>` | `markdown`, `json`, `text`, `html`, `sarif` | `markdown` |
| `-o, --output <file>` | Report path | `<dir>/kotsniff-report.<ext>` |
| `--show-details` | Print each finding to the terminal | off |
| `--max-assertions <n>` | Threshold for Assertion Roulette (must be `>= 1`) | `3` |
| `--report-delay` | Report `delay(...)` as Sleepy Test (opt-in) | off |
| `-x, --exclude <glob>` | Skip files matching a glob (repeatable) | — |
| `--disable-rule <id>` | Disable a rule by id/name (repeatable) | — |
| `--severity <rule>=<level>` | Override a rule severity: `RULE=LEVEL` (repeatable) | rule default |
| `--configure-severity` | Interactive (arrow keys) severity editor; saves to `kotsniff.json` | — |
| `--list-rules` | List rules with their default severity and exit | — |
| `--allowed-number <n>` | Add a numeric literal to the magic-number allowlist (repeatable) | `0,1,100` |
| `--progress` | Show a live progress line during long analyses | off |
| `--report-source <mode>` | HTML source detail: `full` (whole file) or `snippet` | `full` |
| `--report-max-lines <n>` | Max lines embedded per file before context windowing (HTML; must be `>= 1`) | `2000` |
| `--config <file>` | Path to a JSON config file | `<dir>/kotsniff.json` |
| `--no-fail` | Exit `0` even when smells are found | off |
| `--no-color` | Disable ANSI colours | off |
| `-q, --quiet` | Only print errors and the report path | off |
| `--version` | Print the version and exit | — |

`build/`, `.gradle/`, `.git/` and `node_modules/` are excluded by default.

### Exit codes

| Code | Meaning |
|---|---|
| `0` | Analysis succeeded; no smells found (or `--no-fail` was given) |
| `1` | Smells were found |
| `2` | The command could not run (invalid target, malformed config) |

### Configuration file

Optional `kotsniff.json` (or `--config`):

```json
{
  "exclude": ["**/generated/**"],
  "disabledRules": ["MAGIC_NUMBER_TEST"],
  "maxAssertions": 5,
  "reportDelay": false,
  "allowedNumbers": ["0", "1", "100", "404"],
  "reportMaxLines": 2000,
  "severities": {
    "MAGIC_NUMBER_TEST": "high",
    "Unknown Test": "info"
  }
}
```

Numeric settings (`maxAssertions`, `reportMaxLines`) must be `>= 1`; invalid values
(including negative numbers and zero) are rejected before the scan with exit code
`2`. Precedence is **CLI > file > default**: `--max-assertions`/`--report-max-lines`
override the file, which overrides the built-in default.

Severity precedence: **`--severity` (CLI) > `severities` (file) > rule default**.
Rule ids/names are case-insensitive; levels are `info`, `low`, `medium`, `high`.
Run `kotsniff --list-rules` to see the defaults, or `kotsniff --configure-severity`
to edit them interactively (↑/↓ move, Enter edit, Esc finish).

Command-line values override the file where both are provided.

## Rules

| Id | Name | Severity | Description |
|---|---|---|---|
| `IGNORED_TEST` | Ignored Test | medium | Disabled/skipped tests (`!name`, `xtest`, `@Ignored`) |
| `SLEEPY_TEST` | Sleepy Test | medium | `Thread.sleep(...)` (and `delay(...)` when enabled) |
| `CONDITIONAL_TEST_LOGIC` | Conditional Test Logic | medium | Statement-position `if`/`when` in a leaf test |
| `EXCEPTION_HANDLING` | Exception Handling | medium | `try/catch` inside a leaf test |
| `UNKNOWN_TEST` | Unknown Test | high | Leaf test with no assertion or mock verification |
| `DUPLICATE_ASSERT` | Duplicate Assert | low | Same assertion repeated in one test |
| `MAGIC_NUMBER_TEST` | Magic Number Test | low | Numeric literal inside an assertion |
| `ASSERTION_ROULETTE` | Assertion Roulette | medium | More than `maxAssertions` assertions in one test |

## Detection model

KotSniff analyses **leaf tests** (a terminal block such as `test { }`, `it { }`,
`then { }`, `scenario { }`, `"name" { }`, or an `@Test` function). Container
blocks (`describe`, `context`, `given`, `feature`, the infix `"x" - { }` /
`"x" should { }` / `"x" When { }`) are not inspected directly, so findings never
leak from nested tests into a parent block or between sibling tests.

**Supported Kotest styles:** StringSpec, FunSpec, BehaviorSpec, DescribeSpec,
ShouldSpec, ExpectSpec, FreeSpec, WordSpec, FeatureSpec, AnnotationSpec, including
the `init { … }` form and specs whose base class is declared in the same file
(transitive inheritance within a file).

**What counts as an assertion** (one *assertion unit*):
- a Kotest infix matcher (`actual shouldBe expected`) or dot-qualified matcher
  (`actual.shouldBe(expected)`), where the matcher name starts with `should`;
- a known assertion helper call (`assert`, `assertTrue`, `assertEquals`, `expect`,
  `assertFailsWith`, `fail`, …);
- a MockK verification (`verify`, `coVerify`, `verifyOrder`, `verifySequence`,
  `confirmVerified`, …).

Nested assertions inside an assertion expression are **not** counted again, so a
compound helper such as `assertSoftly { … }` counts as a **single** logical
assertion (its numeric literals are still inspected by Magic Number). MockK
*stubbing* (`every`/`coEvery`) is not an assertion. Assertions nested inside
ordinary calls or lambdas (e.g. `list.forEach { it shouldBe 1 }`) are still found.

Known limitations of the model:

- Recognition is **syntactic** (PSI, no type resolution): a domain method whose
  name starts with `should` (e.g. `shouldRefund()`) or matches a known helper is
  counted as an assertion.
- Assertions hidden in a **helper function** called from the test are not visible
  and are reported as `UNKNOWN_TEST`.
- A lambda argument is assumed to **execute**; an assertion that never runs is
  still counted.
- Duplicate Assert compares whitespace-normalised **source text**; it does not
  detect semantically equivalent assertions written differently.

## Reports

- **Console** – summary and, with `--show-details`, per-finding output.
- **JSON** – versioned schema (`schemaVersion`) consumed programmatically.
- **Markdown / text** – human-readable artefacts.
- **HTML** – a single, self-contained file (no network required) with two views:
  a **Files** view (sidebar of files, per-file smell list, full source with
  **built-in Kotlin syntax highlighting** and the smell's line(s) highlighted,
  plus prev/next navigation across findings) and a **List** view
  (searchable/filterable cards). All dynamic content is contextually escaped, and
  no dynamic data is placed into JavaScript.
- **SARIF 2.1.0** – interoperability with code-scanning tools and IDEs.

All formats are produced from the same normalised `Diagnostic` model, so they can
never disagree on rule id, severity or location.

## Reference corpus

The analyser is validated against a hand-annotated corpus in
`kotsniff-analyzer/src/test/resources/corpus/` (see `manifest.csv`). Each fixture
declares its expected findings at **occurrence level** (`RULE@line:column`); the
`CorpusRegressionTest` compares the actual finding set against it, so a missing
finding, an unexpected finding, a wrong location, a wrong rule id or a duplicate
finding all fail the test. Occurrences prefixed with `~` document knowingly
unsupported behaviour: they are asserted as regression guards but excluded from
the metrics.

Per-rule precision/recall/F1 are computed from occurrence-level matching and
written to `kotsniff-analyzer/build/corpus-metrics.csv`. The zero-denominator
convention is explicit: precision (resp. recall) is `1.0` when there is nothing
to predict (resp. observe).

These are **synthetic regression metrics** — conformance to hand-written
expectations on a small corpus — and **not** evidence of precision or recall on
real projects. Empirical validity requires a separate study against real
repositories and is out of scope here.

## Known limitations

- No whole-program analysis: an assertion performed inside a helper function is
  not visible, so such tests are reported as `UNKNOWN_TEST` (documented in the
  corpus as a limitation).
- Test inheritance is resolved only within a single file; a spec whose base
  class lives in another file is still matched by the `*Spec` naming heuristic.
- `delay(...)` (Sleepy Test) is opt-in because it is common in legitimate
  coroutine tests.
- Files with syntax errors are reported as failed and **not** analysed (no
  partial-tree diagnostics).
- Detector recognition is syntactic (PSI); it does not resolve symbols, aliases
  or imports. See [Detection model](#detection-model) for the exact assertion
  and scope rules.
- Ignored tests (`xtest`, `!name`, `@Ignored`) are also counted as `UNKNOWN_TEST`
  when they contain no assertion; suppression across rules is not implemented.
- Experimental Kotlin syntax (e.g. context receivers) is not parseable with the
  default compiler configuration and is reported as a failed file.

## Reproducibility

```bash
./gradlew clean check
cat kotsniff-analyzer/build/corpus-metrics.csv
kotsniff -p <dir> -f json -o report.json
```

## License

MIT — see [LICENSE](LICENSE).

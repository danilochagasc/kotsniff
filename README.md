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
| `--max-assertions <n>` | Threshold for Assertion Roulette | `3` |
| `--report-delay` | Report `delay(...)` as Sleepy Test (opt-in) | off |
| `-x, --exclude <glob>` | Skip files matching a glob (repeatable) | — |
| `--disable-rule <id>` | Disable a rule by id/name (repeatable) | — |
| `--severity <rule>=<level>` | Override a rule severity: `RULE=LEVEL` (repeatable) | rule default |
| `--configure-severity` | Interactive (arrow keys) severity editor; saves to `kotsniff.json` | — |
| `--list-rules` | List rules with their default severity and exit | — |
| `--allowed-number <n>` | Add a numeric literal to the magic-number allowlist (repeatable) | `0,1,100` |
| `--progress` | Show a live progress line during long analyses | off |
| `--report-source <mode>` | HTML source detail: `full` (whole file) or `snippet` | `full` |
| `--report-max-lines <n>` | Max lines embedded per file before context windowing (HTML) | `2000` |
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
  "severities": {
    "MAGIC_NUMBER_TEST": "high",
    "Unknown Test": "info"
  }
}
```

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
`kotsniff-analyzer/src/test/resources/corpus/` (see `manifest.csv`). The
`CorpusRegressionTest` runs every case and fails on any drift. Aggregate
precision/recall/F1 per rule are written to
`kotsniff-analyzer/build/corpus-metrics.csv`.

The corpus is **small and synthetic**: it measures conformance to documented
expectations, not precision/recall on real projects. Cases with a known
limitation are labelled and excluded from the metrics.

## Known limitations

- No whole-program analysis: an assertion performed inside a helper function is
  not visible, so such tests are reported as `UNKNOWN_TEST` (documented in the
  corpus as a limitation).
- Test inheritance is resolved only within a single file; a spec whose base
  class lives in another file is still matched by the `*Spec` naming heuristic.
- `delay(...)` (Sleepy Test) is opt-in because it is common in legitimate
  coroutine tests.
- Files with syntax errors are reported as failed and **not** analysed.
- Detector recognition is syntactic (PSI); it does not resolve symbols, aliases
  or imports.

## Reproducibility

```bash
./gradlew clean check
cat kotsniff-analyzer/build/corpus-metrics.csv
kotsniff -p <dir> -f json -o report.json
```

## License

MIT — see [LICENSE](LICENSE).

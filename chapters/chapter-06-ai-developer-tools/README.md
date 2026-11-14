# Chapter 6: AI Developer Tools, IDEs, and Coding Agents

These optional files support **AI Developer Tools, IDEs, and Coding Agents**. The chapter can be read without running them. They are small experiments, not a deployable order-management application.

## Before you run an example

Download the [chapter-06-v1.0.0 release](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/releases/tag/chapter-06-v1.0.0), extract it, and open this chapter folder. If you cloned the repository instead, open `chapters/chapter-06-ai-developer-tools` from its root. You can use any editor. The folder name is only a way to find the files; it is not part of the business example.

Several files intentionally contain the defect discussed in the chapter. The combined runner expects those checks to fail and expects the corrected checks to pass. A failed check from a deliberately broken version is useful evidence, not a successful application result. Run the combined script to verify the full expected pattern.

## Requirements and one-command run

Use JDK 21 or later, with `java` and `javac` from the same JDK on PATH, and a POSIX shell. The commands work in macOS or Linux; on Windows, use WSL with its own JDK. These scripts are not PowerShell commands. No model account, network service, database, broker or installation is used by the core checks.

Check `java -version` and `javac -version` first. Both must report version 21 or later. From this chapter folder, run:

```sh
sh run_all.sh
```

The script compiles each experiment into its own fresh temporary directory. It prints that directory, keeps command output there, and validates both expected passes and intentional failures. It does not edit the source files. Recorded run output and its environment are in [verified-results](verified-results/README.md). Results are observations of these examples, not measurements of a production system.

## Match the examples to the chapter

| Chapter lesson | Complete sources | Expected observation |
| --- | --- | --- |
| Reverse Engineering an Existing System with AI | `investigation/` | Six baseline assertions pass. Duplicate mode fails because it returns two new confirmations. |
| Implementing a Change with a Coding Agent | `implementation/` | 42 assertions pass in the expanded baseline mode; three pass in duplicate mode. The original output label is retained. |
| Challenge the Checks Before Accepting the Change | `mutations/` | Removing the confirmed-state or cancellation guard compiles, but makes the corresponding checks fail. These are deliberately broken copies. |
| Debug Failures and Find Their Cause | `debugging/` | Five fresh JVM runs reproduce the same controlled outcomes. Independent copies both report new confirmation; atomic acceptance permits one. The lock experiment identifies the owner. Registry counts grow to 40, 80, 120 without cleanup and return to zero with cleanup. |
| Refactor Code and Migrate Interfaces | `refactoring/` | Thirty assertions pass for each version. Original and refactored models produce identical state/result traces for all 1,024 sequences of five operations. The deliberately changed OR predicate fails. |
| Document and Review the Change | `review/` | Flawed caller produces two work entries for repetition and one for cancellation. Corrected caller produces one and zero. |
| Test the Rule and Its Integrations | `testing/` | With real JUnit, the weak test passes the broken and corrected sources. The stronger test catches the removed guard. See the optional commands below. |

The original and corrected `Order.java` have the same class name, so compile them separately. The runner does this automatically. The baseline has an existing, unresolved cancellation-after-confirmation behaviour. Preserving it during refactoring does not approve that policy.

## Individual commands

For example, compile and run the corrected local model from the archive root:

```sh
mkdir -p local-classes
javac --release 21 -d local-classes implementation/Order.java implementation/OrderChecks.java
java -cp local-classes OrderChecks
java -cp local-classes OrderChecks duplicate
```

The confirmation counter in the printed regression excerpt is completed by an assertion. Its full standalone form is `RegressionExcerpt.java`. Compile it with either order version. It fails against the investigation version and passes against the corrected version. Do not run Java with assertions disabled and assume that `assert` checks ran: this example uses explicit `AssertionError` instead.

## Optional JUnit reproduction

The core runner does not download dependencies. `testing/pom.xml` declares JUnit Jupiter 5.11.4, the same real framework used for the chapter's weak/strong comparison. If Maven is already available and you choose to let it obtain these public dependencies, run:

```sh
cd testing
mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath -Dmdep.outputFile=classpath.txt
cd ..
JUNIT_CLASSPATH="$(cat testing/classpath.txt)" sh run_junit.sh
```

Alternatively supply an existing classpath containing Jupiter API/engine 5.11.4, Platform commons/engine 1.11.4, OpenTest4J 1.3.0 and API Guardian 1.1.2. Use the supplied shell commands inside WSL on Windows. `run_junit.sh` uses POSIX paths and the colon classpath separator.

Expected: each variant's weak test passes. The corrected strong test passes; the no-guard strong test fails with NEWLY_CONFIRMED instead of ALREADY_CONFIRMED. Both versions pass the four prerequisite cases. `LocalJUnitRunner` launches the actual engine and returns failure when tests fail; it is not a substitute assertion framework.

## Scope and provenance

The investigation fixture was author-prepared. The corrected two-file example preserves the scoped Claude Code implementation used in the chapter. Debugging, test comparisons, refactoring and caller checks are author-run local experiments, not new agent sessions. `SOURCE_HASHES.json` identifies the exact files in this package. There are no credentials, personal data, account connections or real payment/shipment actions.

The outbox, real-database concurrency, provider-contract and deployment checks in the prose remain proposed integration work. This archive does not establish those production guarantees. MCP configuration in the chapter is a prepared example, not an observed third-party connection.

## Troubleshooting

If `javac` reports that release 21 is not supported, the terminal is using an older JDK. Select an installed JDK 21 or later for that terminal, then check both version commands again. If you see duplicate `Order` classes, compile one variant at a time or use `run_all.sh`, which keeps each variant in its own directory. For a fixture timing failure, inspect the named result log before retrying on a less busy machine; do not remove the assertion to obtain a pass.

The optional JUnit step needs Maven or an existing JUnit classpath. `mvn test` alone does not run this teaching comparison: use the dependency command and `run_junit.sh` above. The custom launcher invokes the real Jupiter engine. It may print a deprecation warning on newer JDKs; the expected test results still determine success.

## Changes and reuse

The [repository licence](../../LICENSE) permits non-commercial learning and educational use under its terms. It is not an MIT licence. Dependency libraries keep their own licences. If an example differs from the chapter, report the chapter heading, release tag, command and observed result in a [repository issue](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/issues). Do not include credentials or customer data.

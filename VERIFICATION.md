# Verification notes

The November 18, 2026 repository update was checked on macOS arm64 with OpenJDK 25.0.4.1, compiling with `javac --release 21`. Gson 2.13.2 supplied the JSON dependency. This establishes compilation against Java 21 APIs; the new examples were executed on Java 25, not separately on a Java 21 runtime.

All 13 newly supplied source files, including the Chapter 15 Dockerfile, were copied without content changes. The existing Chapter 6 sources, licence and workflow were retained.

## Observed local results

The root command `sh run_examples.sh`, with `GSON_JAR` pointing to the dependency, exited successfully.

| Chapter | Observed result |
| --- | --- |
| 2 | Routing checks produced `[A, null, B, null, null, null]`; gateway example compiled |
| 3 | All five catalog cases passed |
| 4 | All invoice proposal cases passed |
| 6 | Core experiments matched expected success and failure outcomes; all 1,024 comparison traces matched |
| 9 | All job ledger cases passed |
| 10 | All loan extension workflow checks passed |
| 11 | All catalog handoff checks passed |
| 12 | All release gate checks passed |
| 13 | All training split audit checks passed |
| 14 | All model route policy checks passed |
| 15 | All 21 checks passed; the demo reported one ticket-creation call after repeated execution |

## Scope of verification

No live gateway or model-provider calls were made. No cloud services were deployed. The Chapter 15 Dockerfile was supplied but was not built during this verification. The optional Chapter 6 JUnit comparison was not rerun for this update; its earlier results and reproduction instructions remain in that chapter's folder.

These results cover the local fixtures and checks supplied with the code. They do not establish production authentication, persistence, concurrency across services or external-provider behaviour.

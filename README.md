# Generative AI for Software Developers, 2nd Edition

This repository contains companion examples for the second edition of **Generative AI for Software Developers**. Use the code to examine the decisions, changes and checks discussed in the book. You can read the chapters without installing or running the examples.

Repository: https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition

## Available material

| Chapter | Companion material | Start here |
| --- | --- | --- |
| 2. Foundation Models and Model Selection for Field Delivery | Local routing checks and an optional live model-gateway comparison | [Chapter 2](chapters/chapter-02/README.md) |
| 3. GenAI Architecture Fundamentals | Validate a catalog draft against approved evidence and select a fallback | [Chapter 3](chapters/chapter-03/README.md) |
| 4. Prompting, Context Engineering, and Structured Outputs | Check an invoice proposal's JSON fields and evidence | [Chapter 4](chapters/chapter-04/README.md) |
| 6. AI Developer Tools, IDEs, and Coding Agents | Order-confirmation experiments covering investigation, implementation, debugging, tests, refactoring and review | [Chapter 6](chapters/chapter-06-ai-developer-tools/README.md) |
| 9. GenAI Application Architecture and Design Patterns | Job leases, retry limits and stale-result prevention | [Chapter 9](chapters/chapter-09/README.md) |
| 10. Agentic AI Architecture and Tool-Using Systems | Equipment-loan extension rules, approvals and duplicate requests | [Chapter 10](chapters/chapter-10/README.md) |
| 11. Multi-Agent Systems, MCP, Interoperability, and AgentOps | Validate a catalog handoff, its task identity and repeated results | [Chapter 11](chapters/chapter-11/README.md) |
| 12. Evaluation, Observability, and Quality Engineering for AI Systems | A release gate using required cases, quality and latency | [Chapter 12](chapters/chapter-12/README.md) |
| 13. Fine-Tuning, Model Customization, and Well-Architected AI Systems | Audit data splits for group overlap and duplicate text | [Chapter 13](chapters/chapter-13/README.md) |
| 14. Enterprise AI Platforms and Governance for Field Teams | Check whether a workload is permitted to use a model route | [Chapter 14](chapters/chapter-14/README.md) |
| 15. Hands-On Field Build | SkillGenie local learning lab, built-in checks and an optional Dockerfile | [Chapter 15](chapters/chapter-15/README.md) |

No runnable companion code is currently supplied for Chapters 1, 5, 7 or 8. Chapter 5's architecture material and Chapters 7 and 8's diagrams, worked scenarios and review checks remain in the book. The book manuscripts and diagrams are not distributed here.

These are focused learning examples with synthetic data. Most use local fixtures and in-memory state. They do not together form a deployed enterprise application, a hosted agent platform or a runnable AWS architecture.

## Download or browse

Download the [current repository ZIP](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/archive/refs/heads/main.zip) and extract it, or clone the repository:

```sh
git clone https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition.git
cd GenAI_For_Dev_2nd_Edition
```

Open the relevant chapter README before running its commands. The `main` branch contains the available chapters and may receive corrections. For the earlier fixed Chapter 6-only release, use [chapter-06-v1.0.0](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/releases/tag/chapter-06-v1.0.0).

## Run the examples

Use **JDK 21 or later**, with both `java` and `javac` on your path. The commands below use a POSIX shell on macOS, Linux or WSL on Windows. Native Windows users can run the individual Java examples with the classpath separator changed from `:` to `;`.

Chapters 2, 3 and 4 use **Gson 2.13.2** for JSON handling. Download it once from Maven Central, from the repository root:

```sh
mkdir -p .deps
curl --fail --location \
  https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar \
  --output .deps/gson-2.13.2.jar
```

The dependency is not included in this repository. See the [Gson project](https://github.com/google/gson) for its documentation and licence. You can also supply an existing copy using the `GSON_JAR` environment variable with its absolute path.

Run all supplied core checks:

```sh
sh run_examples.sh
```

The runner compiles the uploaded examples with Java 21 compatibility, runs their local checks and runs the existing Chapter 6 core experiments. It builds in a temporary directory. It does not download dependencies, contact a model provider or deploy services. Chapter 2's live gateway comparison is **compiled only**; its [README](chapters/chapter-02/README.md) explains the separate opt-in run and required credentials.

Chapter 6 also has an [optional JUnit comparison](chapters/chapter-06-ai-developer-tools/README.md#optional-junit-reproduction), which uses separate Java dependencies. Some Chapter 6 variants are deliberately broken; its runner checks that they fail as expected.

For a single example, follow its chapter README. Chapter 15 additionally supports a scripted demo and an interactive local console.

## Checks and corrections

See [verification notes](VERIFICATION.md) for the checked environment and results. The existing [Chapter 6 GitHub Actions workflow](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/actions/workflows/chapter-06-checks.yml) covers Chapter 6; it does not yet automate the other chapters. A passing local check does not establish production database, messaging or live-provider guarantees.

To report a problem, [open an issue](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/issues) with the chapter, commit or release tag, command, expected result and actual result. Remove credentials, personal information and customer data. For a proposed correction, keep the change focused, run the relevant checks and explain its effect on the example.

## First edition

The [first-edition repository](https://github.com/codebitmaple/GenAIForDev) remains separate. Its chapter numbers and examples differ from this edition.

## Licence

This repository uses the **Learning Purpose Only License**, retaining the first edition's copyright owners, BitMaple, LLC. and Aarya AI, LLC. It permits non-commercial educational and learning use under the terms in [LICENSE](LICENSE). Commercial use requires their written consent. This is not an MIT licence. External dependencies retain their respective licences.

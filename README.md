# Generative AI for Software Developers, 2nd Edition

This repository contains companion examples for the second edition of **Generative AI for Software Developers**. Use the code to examine the decisions, changes and checks discussed in the book. You can read the chapter without installing or running the examples.

## Available material

| Chapter | Companion material | Start here |
| --- | --- | --- |
| 6. AI Developer Tools, IDEs, and Coding Agents | Small Java order-confirmation experiments covering investigation, implementation, debugging, tests, refactoring and code review | [Chapter 6 README](chapters/chapter-06-ai-developer-tools/README.md) |

This first companion release contains Chapter 6. More chapters can be added as their examples are ready. The book manuscripts are not distributed in this repository.

## Download or browse

For the version associated with this companion release, download **Source code (zip)** from [Chapter 6 v1.0.0](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/releases/tag/chapter-06-v1.0.0), then extract it. The release tag gives you a fixed version; the `main` branch may receive later corrections.

If you use Git, you can also clone the repository:

```sh
git clone https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition.git
cd GenAI_For_Dev_2nd_Edition
git switch --detach chapter-06-v1.0.0
```

Open the chapter README before running its commands. It maps the files to chapter headings and describes the expected results.

## Run the Chapter 6 checks

You need JDK 21 or later and a POSIX shell. Use macOS, Linux, or WSL on Windows. Check that `java` and `javac` both use a suitable JDK, then run:

```sh
cd chapters/chapter-06-ai-developer-tools
sh run_all.sh
```

The core checks use no model account, API key, database or cloud service. They do not install dependencies. Some examples are deliberately broken so you can see whether a check catches the defect. The runner verifies both the expected failures and the corrected results.

The [optional JUnit comparison](chapters/chapter-06-ai-developer-tools/README.md#optional-junit-reproduction) uses public Java dependencies. These are small learning experiments with fictional data, not a deployable order-management system.

## Checks and corrections

[Chapter 6 checks](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/actions/workflows/chapter-06-checks.yml) run the core and JUnit comparisons in GitHub Actions. Recorded local results and their environment are included with the chapter. Passing these checks does not establish database, messaging or live-provider guarantees.

To report a problem, [open an issue](https://github.com/bitmaple/GenAI_For_Dev_2nd_Edition/issues) with the release tag, chapter heading, command, expected result and actual result. Remove credentials, personal information and customer data. For a proposed correction, keep the change focused, run the relevant checks and explain the effect on the chapter example.

## First edition

The [first-edition repository](https://github.com/codebitmaple/GenAIForDev) remains separate. Its chapter numbers and examples differ from this edition.

## Licence

This repository uses the **Learning Purpose Only License**, retaining the first edition's copyright owners, BitMaple, LLC. and Aarya AI, LLC. It permits non-commercial educational and learning use under the terms in [LICENSE](LICENSE). Commercial use requires their written consent. This is not an MIT licence. External dependencies retain their respective licences.

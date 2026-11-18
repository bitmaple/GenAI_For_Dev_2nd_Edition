# Chapter 12: Evaluation, Observability, and Quality Engineering for AI Systems

Compare candidate and baseline evaluation rows with a required case manifest. Check missing results, prohibited behaviour, required cases, quality regression and latency before advancing.

## Run the example

Use JDK 21 or later. From `chapters/chapter-12`, run:

```sh
mkdir -p out
javac --release 21 -d out ReleaseGate.java
java -cp out ReleaseGate
```

Expected final output:

```text
All release gate checks passed.
```

The checks are included in `ReleaseGate.java`; a failed check exits with an error.

## What this example covers

The results are synthetic local fixtures. Passing this example does not release a model or run a hosted evaluation service.

[Return to the chapter index](../../README.md#available-material)

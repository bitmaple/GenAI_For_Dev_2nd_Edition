# Chapter 10: Agentic AI Architecture and Tool-Using Systems

Check an equipment-loan extension against the current loan, the requested dates, an exact approval and a previous receipt.

## Run the example

Use JDK 21 or later. From `chapters/chapter-10`, run:

```sh
mkdir -p out
javac --release 21 -d out LoanExtensionWorkflow.java
java -cp out LoanExtensionWorkflow
```

Expected final output:

```text
All loan extension workflow checks passed.
```

The checks are included in `LoanExtensionWorkflow.java`; a failed check exits with an error.

## What this example covers

This is an in-memory equipment-loan example, not a financial lending system. Authentication, persistence and external services are not implemented.

[Return to the chapter index](../../README.md#available-material)

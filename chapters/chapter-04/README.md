# Chapter 4: Prompting, Context Engineering, and Structured Outputs

Check a structured invoice proposal for valid JSON, exact fields, supported currency and an amount backed by the approved invoice evidence.

## Run the example

Use JDK 21 or later. Download the pinned Gson dependency using the [root instructions](../../README.md#run-the-examples) first. From `chapters/chapter-04`, run:

```sh
mkdir -p out
javac --release 21 -cp ../../.deps/gson-2.13.2.jar -d out InvoiceProposal.java
java -cp "out:../../.deps/gson-2.13.2.jar" InvoiceProposal
```

Expected final output:

```text
All invoice proposal cases passed.
```

The checks are included in `InvoiceProposal.java`; a failed check exits with an error.

## What this example covers

This checks a supplied JSON reply. It does not call a model, extract a real invoice or submit a payment.

[Return to the chapter index](../../README.md#available-material)

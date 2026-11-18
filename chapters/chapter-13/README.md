# Chapter 13: Fine-Tuning, Model Customization, and Well-Architected AI Systems

Audit training, validation and test rows for duplicate identifiers, overlapping groups and identical normalized text across splits.

## Run the example

Use JDK 21 or later. From `chapters/chapter-13`, run:

```sh
mkdir -p out
javac --release 21 -d out TrainingSplitAudit.java
java -cp out TrainingSplitAudit
```

Expected final output:

```text
All training split audit checks passed.
```

The checks are included in `TrainingSplitAudit.java`; a failed check exits with an error.

## What this example covers

This is a deterministic dataset check. It does not train a model or detect every form of semantic or near-duplicate leakage.

[Return to the chapter index](../../README.md#available-material)

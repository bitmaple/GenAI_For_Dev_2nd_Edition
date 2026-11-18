# Chapter 9: GenAI Application Architecture and Design Patterns

Follow a job through leases, bounded attempts and publication checks. A worker whose lease has been replaced cannot publish its stale result.

## Run the example

Use JDK 21 or later. From `chapters/chapter-09`, run:

```sh
mkdir -p out
javac --release 21 -d out JobLedger.java
java -cp out JobLedger
```

Expected final output:

```text
All job ledger cases passed.
```

The checks are included in `JobLedger.java`; a failed check exits with an error.

## What this example covers

The ledger is synchronized and held in one process. It does not implement a durable database, distributed lease service or message broker.

[Return to the chapter index](../../README.md#available-material)

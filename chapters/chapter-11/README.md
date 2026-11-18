# Chapter 11: Multi-Agent Systems, MCP, Interoperability, and AgentOps

Accept a catalog draft only when its peer, task, attempt, source revision and result match the expected handoff. Distinguish duplicates from conflicting results.

## Run the example

Use JDK 21 or later. From `chapters/chapter-11`, run:

```sh
mkdir -p out
javac --release 21 -d out CatalogHandoff.java
java -cp out CatalogHandoff
```

Expected final output:

```text
All catalog handoff checks passed.
```

The checks are included in `CatalogHandoff.java`; a failed check exits with an error.

## What this example covers

The caller supplies the authenticated-peer value. This illustrates application contract checks; it is not a running MCP server, A2A transport or identity provider.

[Return to the chapter index](../../README.md#available-material)

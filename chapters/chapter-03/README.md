# Chapter 3: GenAI Architecture Fundamentals

Validate a proposed catalog description against approved product evidence, then use a defined fallback when the proposal is unsuitable.

## Run the example

Use JDK 21 or later. Download the pinned Gson dependency using the [root instructions](../../README.md#run-the-examples) first. From `chapters/chapter-03`, run:

```sh
mkdir -p out
javac --release 21 -cp ../../.deps/gson-2.13.2.jar -d out CatalogDraft.java
java -cp "out:../../.deps/gson-2.13.2.jar" CatalogDraft
```

Expected final output:

```text
All five catalog cases passed.
```

The checks are included in `CatalogDraft.java`; a failed check exits with an error.

## What this example covers

The catalog and generator are local fakes. The example makes no model calls or catalog API requests.

[Return to the chapter index](../../README.md#available-material)

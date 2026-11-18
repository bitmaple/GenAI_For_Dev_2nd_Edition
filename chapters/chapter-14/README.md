# Chapter 14: Enterprise AI Platforms and Governance for Field Teams

Decide whether a workload can use a proposed model route based on identity, route revision, environment, data class, capability and possible processing regions.

## Run the example

Use JDK 21 or later. From `chapters/chapter-14`, run:

```sh
mkdir -p out
javac --release 21 -d out ModelRoutePolicy.java
java -cp out ModelRoutePolicy
```

Expected final output:

```text
All model route policy checks passed.
```

The checks are included in `ModelRoutePolicy.java`; a failed check exits with an error.

## What this example covers

The routes and identities are local fixtures. This does not configure a provider gateway, enforce network policy or verify a real provider region.

[Return to the chapter index](../../README.md#available-material)

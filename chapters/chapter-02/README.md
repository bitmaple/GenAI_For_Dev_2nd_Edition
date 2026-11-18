# Chapter 2: Foundation Models and Model Selection for Field Delivery

`RoutingExample.java` shows a small route-selection policy and controlled fallback. `GatewayComparison.java` sends the same synthetic request to two model routes through a compatible gateway.

## Run the local routing checks

Use JDK 21 or later. From `chapters/chapter-02`, run:

```sh
mkdir -p out
javac --release 21 -d out RoutingExample.java
java -cp out RoutingExample
```

Expected output:

```text
[A, null, B, null, null, null]
```

The program checks each result before printing it. `null` means no permitted route or fallback was selected. This example makes no network calls.

## Compile the optional gateway comparison

Download Gson using the [root instructions](../../README.md#run-the-examples), then run from this chapter folder:

```sh
mkdir -p out
javac --release 21 -cp ../../.deps/gson-2.13.2.jar -d out GatewayComparison.java
```

## Run the live comparison only when you intend to call models

This separate run needs an existing gateway, credentials and access to two different model routes. It sends two requests and may incur provider charges. It is not included in the automatic local checks.

Set these environment variables privately in your terminal or through your secret manager:

| Variable | Meaning |
| --- | --- |
| `EVAL_URL` | HTTPS gateway endpoint whose path ends in `/chat/completions`, without user information, a query string or a fragment |
| `EVAL_A` | First permitted gateway model-route identifier |
| `EVAL_B` | A different permitted model-route identifier |
| `EVAL_TOKEN` | Bearer credential accepted by your gateway |

With these values configured, run:

```sh
java -cp "out:../../.deps/gson-2.13.2.jar" GatewayComparison
```

The program expects a Chat Completions-compatible request and response format. It prints one JSON result per route, including request latency, HTTP status and available response details. Results depend on your gateway and model. A successful compilation is not evidence of a successful live comparison, and the program does not automatically score answer correctness or calculate cost.

Keep tokens and account-specific results out of commits and issues. No credential or live-provider result is included in this repository.

[Return to the chapter index](../../README.md#available-material)

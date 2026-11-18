# Chapter 15: Hands-On Field Build

SkillGenie is a local learning lab for checking a proposed learning plan and carrying out an approved training-ticket request. It demonstrates permitted source selection, plan validation, approval, repeated requests and recovery when a ticket's outcome is uncertain.

`SkillGenie.java` holds the application rules. `SkillGenieLab.java` supplies fictional people and documents, a deterministic planner, an in-memory ticket service, a controllable clock, checks and a console.

## Compile and check

Use JDK 21 or later. No external Java libraries or model account are needed. From `chapters/chapter-15`, run:

```sh
mkdir -p out
javac --release 21 -d out SkillGenie.java SkillGenieLab.java
java -cp out SkillGenieLab --check
```

The check mode prints a `PASS` line for each case and finishes with `Passed 21 checks.` A failed check exits with an error.

## Follow the scripted demo

```sh
java -cp out SkillGenieLab --demo
```

The demo selects a 45-minute plan, previews a training-ticket request, records the manager's approval and executes the request twice. Both executions report the same ticket. The final line is `Ticket create calls: 1`.

## Try the local console

```sh
java -cp out SkillGenieLab --shell
```

Enter these commands one at a time in a fresh console session:

```text
search alex java
plan alex java 45
preview alex P1
approve robin R1
execute alex R1
execute alex R1
audit
quit
```

`alex` is a fictional learner and `robin` is that learner's manager. `P1` and `R1` are identifiers printed by this fresh session. Use the identifiers returned by your session if you create additional plans or requests. Enter `help` to see the other console commands.

## Optional container

The supplied Dockerfile compiles and runs the same local lab. If you have Docker, you can run these commands from this chapter folder:

```sh
docker build -t skillgenie-lab .
docker run --rm skillgenie-lab --check
docker run --rm skillgenie-lab --demo
docker run --rm -it skillgenie-lab --shell
```

Building the image downloads its Eclipse Temurin base images. The Docker commands are optional; verification for this repository update ran the Java commands directly, not a container build.

## What this lab does and does not implement

Source selection and planning are deterministic local operations. The planner does not call an LLM, and the lab does not use embeddings or a vector database. The ticket service and audit list live in memory. Each process starts with fresh fixtures, and state is lost when it exits.

The console accepts fictional actor names so you can explore the rules; it is not an authentication boundary. The lab illustrates application checks but does not deploy a hosted service, connect an enterprise identity provider, or create real tickets.

[Return to the chapter index](../../README.md#available-material)

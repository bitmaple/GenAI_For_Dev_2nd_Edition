import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public final class SkillGenieLab {
    private static final String ALEX = "alex";
    private static final String ROBIN = "robin";
    private static final String MINA = "mina";

    private static final class LabClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T09:00:00Z");

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) {
            Objects.requireNonNull(zone);
            return new Clock() {
                @Override public Instant instant() { return LabClock.this.instant(); }
                @Override public ZoneId getZone() { return zone; }
                @Override public Clock withZone(ZoneId other) {
                    return LabClock.this.withZone(other);
                }
            };
        }

        void advance(Duration duration) { now = now.plus(duration); }
    }

    private static final class TicketDesk implements SkillGenie.Tickets {
        private record Stored(SkillGenie.Terms terms, SkillGenie.Receipt receipt) {}

        private final Map<String, Stored> byKey = new LinkedHashMap<>();
        private int calls;
        private boolean loseReply;
        private boolean failBeforeCommit;

        @Override public SkillGenie.Receipt create(
                String key, SkillGenie.Terms terms) {
            calls++;
            if (failBeforeCommit) {
                throw new SkillGenie.UnknownOutcome();
            }
            Stored existing = byKey.get(key);
            if (existing != null) {
                SkillGenie.require(existing.terms().equals(terms),
                        "KEY_CONFLICT");
                return existing.receipt();
            }
            var receipt = new SkillGenie.Receipt(
                    "T" + (byKey.size() + 1),
                    SkillGenie.TicketState.OPEN);
            byKey.put(key, new Stored(terms, receipt));
            if (loseReply) {
                throw new SkillGenie.UnknownOutcome();
            }
            return receipt;
        }

        @Override public SkillGenie.Receipt find(String key) {
            Stored stored = byKey.get(key);
            return stored == null ? null : stored.receipt();
        }

        @Override public SkillGenie.Receipt cancelOpen(String key) {
            Stored stored = byKey.get(key);
            SkillGenie.require(stored != null, "TICKET_UNAVAILABLE");
            var cancelled = new SkillGenie.Receipt(
                    stored.receipt().id(), SkillGenie.TicketState.CANCELLED);
            byKey.put(key, new Stored(stored.terms(), cancelled));
            return cancelled;
        }
    }

    private record Lab(SkillGenie app, TicketDesk desk, LabClock clock) {}

    private static SkillGenie.Actor alex(String manager, boolean active) {
        return new SkillGenie.Actor(ALEX, "acme", "blue",
                SkillGenie.Role.LEARNER, manager, active);
    }

    private static SkillGenie.Doc javaLab(int revision) {
        return new SkillGenie.Doc(new SkillGenie.Ref("JAVA-LAB", revision),
                "acme", "blue", "java", "Java test lab",
                "Run a failing test and explain the result.",
                "LAB-JAVA", 25, true);
    }

    private static Lab lab() {
        return lab(SkillGenieLab::propose);
    }

    private static Lab lab(SkillGenie.Planner planner) {
        var desk = new TicketDesk();
        var clock = new LabClock();
        var people = List.of(alex(ROBIN, true),
                new SkillGenie.Actor(ROBIN, "acme", "blue",
                        SkillGenie.Role.MANAGER, null, true),
                new SkillGenie.Actor("blair", "acme", "green",
                        SkillGenie.Role.LEARNER, MINA, true),
                new SkillGenie.Actor(MINA, "acme", "green",
                        SkillGenie.Role.MANAGER, null, true),
                new SkillGenie.Actor("sam", "other", "blue",
                        SkillGenie.Role.LEARNER, null, true));
        var sources = List.of(
                new SkillGenie.Doc(new SkillGenie.Ref("JAVA-START", 3),
                        "acme", "*", "java", "Java service starter",
                        "Build the sample service and run its unit tests.",
                        null, 20, true),
                javaLab(2),
                new SkillGenie.Doc(new SkillGenie.Ref("JAVA-TEAM", 1),
                        "acme", "green", "java", "Java team guide",
                        "Review the green team's Java test checklist.",
                        null, 15, true),
                new SkillGenie.Doc(new SkillGenie.Ref("JAVA-OLD", 1),
                        "acme", "*", "java", "Retired starter",
                        "Follow the older build process.", null, 10, false),
                new SkillGenie.Doc(new SkillGenie.Ref("HANDOFF", 1),
                        "acme", "*", "handoff", "Shift handoff",
                        "Record the owner, open work, and the next action.",
                        null, 15, true));
        return new Lab(new SkillGenie(clock, planner, desk, people, sources),
                desk, clock);
    }

    private static List<SkillGenie.Step> propose(
            List<SkillGenie.Doc> evidence, int budget) {
        List<SkillGenie.Step> steps = new ArrayList<>();
        int remaining = budget;
        for (var doc : evidence) {
            if (doc.minutes() <= remaining) {
                steps.add(new SkillGenie.Step(doc.ref(), doc.minutes()));
                remaining -= doc.minutes();
            }
            if (steps.size() == 3) {
                break;
            }
        }
        return List.copyOf(steps);
    }

    private static String preview(Lab lab) {
        var plan = lab.app().plan(ALEX, "java", 45);
        return lab.app().preview(ALEX, plan.id()).id();
    }

    private static String approved(Lab lab) {
        String id = preview(lab);
        lab.app().approve(ROBIN, id);
        return id;
    }

    private static void demo() {
        Lab lab = lab();
        var plan = lab.app().plan(ALEX, "java", 45);
        print(plan);
        var request = lab.app().preview(ALEX, plan.id());
        print(request);
        print(lab.app().approve(ROBIN, request.id()));
        print(lab.app().execute(ALEX, request.id()));
        print(lab.app().execute(ALEX, request.id()));
        System.out.println("Ticket create calls: " + lab.desk().calls);
    }

    private static void print(SkillGenie.Plan plan) {
        int minutes = plan.steps().stream().mapToInt(SkillGenie.Step::minutes).sum();
        System.out.printf("%s %s %d minutes%n", plan.id(), plan.topic(), minutes);
        for (var step : plan.steps()) {
            System.out.printf("  %s %d minutes%n", step.ref(), step.minutes());
        }
    }

    private static void print(SkillGenie.Request request) {
        System.out.printf("%s %s %s", request.id(), request.state(),
                request.terms().module());
        if (request.ticketId() != null) {
            System.out.print(" " + request.ticketId());
        }
        System.out.println();
    }

    private static void shell() {
        Lab lab = lab();
        System.out.println("SkillGenie 2.0 local lab. Type help for commands.");
        try (var input = new Scanner(System.in)) {
            while (input.hasNextLine()) {
                String line = input.nextLine().strip();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals("quit")) {
                    return;
                }
                try {
                    command(lab, line.split("\\s+"));
                } catch (SkillGenie.Rejected error) {
                    System.out.println("Rejected: " + error.getMessage());
                } catch (NumberFormatException error) {
                    System.out.println("Use a whole number of minutes.");
                }
            }
        }
    }

    private static void command(Lab lab, String[] parts) {
        String name = parts[0];
        int expected = switch (name) {
            case "help", "audit" -> 1;
            case "search", "preview", "approve", "execute",
                    "reconcile", "cancel" -> 3;
            case "plan" -> 4;
            default -> throw new SkillGenie.Rejected("UNKNOWN_COMMAND");
        };
        SkillGenie.require(parts.length == expected, "CHECK_HELP");
        var app = lab.app();
        switch (name) {
            case "help" -> System.out.println("""
                    search ACTOR java|handoff
                    plan ACTOR java|handoff MINUTES
                    preview ACTOR PLAN_ID
                    approve MANAGER REQUEST_ID
                    execute ACTOR REQUEST_ID
                    reconcile ACTOR REQUEST_ID
                    cancel MANAGER REQUEST_ID
                    audit
                    quit
                    """);
            case "audit" -> app.events().forEach(event -> System.out.printf(
                    "%s %s %s %s%n", event.actor(), event.action(),
                    event.subject(), event.result()));
            case "search" -> {
                var evidence = app.search(parts[1], parts[2]);
                for (var doc : evidence) {
                    System.out.printf("%s %s%n  %s%n",
                            doc.ref(), doc.title(), doc.excerpt());
                }
                if (evidence.isEmpty()) {
                    System.out.println("No approved source found.");
                }
            }
            case "plan" -> print(app.plan(parts[1], parts[2],
                    Integer.parseInt(parts[3])));
            case "preview" -> print(app.preview(parts[1], parts[2]));
            case "approve" -> print(app.approve(parts[1], parts[2]));
            case "execute" -> print(app.execute(parts[1], parts[2]));
            case "reconcile" -> print(app.reconcile(parts[1], parts[2]));
            case "cancel" -> print(app.cancel(parts[1], parts[2]));
            default -> throw new AssertionError(name);
        }
    }

    private record Case(String name, Runnable check) {}

    private static void runChecks() {
        var cases = List.of(
                new Case("authorized search", () -> {
                    var refs = lab().app().search(ALEX, "java").stream()
                            .map(doc -> doc.ref().toString()).toList();
                    equal(List.of("JAVA-START/v3", "JAVA-LAB/v2"), refs);
                }),
                new Case("tenant isolation", () ->
                    equal(List.of(), lab().app().search("sam", "java"))),
                new Case("different team", () -> {
                    var refs = lab().app().search("blair", "java").stream()
                            .map(doc -> doc.ref().toString()).toList();
                    equal(List.of("JAVA-START/v3", "JAVA-TEAM/v1"), refs);
                }),
                new Case("handoff has no lab", () -> {
                    Lab lab = lab();
                    var plan = lab.app().plan(ALEX, "handoff", 15);
                    rejected("NO_LAB", () -> lab.app().preview(ALEX, plan.id()));
                }),
                new Case("valid plan", () -> {
                    var plan = lab().app().plan(ALEX, "java", 45);
                    equal(List.of(
                        new SkillGenie.Step(new SkillGenie.Ref("JAVA-START", 3), 20),
                        new SkillGenie.Step(new SkillGenie.Ref("JAVA-LAB", 2), 25)),
                        plan.steps());
                }),
                new Case("no evidence", () -> rejected("NO_EVIDENCE",
                        () -> lab().app().plan("sam", "java", 45))),
                new Case("budget bounds", () -> rejected("BAD_BUDGET",
                        () -> lab().app().plan(ALEX, "java", 121))),
                new Case("foreign source proposal", () -> {
                    Lab lab = lab((docs, budget) -> List.of(
                            new SkillGenie.Step(new SkillGenie.Ref("JAVA-TEAM", 1), 15)));
                    rejected("INVALID_PLAN", () -> lab.app().plan(ALEX, "java", 45));
                }),
                new Case("wrong duration", () -> {
                    Lab lab = lab((docs, budget) -> List.of(
                            new SkillGenie.Step(docs.getFirst().ref(), 5)));
                    rejected("INVALID_PLAN", () -> lab.app().plan(ALEX, "java", 45));
                }),
                new Case("foreign plan", () -> {
                    Lab lab = lab();
                    var plan = lab.app().plan(ALEX, "java", 45);
                    rejected("PLAN_UNAVAILABLE",
                            () -> lab.app().preview("blair", plan.id()));
                }),
                new Case("wrong manager", () -> {
                    Lab lab = lab();
                    String id = preview(lab);
                    rejected("FORBIDDEN", () -> lab.app().approve(MINA, id));
                    equal(0, lab.desk().calls);
                }),
                new Case("execute before approval", () -> {
                    Lab lab = lab();
                    String id = preview(lab);
                    rejected("WRONG_STATE", () -> lab.app().execute(ALEX, id));
                    equal(0, lab.desk().calls);
                }),
                new Case("approved and duplicate execute", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    equal(0, lab.desk().calls);
                    var receipt = lab.app().execute(ALEX, id);
                    equal(SkillGenie.State.CREATED, receipt.state());
                    equal("T1", receipt.ticketId());
                    equal(receipt, lab.app().execute(ALEX, id));
                    equal(1, lab.desk().calls);
                }),
                new Case("source changed", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    lab.app().publish(javaLab(3));
                    rejected("SOURCE_CHANGED", () -> lab.app().execute(ALEX, id));
                    equal(0, lab.desk().calls);
                }),
                new Case("approval authority changed", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    lab.app().updateActor(alex(MINA, true));
                    rejected("FORBIDDEN", () -> lab.app().execute(ALEX, id));
                    equal(0, lab.desk().calls);
                }),
                new Case("learner deactivated", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    lab.app().updateActor(alex(ROBIN, false));
                    rejected("FORBIDDEN", () -> lab.app().execute(ALEX, id));
                    equal(0, lab.desk().calls);
                }),
                new Case("approval expired", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    lab.clock().advance(Duration.ofMinutes(15));
                    rejected("EXPIRED", () -> lab.app().execute(ALEX, id));
                    equal(0, lab.desk().calls);
                }),
                new Case("lost response after commit", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    lab.desk().loseReply = true;
                    var unknown = lab.app().execute(ALEX, id);
                    equal(SkillGenie.State.OUTCOME_UNKNOWN,
                            unknown.state());
                    equal(unknown, lab.app().execute(ALEX, id));
                    rejected("LOOKUP_FIRST",
                            () -> lab.app().cancel(ROBIN, id));
                    var recovered = lab.app().reconcile(ALEX, id);
                    equal(SkillGenie.State.CREATED, recovered.state());
                    equal("T1", recovered.ticketId());
                    equal(1, lab.desk().calls);
                }),
                new Case("uncertain outcome with no receipt", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    lab.desk().failBeforeCommit = true;
                    var unknown = lab.app().execute(ALEX, id);
                    equal(SkillGenie.State.OUTCOME_UNKNOWN,
                            unknown.state());
                    equal(unknown, lab.app().reconcile(ALEX, id));
                    equal(1, lab.desk().calls);
                }),
                new Case("cancel created ticket", () -> {
                    Lab lab = lab();
                    String id = approved(lab);
                    lab.app().execute(ALEX, id);
                    var cancelled = lab.app().cancel(ROBIN, id);
                    equal(SkillGenie.State.CANCELLED, cancelled.state());
                    equal("T1", cancelled.ticketId());
                    equal(SkillGenie.TicketState.CANCELLED,
                            lab.desk().find(id).state());
                    equal(cancelled, lab.app().cancel(ROBIN, id));
                }),
                new Case("cancel pending request", () -> {
                    Lab lab = lab();
                    String id = preview(lab);
                    equal(SkillGenie.State.CANCELLED,
                            lab.app().cancel(ROBIN, id).state());
                    rejected("WRONG_STATE", () -> lab.app().approve(ROBIN, id));
                    equal(0, lab.desk().calls);
                }));
        for (var row : cases) {
            row.check().run();
            System.out.println("PASS " + row.name());
        }
        System.out.println("Passed " + cases.size() + " checks.");
    }

    private static void rejected(String code, Runnable operation) {
        try {
            operation.run();
        } catch (SkillGenie.Rejected error) {
            equal(code, error.getMessage());
            return;
        }
        throw new AssertionError("Expected rejection: " + code);
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "--demo" : args[0];
        switch (mode) {
            case "--demo" -> demo();
            case "--check" -> runChecks();
            case "--shell" -> shell();
            default -> {
                System.err.println("Use --demo, --check, or --shell.");
                System.exit(2);
            }
        }
    }
}

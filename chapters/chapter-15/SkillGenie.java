import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class SkillGenie {
    enum Role { LEARNER, MANAGER }
    enum State { PENDING_APPROVAL, APPROVED, OUTCOME_UNKNOWN,
                 CREATED, CANCELLED }
    enum TicketState { OPEN, CANCELLED }

    record Actor(String id, String tenant, String team,
                 Role role, String manager, boolean active) {}
    record Ref(String id, int version) {
        @Override public String toString() {
            return id + "/v" + version;
        }
    }
    record Doc(Ref ref, String tenant, String team, String topic,
               String title, String excerpt, String module,
               int minutes, boolean current) {}
    record Step(Ref ref, int minutes) {}
    record Plan(String id, String owner, String topic, int budget,
                List<Step> steps) {
        Plan { steps = List.copyOf(steps); }
    }
    record Terms(String learner, String module, Ref source,
                 Instant expiresAt) {}
    record Request(String id, Terms terms, State state,
                   String approvedBy, String ticketId) {
        Request move(State next, String ticket) {
            return new Request(id, terms, next, approvedBy, ticket);
        }
    }
    record Receipt(String id, TicketState state) {}
    record Event(String actor, String action, String subject, String result) {}

    static final class Rejected extends RuntimeException {
        Rejected(String code) { super(code); }
    }

    static final class UnknownOutcome extends RuntimeException {}

    interface Planner {
        List<Step> propose(List<Doc> evidence, int budget);
    }

    interface Tickets {
        Receipt create(String key, Terms terms);
        Receipt find(String key);
        Receipt cancelOpen(String key);
    }

    private final Map<String, Actor> directory = new LinkedHashMap<>();
    private final Map<String, Doc> documents = new LinkedHashMap<>();
    private final Map<String, Plan> plans = new LinkedHashMap<>();
    private final Map<String, Request> requests = new LinkedHashMap<>();
    private final List<Event> audit = new ArrayList<>();
    private final Clock clock;
    private final Planner planner;
    private final Tickets tickets;

    SkillGenie(Clock clock, Planner planner, Tickets tickets,
               List<Actor> people, List<Doc> sources) {
        this.clock = clock;
        this.planner = planner;
        this.tickets = tickets;
        people.forEach(actor -> directory.put(actor.id(), actor));
        sources.forEach(this::publish);
    }

    // These two methods represent trusted fixture or admin changes.
    synchronized void publish(Doc doc) {
        documents.put(doc.ref().id(), doc);
    }

    synchronized void updateActor(Actor actor) {
        directory.put(actor.id(), actor);
    }

    synchronized List<Doc> search(String actorId, String topic) {
        Actor actor = activeActor(actorId);
        String key = topicKey(topic);
        List<Doc> evidence = documents.values().stream()
                .filter(doc -> readable(actor, doc))
                .filter(doc -> doc.topic().equals(key))
                .toList();
        audit.add(new Event(actorId, "SEARCH", key,
                "matches=" + evidence.size()));
        return evidence;
    }

    private static boolean readable(Actor actor, Doc doc) {
        return doc.current() && actor.tenant().equals(doc.tenant())
                && (doc.team().equals("*")
                    || actor.team().equals(doc.team()));
    }

    synchronized Plan plan(String actorId, String topic, int budget) {
        learner(actorId);
        require(budget >= 15 && budget <= 120, "BAD_BUDGET");
        List<Doc> evidence = search(actorId, topic);
        require(!evidence.isEmpty(), "NO_EVIDENCE");
        List<Step> proposal = planner.propose(evidence, budget);
        require(validPlan(proposal, evidence, budget), "INVALID_PLAN");
        String id = "P" + (plans.size() + 1);
        Plan plan = new Plan(id, actorId, topicKey(topic), budget, proposal);
        plans.put(id, plan);
        audit.add(new Event(actorId, "PLAN", id, "VALIDATED"));
        return plan;
    }

    private static boolean validPlan(List<Step> proposed,
                                     List<Doc> evidence, int budget) {
        if (proposed == null || proposed.isEmpty()
                || proposed.size() > 3) {
            return false;
        }
        int total = 0;
        var used = new java.util.HashSet<Ref>();
        for (Step step : proposed) {
            if (step == null || !used.add(step.ref())) {
                return false;
            }
            boolean matches = evidence.stream().anyMatch(doc ->
                    doc.ref().equals(step.ref())
                    && doc.minutes() == step.minutes());
            if (!matches || step.minutes() <= 0
                    || step.minutes() > budget) {
                return false;
            }
            total += step.minutes();
        }
        return total <= budget;
    }

    synchronized Request preview(String actorId, String planId) {
        Actor actor = learner(actorId);
        Plan plan = plans.get(planId);
        require(plan != null && plan.owner().equals(actorId),
                "PLAN_UNAVAILABLE");
        Doc lab = plan.steps().stream()
                .map(step -> currentDoc(actor, step.ref()))
                .filter(doc -> doc.module() != null)
                .findFirst().orElseThrow(() -> new Rejected("NO_LAB"));
        Terms terms = new Terms(actorId, lab.module(), lab.ref(),
                clock.instant().plus(Duration.ofMinutes(15)));
        String id = "R" + (requests.size() + 1);
        var pending = new Request(
                id, terms, State.PENDING_APPROVAL, null, null);
        return save(actorId, "PREVIEW", pending);
    }

    synchronized Request approve(String managerId, String requestId) {
        Request request = request(requestId);
        Actor learner = approver(managerId, request.terms());
        require(request.state() == State.PENDING_APPROVAL,
                "WRONG_STATE");
        checkTerms(learner, request.terms());
        var approved = new Request(request.id(), request.terms(),
                State.APPROVED, managerId, null);
        return save(managerId, "APPROVE", approved);
    }

    synchronized Request execute(String actorId, String requestId) {
        Request request = request(requestId);
        Actor learner = owner(actorId, request.terms());
        if (request.state() == State.CREATED
                || request.state() == State.OUTCOME_UNKNOWN) {
            return request;
        }
        require(request.state() == State.APPROVED, "WRONG_STATE");
        approver(request.approvedBy(), request.terms());
        checkTerms(learner, request.terms());
        try {
            Receipt receipt = tickets.create(
                    request.id(), request.terms());
            if (!openReceipt(receipt)) {
                throw new UnknownOutcome();
            }
            return save(actorId, "CREATE",
                    request.move(State.CREATED, receipt.id()));
        } catch (UnknownOutcome lost) {
            return save(actorId, "CREATE",
                    request.move(State.OUTCOME_UNKNOWN, null));
        }
    }

    synchronized Request reconcile(String actorId, String requestId) {
        Request request = request(requestId);
        owner(actorId, request.terms());
        require(request.state() == State.OUTCOME_UNKNOWN, "WRONG_STATE");
        Receipt receipt = tickets.find(request.id());
        if (!openReceipt(receipt)) {
            return save(actorId, "LOOKUP", request);
        }
        return save(actorId, "LOOKUP",
                request.move(State.CREATED, receipt.id()));
    }

    synchronized Request cancel(String managerId, String requestId) {
        Request request = request(requestId);
        approver(managerId, request.terms());
        require(request.state() != State.OUTCOME_UNKNOWN, "LOOKUP_FIRST");
        if (request.state() == State.CANCELLED) {
            return request;
        }
        if (request.state() == State.CREATED) {
            Receipt receipt = tickets.cancelOpen(request.id());
            return save(managerId, "CANCEL_TOOL",
                    request.move(State.CANCELLED, receipt.id()));
        }
        return save(managerId, "CANCEL_PREVIEW",
                request.move(State.CANCELLED, null));
    }

    private static boolean openReceipt(Receipt receipt) {
        return receipt != null && receipt.state() == TicketState.OPEN
                && receipt.id() != null && !receipt.id().isBlank();
    }

    synchronized List<Event> events() {
        return List.copyOf(audit);
    }

    private void checkTerms(Actor learner, Terms terms) {
        require(clock.instant().isBefore(terms.expiresAt()), "EXPIRED");
        Doc doc = currentDoc(learner, terms.source());
        require(Objects.equals(doc.module(), terms.module()), "SOURCE_CHANGED");
    }

    private Actor owner(String id, Terms terms) {
        Actor learner = learner(id);
        require(learner.id().equals(terms.learner()), "FORBIDDEN");
        return learner;
    }

    private Actor approver(String id, Terms terms) {
        Actor manager = activeActor(id);
        Actor learner = learner(terms.learner());
        require(manager.role() == Role.MANAGER
                && manager.id().equals(learner.manager())
                && manager.tenant().equals(learner.tenant()), "FORBIDDEN");
        return learner;
    }

    private Actor learner(String id) {
        Actor actor = activeActor(id);
        require(actor.role() == Role.LEARNER, "FORBIDDEN");
        return actor;
    }

    private Actor activeActor(String id) {
        Actor actor = directory.get(id);
        require(actor != null && actor.active(), "FORBIDDEN");
        return actor;
    }

    private Doc currentDoc(Actor actor, Ref ref) {
        Doc doc = documents.get(ref.id());
        require(doc != null && doc.ref().equals(ref) && readable(actor, doc),
                "SOURCE_CHANGED");
        return doc;
    }

    private Request request(String id) {
        Request request = requests.get(id);
        require(request != null, "REQUEST_UNAVAILABLE");
        return request;
    }

    private Request save(String actor, String action, Request request) {
        requests.put(request.id(), request);
        audit.add(new Event(actor, action, request.id(),
                request.state().name()));
        return request;
    }

    private static String topicKey(String topic) {
        String key = topic.strip().toLowerCase(Locale.ROOT);
        require(key.equals("java") || key.equals("handoff"), "UNKNOWN_TOPIC");
        return key;
    }

    static void require(boolean ok, String code) {
        if (!ok) {
            throw new Rejected(code);
        }
    }
}

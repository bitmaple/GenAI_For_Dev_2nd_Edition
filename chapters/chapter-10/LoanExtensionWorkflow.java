import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class LoanExtensionWorkflow {
    record Loan(String id, LocalDate dueDate,
                LocalDate nextReservation, int revision) {}
    record Proposal(String loanId, String requester,
                    LocalDate newDueDate, int expectedRevision) {}
    record Decision(Proposal proposal, String reviewer,
                    Instant expiresAt, boolean granted) {}

    enum Outcome {
        COMMITTED, APPROVAL_REQUIRED, EXPIRED, STALE, DATE_CONFLICT
    }
    record Result(Outcome outcome, LocalDate confirmedDueDate) {}

    private Loan loan;
    private final Map<String, Decision> decisions;
    private final Set<String> activeReviewers;
    private final Map<String, Result> receipts = new HashMap<>();

    LoanExtensionWorkflow(Loan loan, Map<String, Decision> decisions,
                          Set<String> activeReviewers) {
        this.loan = loan;
        this.decisions = Map.copyOf(decisions);
        this.activeReviewers = Set.copyOf(activeReviewers);
    }

    synchronized Result apply(Proposal proposal, String decisionId,
                              Instant now) {
        Decision decision = decisions.get(decisionId);
        if (decision == null || !decision.granted()
            || !decision.proposal().equals(proposal)
            || !activeReviewers.contains(decision.reviewer())
            || decision.reviewer().equals(proposal.requester())) {
            return stopped(Outcome.APPROVAL_REQUIRED);
        }

        Result prior = receipts.get(decisionId);
        if (prior != null) {
            return prior;
        }
        if (!now.isBefore(decision.expiresAt())) {
            return stopped(Outcome.EXPIRED);
        }
        if (!loan.id().equals(proposal.loanId())
            || loan.revision() != proposal.expectedRevision()) {
            return stopped(Outcome.STALE);
        }

        LocalDate end = proposal.newDueDate();
        if (!end.isAfter(loan.dueDate())
            || end.isAfter(loan.dueDate().plusDays(7))
            || !end.isBefore(loan.nextReservation())) {
            return stopped(Outcome.DATE_CONFLICT);
        }

        loan = new Loan(loan.id(), end, loan.nextReservation(),
                        loan.revision() + 1);
        Result receipt = new Result(Outcome.COMMITTED, end);
        receipts.put(decisionId, receipt);
        return receipt;
    }

    private static Result stopped(Outcome reason) {
        return new Result(reason, null);
    }

    public static void main(String[] args) {
        LocalDate due = LocalDate.of(2026, 8, 14);
        LocalDate newDue = due.plusDays(3);
        Instant now = Instant.parse("2026-08-12T09:00:00Z");
        Instant expiry = now.plusSeconds(3600);
        Loan startingLoan = new Loan("L-17", due, due.plusDays(5), 4);
        Proposal proposed = new Proposal("L-17", "Rina", newDue, 4);
        Proposal altered =
            new Proposal("L-17", "Rina", due.plusDays(4), 4);
        Proposal collision =
            new Proposal("L-17", "Rina", due.plusDays(5), 4);
        Proposal stale = new Proposal("L-17", "Rina", newDue, 3);

        var workflow = new LoanExtensionWorkflow(startingLoan, Map.of(
            "approved", new Decision(proposed, "Jules", expiry, true),
            "declined", new Decision(proposed, "Jules", expiry, false),
            "outsider", new Decision(proposed, "Alex", expiry, true),
            "self", new Decision(proposed, "Rina", expiry, true),
            "collision", new Decision(collision, "Jules", expiry, true),
            "stale", new Decision(stale, "Jules", expiry, true)),
            Set.of("Jules", "Rina"));

        Outcome approvalNeeded = Outcome.APPROVAL_REQUIRED;
        expect(workflow, proposed, "missing", now, approvalNeeded);
        expect(workflow, proposed, "declined", now, approvalNeeded);
        expect(workflow, proposed, "outsider", now, approvalNeeded);
        expect(workflow, proposed, "self", now, approvalNeeded);
        expect(workflow, altered, "approved", now, approvalNeeded);
        expect(workflow, proposed, "approved", expiry, Outcome.EXPIRED);
        expect(workflow, stale, "stale", now, Outcome.STALE);
        expect(workflow, collision, "collision", now,
               Outcome.DATE_CONFLICT);
        check(workflow.loan.equals(startingLoan), "loan changed");

        Result first = workflow.apply(proposed, "approved", now);
        check(first.equals(new Result(Outcome.COMMITTED, newDue)),
              "expected the approved date");
        Result replay = workflow.apply(proposed, "approved", expiry);
        check(first.equals(replay),
              "replay should return the original receipt");
        check(workflow.loan.revision() == 5, "expected one update");

        System.out.println(
            "All loan extension workflow checks passed.");
    }

    private static void expect(LoanExtensionWorkflow workflow,
                               Proposal proposal, String decisionId,
                               Instant time, Outcome expected) {
        Result result = workflow.apply(proposal, decisionId, time);
        Outcome found = result.outcome();
        check(found == expected, decisionId + " returned " + found);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

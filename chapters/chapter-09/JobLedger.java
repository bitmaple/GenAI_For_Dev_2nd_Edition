import java.time.Duration;
import java.time.Instant;
import java.util.OptionalInt;

class JobLedger {
    enum Status { QUEUED, RUNNING, READY, FAILED }

    record Snapshot(Status status, int attempt, String resultRef) {}

    private static final int MAX_ATTEMPTS = 2;
    private static final Duration LEASE_DURATION = Duration.ofSeconds(5);

    private Status status = Status.QUEUED;
    private int attempt;
    private Instant expiresAt = Instant.MIN;
    private String resultRef;

    synchronized OptionalInt claim(Instant now) {
        if (status == Status.READY || status == Status.FAILED
            || hasActiveLease(now)) {
            return OptionalInt.empty();
        }

        if (attempt >= MAX_ATTEMPTS) {
            status = Status.FAILED;
            return OptionalInt.empty();
        }

        expiresAt = now.plus(LEASE_DURATION);
        status = Status.RUNNING;
        return OptionalInt.of(++attempt);
    }

    private boolean hasActiveLease(Instant now) {
        return status == Status.RUNNING && now.isBefore(expiresAt);
    }

    synchronized boolean publish(int workerAttempt, Instant now,
                                 String checkedResult) {
        if (!hasActiveLease(now) || workerAttempt != attempt
            || checkedResult == null || checkedResult.isBlank()) {
            return false;
        }

        resultRef = checkedResult;
        status = Status.READY;
        return true;
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(status, attempt, resultRef);
    }

    public static void main(String[] args) {
        Instant start = Instant.EPOCH;
        JobLedger job = new JobLedger();
        int first = job.claim(start).orElseThrow();
        check(first == 1, "first claim allocates attempt 1");
        check(job.claim(start.plusSeconds(1)).isEmpty(),
            "duplicate delivery cannot take an active claim");

        Instant takeover = start.plusSeconds(6);
        int second = job.claim(takeover).orElseThrow();
        check(second == 2, "an expired claim can be replaced");
        check(!job.publish(first, takeover, "attempt-1"),
            "a late worker cannot publish");
        check(job.publish(second, takeover, "attempt-2"),
            "the current worker can publish");
        check(job.claim(takeover).isEmpty(),
            "a completed job is not claimed again");
        check(job.snapshot().equals(
            new Snapshot(Status.READY, 2, "attempt-2")),
            "readers see only the accepted result");

        JobLedger exhausted = new JobLedger();
        exhausted.claim(start).orElseThrow();
        exhausted.claim(takeover).orElseThrow();
        check(exhausted.claim(takeover.plusSeconds(6)).isEmpty(),
            "an expired last attempt cannot create a third attempt");
        check(exhausted.snapshot().equals(
            new Snapshot(Status.FAILED, 2, null)),
            "exhaustion becomes a visible failure");

        System.out.println("All job ledger cases passed.");
    }

    private static void check(boolean condition, String explanation) {
        if (!condition) {
            throw new AssertionError(explanation);
        }
    }
}

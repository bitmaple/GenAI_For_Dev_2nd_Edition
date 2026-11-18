import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ReleaseGate {
    record Result(String id, boolean passed, boolean prohibited,
                  long elapsedMs) {}

    enum Decision {
        ADVANCE, HOLD_EVIDENCE, HOLD_BOUNDARY, HOLD_REQUIRED,
        HOLD_QUALITY, HOLD_LATENCY
    }

    static Decision decide(Set<String> expected, Set<String> required,
                           List<Result> baseline, List<Result> candidate,
                           long latencyLimitMs) {
        if (expected.isEmpty() || !expected.containsAll(required)
            || latencyLimitMs < 0) {
            return Decision.HOLD_EVIDENCE;
        }

        Map<String, Result> base = index(baseline);
        Map<String, Result> trial = index(candidate);
        if (!base.keySet().equals(expected)
            || !trial.keySet().equals(expected)) {
            return Decision.HOLD_EVIDENCE;
        }
        if (candidate.stream().anyMatch(Result::prohibited)) {
            return Decision.HOLD_BOUNDARY;
        }
        if (required.stream().anyMatch(id -> !trial.get(id).passed())) {
            return Decision.HOLD_REQUIRED;
        }
        if (passes(candidate) < passes(baseline)) {
            return Decision.HOLD_QUALITY;
        }
        if (percentile95(candidate) > latencyLimitMs) {
            return Decision.HOLD_LATENCY;
        }
        return Decision.ADVANCE;
    }

    private static Map<String, Result> index(List<Result> results) {
        Map<String, Result> byId = new HashMap<>();
        for (Result row : results) {
            if (row == null || row.id() == null || row.id().isBlank()
                || row.elapsedMs() < 0
                || byId.putIfAbsent(row.id(), row) != null) {
                return Map.of();
            }
        }
        return byId;
    }

    private static long passes(List<Result> results) {
        return results.stream().filter(Result::passed).count();
    }

    private static long percentile95(List<Result> results) {
        long[] sorted = results.stream().mapToLong(Result::elapsedMs)
            .sorted().toArray();
        int rank = (int) ((95L * sorted.length + 99) / 100);
        return sorted[rank - 1];
    }

    public static void main(String[] args) {
        Set<String> manifest = Set.of("ACCESS", "GUIDE", "FILTER",
                                      "REVISION", "PART");
        Set<String> required = Set.of("ACCESS");
        long limit = 1800;
        List<Result> baseline = List.of(
            new Result("ACCESS", true, false, 450),
            new Result("GUIDE", true, false, 850),
            new Result("FILTER", true, false, 1040),
            new Result("REVISION", false, false, 1150),
            new Result("PART", true, false, 1000));
        List<Result> candidate = List.of(
            new Result("ACCESS", true, false, 430),
            new Result("GUIDE", false, false, 760),
            new Result("FILTER", true, false, 990),
            new Result("REVISION", true, false, 1060),
            new Result("PART", true, false, 960));

        expect(decide(manifest, required, baseline, candidate, limit),
            Decision.ADVANCE);
        expect(decide(manifest, required, baseline,
                      candidate.subList(0, 4), limit),
            Decision.HOLD_EVIDENCE);

        List<Result> duplicate = List.of(
            candidate.get(0), candidate.get(0), candidate.get(2),
            candidate.get(3), candidate.get(4));
        expect(decide(manifest, required, baseline, duplicate, limit),
            Decision.HOLD_EVIDENCE);

        List<Result> prohibited = replace(candidate,
            new Result("ACCESS", false, true, 430));
        expect(decide(manifest, required, baseline, prohibited, limit),
            Decision.HOLD_BOUNDARY);

        List<Result> missingRequired = replace(candidate,
            new Result("ACCESS", false, false, 430));
        expect(decide(manifest, required, baseline, missingRequired,
                      limit), Decision.HOLD_REQUIRED);

        List<Result> lower = replace(candidate,
            new Result("PART", false, false, 960));
        expect(decide(manifest, required, baseline, lower, limit),
            Decision.HOLD_QUALITY);

        List<Result> slow = replace(candidate,
            new Result("PART", true, false, 1900));
        expect(decide(manifest, required, baseline, slow, limit),
            Decision.HOLD_LATENCY);

        System.out.println("All release gate checks passed.");
    }

    private static List<Result> replace(List<Result> rows, Result row) {
        return rows.stream()
            .map(r -> r.id().equals(row.id()) ? row : r).toList();
    }

    private static void expect(Decision actual, Decision expected) {
        if (actual != expected) {
            String message = "Expected " + expected + ", got " + actual;
            throw new AssertionError(message);
        }
    }
}

import java.util.Arrays;
import java.util.List;
import java.util.Set;

class RoutingExample {
    static String firstRoute(boolean routine, Set<String> allowed) {
        if (routine && allowed.contains("A")) {
            return "A";
        }
        return allowed.contains("B") ? "B" : null;
    }

    static String alternateRoute(String primary, String failure,
            Set<String> allowed, long timeLeftMs) {
        boolean retryable = "TIMEOUT".equals(failure)
                || "INVALID_FORMAT".equals(failure);
        if ("A".equals(primary) && retryable && allowed.contains("B")
                && timeLeftMs >= 3_000) {
            return "B";
        }
        return null;
    }

    public static void main(String[] args) {
        var both = Set.of("A", "B");
        var onlyA = Set.of("A");
        var results = Arrays.asList(
                firstRoute(true, both),
                firstRoute(false, onlyA),
                alternateRoute("A", "INVALID_FORMAT", both, 3_200),
                alternateRoute("A", "TIMEOUT", both, 2_000),
                alternateRoute("A", "INVALID_FORMAT", onlyA, 3_200),
                alternateRoute("A", "MISSING_EVIDENCE", both, 4_000));
        List<String> expected = Arrays.asList(
                "A", null, "B", null, null, null);
        if (!results.equals(expected)) {
            throw new AssertionError(results);
        }
        System.out.println(results);
    }
}

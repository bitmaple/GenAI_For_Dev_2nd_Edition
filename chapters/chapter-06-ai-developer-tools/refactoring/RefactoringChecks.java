import java.util.Objects;

/** Deterministic, local comparisons; deliberately no network or database. */
public final class RefactoringChecks {
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("traces")) {
            traces();
        } else {
            checks();
        }
    }

    private static void traces() {
        // Enumerate every length-five sequence over payment, stock, cancel, confirm.
        for (int sequence = 0; sequence < 1024; sequence++) {
            Order order = new Order();
            int remaining = sequence;
            StringBuilder trace = new StringBuilder().append(sequence);
            for (int step = 0; step < 5; step++) {
                int operation = remaining % 4;
                remaining /= 4;
                Object result = "none";
                switch (operation) {
                    case 0 -> order.recordPaymentAuthorization();
                    case 1 -> order.recordStockReservation();
                    case 2 -> order.cancel();
                    case 3 -> result = order.confirm();
                    default -> throw new AssertionError("Invalid test operation");
                }
                trace.append('|').append(operation).append(':').append(result)
                     .append(':').append(order.status());
            }
            System.out.println(trace);
        }
    }

    private static int assertions;
    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
        assertions++;
    }
    private static void invalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            assertions++;
            return;
        }
        throw new AssertionError("Expected invalid response to be rejected");
    }
    private static void checks() {
        Order order = new Order();
        equal(Order.ConfirmationResult.WAITING, order.confirm());
        order.recordPaymentAuthorization();
        equal(Order.ConfirmationResult.WAITING, order.confirm());
        order.recordStockReservation();
        equal(Order.ConfirmationResult.NEWLY_CONFIRMED, order.confirm());
        equal(Order.ConfirmationResult.ALREADY_CONFIRMED, order.confirm());
        equal(Order.Status.CONFIRMED, order.status());

        Order reversed = new Order();
        reversed.recordStockReservation();
        equal(Order.ConfirmationResult.WAITING, reversed.confirm());
        reversed.recordPaymentAuthorization();
        equal(Order.ConfirmationResult.NEWLY_CONFIRMED, reversed.confirm());

        Order cancelled = new Order();
        cancelled.cancel();
        cancelled.recordPaymentAuthorization();
        cancelled.recordStockReservation();
        equal(Order.ConfirmationResult.BLOCKED, cancelled.confirm());
        equal(Order.Status.CANCELLED, cancelled.status());

        String[] oldOutcomes = {"RESERVED", "PENDING", "UNAVAILABLE"};
        String[] newOutcomes = {"HELD", "IN_PROGRESS", "REFUSED"};
        ReservationAdapter.ReservationState[] expected = {
            ReservationAdapter.ReservationState.RESERVED,
            ReservationAdapter.ReservationState.WAITING,
            ReservationAdapter.ReservationState.UNAVAILABLE
        };
        for (int i = 0; i < expected.length; i++) {
            equal(expected[i], ReservationAdapter.fromOldOutcome(oldOutcomes[i]));
            equal(expected[i], ReservationAdapter.fromNewOutcome(newOutcomes[i]));
            equal(ReservationAdapter.fromOldOutcome(oldOutcomes[i]),
                  ReservationAdapter.fromNewOutcome(newOutcomes[i]));
        }
        for (String value : new String[]{null, "", " ", "OTHER", "held"}) {
            invalid(() -> ReservationAdapter.fromOldOutcome(value));
            invalid(() -> ReservationAdapter.fromNewOutcome(value));
        }
        invalid(() -> ReservationAdapter.fromNewOutcome("RESERVED"));
        invalid(() -> ReservationAdapter.fromOldOutcome("HELD"));
        System.out.println("Assertions passed: " + assertions);
    }
}

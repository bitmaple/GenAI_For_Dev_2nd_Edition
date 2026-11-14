/** Dependency-free checks for the author-run investigation, not a production test suite. */
public final class OrderChecks {
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("duplicate")) {
            Order order = readyOrder();
            int newConfirmations = 0;
            if (order.confirm() == Order.ConfirmationResult.NEWLY_CONFIRMED) newConfirmations++;
            order.recordPaymentAuthorization();
            if (order.confirm() == Order.ConfirmationResult.NEWLY_CONFIRMED) newConfirmations++;
            require("repeated notification: newly confirmed once", 1, newConfirmations);
            return;
        }
        Order waiting = new Order();
        waiting.recordPaymentAuthorization();
        require("payment only: wait", Order.ConfirmationResult.WAITING, waiting.confirm());
        require("waiting order: pending", Order.Status.PENDING, waiting.status());
        Order ready = readyOrder();
        require("both facts: confirm", Order.ConfirmationResult.NEWLY_CONFIRMED, ready.confirm());
        require("ready order: confirmed", Order.Status.CONFIRMED, ready.status());
        Order cancelled = readyOrder();
        cancelled.cancel();
        require("cancelled order: blocked", Order.ConfirmationResult.BLOCKED, cancelled.confirm());
        require("cancelled order: unchanged", Order.Status.CANCELLED, cancelled.status());
        System.out.println("6 baseline checks passed.");
    }

    private static Order readyOrder() {
        Order order = new Order();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        return order;
    }

    private static void require(String label, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            System.out.println("FAIL " + label + ": expected " + expected + ", actual " + actual);
            throw new AssertionError(label);
        }
        System.out.println("PASS " + label);
    }
}

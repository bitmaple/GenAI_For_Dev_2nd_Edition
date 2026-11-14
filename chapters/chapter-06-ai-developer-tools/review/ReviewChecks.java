import java.util.ArrayList;
import java.util.List;

/** Synthetic caller evidence. No durable queue, warehouse, DB or agent session. */
public final class ReviewChecks {
    static void proposedHandler(Order order, List<String> requests) {
        var result = order.confirm();
        if (result != Order.ConfirmationResult.WAITING) {
            requests.add("Start fulfilment");
        }
    }

    static void correctedHandler(Order order, List<String> requests) {
        var result = order.confirm();
        if (result == Order.ConfirmationResult.NEWLY_CONFIRMED) {
            requests.add("Start fulfilment");
        }
    }

    private static void handle(boolean corrected, Order order, List<String> requests) {
        if (corrected) correctedHandler(order, requests);
        else proposedHandler(order, requests);
    }

    static int repeated(boolean corrected) {
        Order order = new Order();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        List<String> requests = new ArrayList<>();
        handle(corrected, order, requests);
        handle(corrected, order, requests);
        require(order.status() == Order.Status.CONFIRMED, "Final order state");
        return requests.size();
    }

    static int cancelled(boolean corrected) {
        Order order = new Order();
        order.cancel();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        List<String> requests = new ArrayList<>();
        handle(corrected, order, requests);
        require(order.status() == Order.Status.CANCELLED, "Cancellation preserved");
        return requests.size();
    }

    static void waitingChecks() {
        for (int facts = 0; facts < 3; facts++) {
            Order order = new Order();
            if (facts == 1) order.recordPaymentAuthorization();
            if (facts == 2) order.recordStockReservation();
            List<String> requests = new ArrayList<>();
            correctedHandler(order, requests);
            require(requests.isEmpty(), "Missing prerequisite must not create work");
            require(order.status() == Order.Status.PENDING, "Waiting state preserved");
        }
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        switch (args[0]) {
            case "observe" -> {
                System.out.println("Proposed caller, repeated calls: " + repeated(false));
                System.out.println("Corrected caller, repeated calls: " + repeated(true));
                System.out.println("Proposed caller, cancelled order: " + cancelled(false));
                System.out.println("Corrected caller, cancelled order: " + cancelled(true));
            }
            case "proposed-repeat" -> require(repeated(false) == 1,
                "Expected one work entry across repeated calls");
            case "proposed-cancel" -> require(cancelled(false) == 0,
                "Expected no work for cancelled order");
            case "corrected" -> {
                require(repeated(true) == 1, "Expected one work entry");
                require(cancelled(true) == 0, "Expected no cancelled-order work");
                waitingChecks();
                System.out.println("Caller checks passed: repeated, cancelled, three waiting cases");
            }
            default -> throw new IllegalArgumentException("Unknown test mode");
        }
    }
}

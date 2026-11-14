/** Dependency-free checks for the author-run investigation, not a production test suite. */
public final class OrderChecks {
    private static int passed;

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("duplicate")) {
            Order order = readyOrder();
            int newConfirmations = 0;
            if (order.confirm() == Order.ConfirmationResult.NEWLY_CONFIRMED) newConfirmations++;
            order.recordPaymentAuthorization();
            if (order.confirm() == Order.ConfirmationResult.NEWLY_CONFIRMED) newConfirmations++;
            require("repeated notification: newly confirmed once", 1, newConfirmations);
            require("repeated notification: already confirmed", Order.ConfirmationResult.ALREADY_CONFIRMED, order.confirm());
            require("repeated notification: still confirmed", Order.Status.CONFIRMED, order.status());
            System.out.println(passed + " duplicate checks passed.");
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

        Order empty = new Order();
        require("no facts: wait", Order.ConfirmationResult.WAITING, empty.confirm());
        require("no facts: pending", Order.Status.PENDING, empty.status());

        Order stockFirst = new Order();
        stockFirst.recordStockReservation();
        require("stock only: wait", Order.ConfirmationResult.WAITING, stockFirst.confirm());
        require("stock only: pending", Order.Status.PENDING, stockFirst.status());
        stockFirst.recordPaymentAuthorization();
        require("stock then payment: confirm", Order.ConfirmationResult.NEWLY_CONFIRMED, stockFirst.confirm());
        require("stock then payment: confirmed", Order.Status.CONFIRMED, stockFirst.status());

        Order repeatedPayment = new Order();
        repeatedPayment.recordPaymentAuthorization();
        repeatedPayment.recordPaymentAuthorization();
        require("repeated payment while waiting: wait", Order.ConfirmationResult.WAITING, repeatedPayment.confirm());
        require("repeated payment while waiting: pending", Order.Status.PENDING, repeatedPayment.status());
        repeatedPayment.recordStockReservation();
        require("repeated payment then stock: confirm", Order.ConfirmationResult.NEWLY_CONFIRMED, repeatedPayment.confirm());

        Order repeatedStock = new Order();
        repeatedStock.recordStockReservation();
        repeatedStock.recordStockReservation();
        require("repeated stock while waiting: wait", Order.ConfirmationResult.WAITING, repeatedStock.confirm());
        require("repeated stock while waiting: pending", Order.Status.PENDING, repeatedStock.status());
        repeatedStock.recordPaymentAuthorization();
        require("repeated stock then payment: confirm", Order.ConfirmationResult.NEWLY_CONFIRMED, repeatedStock.confirm());

        Order confirmed = readyOrder();
        require("before repeats: confirm", Order.ConfirmationResult.NEWLY_CONFIRMED, confirmed.confirm());
        confirmed.recordPaymentAuthorization();
        require("repeated payment after confirmation: already", Order.ConfirmationResult.ALREADY_CONFIRMED, confirmed.confirm());
        require("repeated payment after confirmation: confirmed", Order.Status.CONFIRMED, confirmed.status());
        confirmed.recordStockReservation();
        require("repeated stock after confirmation: already", Order.ConfirmationResult.ALREADY_CONFIRMED, confirmed.confirm());
        require("repeated stock after confirmation: confirmed", Order.Status.CONFIRMED, confirmed.status());

        Order cancelledFirst = new Order();
        cancelledFirst.cancel();
        cancelledFirst.recordPaymentAuthorization();
        cancelledFirst.recordStockReservation();
        require("cancel then late facts: blocked", Order.ConfirmationResult.BLOCKED, cancelledFirst.confirm());
        require("cancel then late facts: cancelled", Order.Status.CANCELLED, cancelledFirst.status());

        Order cancelledWaiting = new Order();
        cancelledWaiting.recordPaymentAuthorization();
        cancelledWaiting.cancel();
        cancelledWaiting.recordStockReservation();
        require("cancel while waiting then late stock: blocked", Order.ConfirmationResult.BLOCKED, cancelledWaiting.confirm());
        require("cancel while waiting then late stock: cancelled", Order.Status.CANCELLED, cancelledWaiting.status());

        for (Order.ConfirmationResult result : Order.ConfirmationResult.values()) {
            int expected = result == Order.ConfirmationResult.NEWLY_CONFIRMED ? 1 : 0;
            require("caller counts " + result, expected, newWork(result));
        }

        Order streamed = new Order();
        int work = 0;
        streamed.recordStockReservation();
        work += newWork(requireResult("caller stream stock", Order.ConfirmationResult.WAITING, streamed.confirm()));
        streamed.recordStockReservation();
        work += newWork(requireResult("caller stream repeated stock", Order.ConfirmationResult.WAITING, streamed.confirm()));
        streamed.recordPaymentAuthorization();
        work += newWork(requireResult("caller stream payment", Order.ConfirmationResult.NEWLY_CONFIRMED, streamed.confirm()));
        streamed.recordPaymentAuthorization();
        work += newWork(requireResult("caller stream repeated payment", Order.ConfirmationResult.ALREADY_CONFIRMED, streamed.confirm()));
        streamed.recordStockReservation();
        work += newWork(requireResult("caller stream late stock", Order.ConfirmationResult.ALREADY_CONFIRMED, streamed.confirm()));
        require("caller stream: one new unit of work", 1, work);
        require("caller stream: confirmed", Order.Status.CONFIRMED, streamed.status());

        Order streamedCancelled = new Order();
        int cancelledWork = 0;
        streamedCancelled.cancel();
        streamedCancelled.recordPaymentAuthorization();
        cancelledWork += newWork(requireResult("caller cancelled stream payment", Order.ConfirmationResult.BLOCKED, streamedCancelled.confirm()));
        streamedCancelled.recordStockReservation();
        cancelledWork += newWork(requireResult("caller cancelled stream stock", Order.ConfirmationResult.BLOCKED, streamedCancelled.confirm()));
        require("caller cancelled stream: no new work", 0, cancelledWork);
        require("caller cancelled stream: cancelled", Order.Status.CANCELLED, streamedCancelled.status());

        System.out.println(passed + " baseline checks passed.");
    }

    private static Order readyOrder() {
        Order order = new Order();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        return order;
    }

    private static int newWork(Order.ConfirmationResult result) {
        return result == Order.ConfirmationResult.NEWLY_CONFIRMED ? 1 : 0;
    }

    private static Order.ConfirmationResult requireResult(String label, Order.ConfirmationResult expected, Order.ConfirmationResult actual) {
        require(label, expected, actual);
        return actual;
    }

    private static void require(String label, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            System.out.println("FAIL " + label + ": expected " + expected + ", actual " + actual);
            throw new AssertionError(label);
        }
        passed++;
        System.out.println("PASS " + label);
    }
}

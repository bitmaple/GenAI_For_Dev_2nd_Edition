import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** Tests the actual earlier example; no persistence or external service. */
class WeakConfirmationTest {
    private Order readyOrder() {
        var order = new Order();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        return order;
    }

    @Test
    void repeatedCallKeepsConfirmedState() {
        var order = readyOrder();
        order.confirm();
        order.confirm();
        assertEquals(Order.Status.CONFIRMED, order.status());
    }
}

class StrongConfirmationTest {
    // The earlier example is in the unnamed package. Aliases preserve the
    // manuscript's readable imported enum names without changing that source.
    private static final Order.ConfirmationResult NEWLY_CONFIRMED =
        Order.ConfirmationResult.NEWLY_CONFIRMED;
    private static final Order.ConfirmationResult ALREADY_CONFIRMED =
        Order.ConfirmationResult.ALREADY_CONFIRMED;

    private Order readyOrder() {
        var order = new Order();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        return order;
    }

    @Test
    void repeatedCallDoesNotSignalNewWork() {
        var order = readyOrder();
        assertEquals(NEWLY_CONFIRMED, order.confirm());
        assertEquals(ALREADY_CONFIRMED, order.confirm());
        assertEquals(Order.Status.CONFIRMED, order.status());
    }
}

class PrerequisiteTests {
    @Test
    void noAcceptedFactsWaits() {
        var order = new Order();
        assertEquals(Order.ConfirmationResult.WAITING, order.confirm());
        assertEquals(Order.Status.PENDING, order.status());
    }

    @Test
    void paymentAloneWaits() {
        var order = new Order();
        order.recordPaymentAuthorization();
        assertEquals(Order.ConfirmationResult.WAITING, order.confirm());
        assertEquals(Order.Status.PENDING, order.status());
    }

    @Test
    void stockAloneWaitsThenPaymentConfirms() {
        var order = new Order();
        order.recordStockReservation();
        assertEquals(Order.ConfirmationResult.WAITING, order.confirm());
        assertEquals(Order.Status.PENDING, order.status());
        order.recordPaymentAuthorization();
        assertEquals(Order.ConfirmationResult.NEWLY_CONFIRMED, order.confirm());
        assertEquals(Order.Status.CONFIRMED, order.status());
    }

    @Test
    void cancellationBeforeLateFactsStaysTerminal() {
        var order = new Order();
        order.cancel();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        assertEquals(Order.ConfirmationResult.BLOCKED, order.confirm());
        assertEquals(Order.Status.CANCELLED, order.status());
    }
}

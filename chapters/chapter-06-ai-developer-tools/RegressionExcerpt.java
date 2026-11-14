/** Complete context for the printed regression excerpt. */
public final class RegressionExcerpt {
    public static void main(String[] args) {
        Order order = new Order();
        order.recordPaymentAuthorization();
        order.recordStockReservation();
        int newConfirmations = 0;
        if (order.confirm() == Order.ConfirmationResult.NEWLY_CONFIRMED) {
            newConfirmations++;
        }
        order.recordPaymentAuthorization();
        if (order.confirm() == Order.ConfirmationResult.NEWLY_CONFIRMED) {
            newConfirmations++;
        }
        if (newConfirmations != 1) {
            throw new AssertionError(
                "Expected one new confirmation, got " + newConfirmations);
        }
        System.out.println("One new confirmation observed.");
    }
}

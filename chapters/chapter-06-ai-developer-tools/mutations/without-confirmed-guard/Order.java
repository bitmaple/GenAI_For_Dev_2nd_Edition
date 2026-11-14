/** Synthetic local teaching model. No persistence, messaging or customer data. */
public final class Order {
    public enum Status { PENDING, CONFIRMED, CANCELLED }
    public enum ConfirmationResult { WAITING, BLOCKED, ALREADY_CONFIRMED, NEWLY_CONFIRMED }

    private Status status = Status.PENDING;
    private boolean paymentAuthorized;
    private boolean stockReserved;

    public void recordPaymentAuthorization() { paymentAuthorized = true; }
    public void recordStockReservation() { stockReserved = true; }
    public void cancel() { status = Status.CANCELLED; }
    public Status status() { return status; }

    public ConfirmationResult confirm() {
        if (status == Status.CANCELLED) return ConfirmationResult.BLOCKED;
        if (!paymentAuthorized || !stockReserved) {
            return ConfirmationResult.WAITING;
        }
        status = Status.CONFIRMED;
        return ConfirmationResult.NEWLY_CONFIRMED;
    }
}

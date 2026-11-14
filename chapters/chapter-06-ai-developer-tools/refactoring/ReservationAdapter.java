/** Illustrative reviewed-contract translation only, not a live provider client. */
public final class ReservationAdapter {
    enum ReservationState { RESERVED, WAITING, UNAVAILABLE }

    static ReservationState fromNewOutcome(String outcome) {
        if (outcome == null) {
            throw new IllegalArgumentException("Missing outcome");
        }
        return switch (outcome) {
            case "HELD" -> ReservationState.RESERVED;
            case "IN_PROGRESS" -> ReservationState.WAITING;
            case "REFUSED" -> ReservationState.UNAVAILABLE;
            default ->
                throw new IllegalArgumentException("Unknown outcome");
        };
    }

    static ReservationState fromOldOutcome(String outcome) {
        if (outcome == null) {
            throw new IllegalArgumentException("Missing outcome");
        }
        return switch (outcome) {
            case "RESERVED" -> ReservationState.RESERVED;
            case "PENDING" -> ReservationState.WAITING;
            case "UNAVAILABLE" -> ReservationState.UNAVAILABLE;
            default -> throw new IllegalArgumentException("Unknown outcome");
        };
    }
}

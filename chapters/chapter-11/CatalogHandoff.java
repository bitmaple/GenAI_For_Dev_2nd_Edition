class CatalogHandoff {
    record Work(String workId, String taskId, String peer, int attempt,
                int sourceRevision, String locale, String contract) {}

    record Reply(String workId, String taskId, int attempt,
                 int sourceRevision, String locale, String contract,
                 String draftId) {}

    enum Outcome {
        ACCEPTED, DUPLICATE, CONFLICT, DENIED, WRONG_TASK, STALE, INVALID
    }

    private final Work work;
    private Reply accepted;

    CatalogHandoff(Work work) {
        this.work = work;
    }

    synchronized Outcome accept(String authenticatedPeer, Reply reply,
                                int currentSourceRevision) {
        if (!work.peer().equals(authenticatedPeer)) {
            return Outcome.DENIED;
        }
        if (reply == null) {
            return Outcome.INVALID;
        }
        if (!work.workId().equals(reply.workId())
            || !work.taskId().equals(reply.taskId())) {
            return Outcome.WRONG_TASK;
        }
        if (work.sourceRevision() != currentSourceRevision
            || work.sourceRevision() != reply.sourceRevision()
            || work.attempt() != reply.attempt()) {
            return Outcome.STALE;
        }
        if (!work.locale().equals(reply.locale())
            || !work.contract().equals(reply.contract())
            || reply.draftId() == null || reply.draftId().isBlank()) {
            return Outcome.INVALID;
        }
        if (accepted != null) {
            return accepted.equals(reply)
                ? Outcome.DUPLICATE : Outcome.CONFLICT;
        }

        accepted = reply;
        return Outcome.ACCEPTED;
    }

    public static void main(String[] args) {
        String workId = "locale-fr-ca";
        String peer = "localization";
        String taskId = "task-302";
        String locale = "fr-CA";
        String contract = "catalog-copy-v2";

        Work work =
            new Work(workId, taskId, peer, 2, 12, locale, contract);
        var handoff = new CatalogHandoff(work);
        Reply draft = new Reply(workId, taskId, 2, 12, locale, contract,
                                "draft-a");

        expect(handoff.accept("unknown", draft, 12), Outcome.DENIED);
        expect(handoff.accept(peer, null, 12), Outcome.INVALID);

        Reply otherTask = new Reply(workId, "task-999", 2, 12, locale,
                                    contract, "draft-a");
        expect(handoff.accept(peer, otherTask, 12), Outcome.WRONG_TASK);

        Reply otherWork = new Reply("locale-en-ca", taskId, 2, 12, locale,
                                    contract, "draft-a");
        expect(handoff.accept(peer, otherWork, 12), Outcome.WRONG_TASK);

        Reply firstAttempt = new Reply(workId, taskId, 1, 12, locale,
                                       contract, "draft-a");
        expect(handoff.accept(peer, firstAttempt, 12), Outcome.STALE);

        Reply oldSource = new Reply(workId, taskId, 2, 11, locale,
                                    contract, "draft-a");
        expect(handoff.accept(peer, oldSource, 12), Outcome.STALE);
        expect(handoff.accept(peer, draft, 13), Outcome.STALE);

        Reply wrongLocale = new Reply(workId, taskId, 2, 12, "fr-FR",
                                      contract, "draft-a");
        expect(handoff.accept(peer, wrongLocale, 12), Outcome.INVALID);

        Reply wrongContract = new Reply(workId, taskId, 2, 12, locale,
                                        "catalog-copy-v1", "draft-a");
        expect(handoff.accept(peer, wrongContract, 12), Outcome.INVALID);

        Reply missingDraft = new Reply(workId, taskId, 2, 12, locale,
                                       contract, "");
        expect(handoff.accept(peer, missingDraft, 12), Outcome.INVALID);

        expect(handoff.accept(peer, draft, 12), Outcome.ACCEPTED);
        expect(handoff.accept(peer, draft, 12), Outcome.DUPLICATE);

        Reply differentDraft = new Reply(workId, taskId, 2, 12, locale,
                                         contract, "draft-b");
        Outcome conflict = handoff.accept(peer, differentDraft, 12);
        expect(conflict, Outcome.CONFLICT);
        if (!draft.equals(handoff.accepted)) {
            throw new AssertionError("Accepted draft changed");
        }

        System.out.println("All catalog handoff checks passed.");
    }

    private static void expect(Outcome actual, Outcome expected) {
        if (actual != expected) {
            String message = "Expected " + expected + ", got " + actual;
            throw new AssertionError(message);
        }
    }
}

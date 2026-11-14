import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Local synthetic experiments only. No database, network or customer data. */
public final class DebuggingChecks {
    private static int assertions;
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5, TimeUnit.SECONDS))
            throw new AssertionError("Fixture coordination timed out");
    }
    enum Status { PENDING, CONFIRMED, CANCELLED }
    record State(long revision, Status status) {}
    static final class LocalOrder {
        private Status status = Status.PENDING;
        // Both accepted facts are present before either worker starts.
        synchronized boolean confirm() {
            if (status != Status.PENDING) return false;
            status = Status.CONFIRMED;
            return true;
        }
    }
    static final class StoreModel {
        final AtomicReference<State> current =
            new AtomicReference<>(new State(0, Status.PENDING));
        boolean saveIfUnchanged(State loaded) {
            var confirmed = new State(
                loaded.revision() + 1, Status.CONFIRMED);
            return current.compareAndSet(loaded, confirmed);
        }
    }
    static void competingCopies() throws Exception {
        var store = new StoreModel();
        var bothLoaded = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var proposals = new AtomicInteger();
        var accepted = new AtomicInteger();
        var conflicts = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> worker = () -> {
                var loaded = store.current.get();
                var local = new LocalOrder();
                bothLoaded.countDown();
                await(release);
                if (loaded.status() == Status.PENDING && local.confirm()) {
                    proposals.incrementAndGet();
                    if (store.saveIfUnchanged(loaded)) accepted.incrementAndGet();
                    else conflicts.incrementAndGet();
                }
                return null;
            };
            var first = executor.submit(worker);
            var second = executor.submit(worker);
            try { await(bothLoaded); } finally { release.countDown(); }
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        }
        require(proposals.get() == 2, "Both separate local guards must pass");
        require(accepted.get() == 1, "Only one shared-state update may win");
        require(conflicts.get() == 1, "One stale update must be rejected");
        require(store.current.get().status() == Status.CONFIRMED, "Final state");
        require(store.current.get().revision() == 1, "Single revision advance");
        System.out.println("Local confirmation proposals: " + proposals);
        System.out.println("Accepted shared-state updates: " + accepted);
        System.out.println("Rejected stale updates: " + conflicts);

        var cancelStore = new StoreModel();
        var stale = cancelStore.current.get();
        cancelStore.current.set(new State(1, Status.CANCELLED));
        require(!cancelStore.saveIfUnchanged(stale), "Cancellation beats stale proposal");
        require(cancelStore.current.get().status() == Status.CANCELLED, "No resurrection");
    }
    static void waitingForLock() throws Exception {
        var monitor = new Object();
        var ownerEntered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiterDone = new CountDownLatch(1);
        var problem = new AtomicReference<Throwable>();
        Thread owner = new Thread(() -> {
            try {
                synchronized (monitor) {
                    ownerEntered.countDown();
                    await(release); // controlled stand-in for a slow external wait
                }
            } catch (Throwable t) { problem.set(t); }
        }, "notification-owner");
        Thread waiter = new Thread(() -> {
            synchronized (monitor) { waiterDone.countDown(); }
        }, "confirmation-worker");
        owner.start();
        ThreadInfo observed = null;
        try {
            await(ownerEntered);
            waiter.start();
            var bean = ManagementFactory.getThreadMXBean();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                observed = bean.getThreadInfo(waiter.threadId(), 8);
                if (observed != null && observed.getThreadState() == Thread.State.BLOCKED)
                    break;
                Thread.sleep(2);
            }
            require(observed != null && observed.getThreadState() == Thread.State.BLOCKED,
                    "Must observe a real blocked platform thread");
            require("notification-owner".equals(observed.getLockOwnerName()), "Lock owner");
            System.out.println("confirmation-worker: " + observed.getThreadState());
            System.out.println("Lock owner: " + observed.getLockOwnerName());
        } finally {
            release.countDown();
            owner.join(5000);
            waiter.join(5000);
        }
        require(problem.get() == null, "Owner completed without fixture error");
        require(waiterDone.getCount() == 0, "Waiter proceeds after release");
        require(!owner.isAlive() && !waiter.isAlive(), "Threads finished");

        var shortMonitor = new Object();
        var externalWaitStarted = new CountDownLatch(1);
        var externalRelease = new CountDownLatch(1);
        var otherDone = new CountDownLatch(1);
        Thread shortOwner = new Thread(() -> {
            try {
                synchronized (shortMonitor) { /* short local state decision */ }
                externalWaitStarted.countDown();
                await(externalRelease);
            } catch (Throwable t) { problem.set(t); }
        }, "short-owner");
        Thread other = new Thread(() -> {
            synchronized (shortMonitor) { otherDone.countDown(); }
        }, "other-order-worker");
        shortOwner.start();
        try {
            await(externalWaitStarted);
            other.start();
            await(otherDone);
            require(externalRelease.getCount() == 1, "Other work before external release");
            System.out.println("Short lock: other worker entered before wait ended");
        } finally {
            externalRelease.countDown();
            shortOwner.join(5000);
            other.join(5000);
        }
        require(problem.get() == null, "Short-owner experiment finished");
    }
    static void track(ConcurrentHashMap<String, CompletableFuture<String>> inFlight,
                      String requestId, CompletableFuture<String> task) {
        if (inFlight.putIfAbsent(requestId, task) != null)
            throw new IllegalStateException("Request is already tracked");
        task.whenComplete((result, error) ->
            inFlight.remove(requestId, task));
    }
    static List<Integer> retainedCounts(boolean cleanup) {
        var inFlight = new ConcurrentHashMap<String, CompletableFuture<String>>();
        var counts = new ArrayList<Integer>();
        for (int batch = 0; batch < 3; batch++) {
            for (int item = 0; item < 40; item++) {
                var task = new CompletableFuture<String>();
                String id = batch + ":" + item;
                if (cleanup) track(inFlight, id, task);
                else inFlight.put(id, task);
                task.complete("Completed order check");
            }
            counts.add(inFlight.size());
        }
        return counts;
    }
    static void retainedWork() {
        var unbounded = retainedCounts(false);
        var cleaned = retainedCounts(true);
        require(unbounded.equals(List.of(40,80,120)), "Retained completed futures");
        require(cleaned.equals(List.of(0,0,0)), "Completed entries cleaned");
        System.out.println("Without cleanup: " + unbounded);
        System.out.println("With cleanup: " + cleaned);
        var map = new ConcurrentHashMap<String, CompletableFuture<String>>();
        var failed = new CompletableFuture<String>();
        track(map,"failed",failed); failed.completeExceptionally(new IllegalStateException("fixture"));
        require(map.isEmpty(), "Failure cleanup");
        var cancelled = new CompletableFuture<String>();
        track(map,"cancelled",cancelled); cancelled.cancel(false);
        require(map.isEmpty(), "Future cancellation cleanup");
        var rejected = new CompletableFuture<String>();
        track(map,"rejected",rejected);
        rejected.completeExceptionally(new RejectedExecutionException("fixture"));
        require(map.isEmpty(), "Submission rejection cleanup when completed exceptionally");
        var unfinished = new CompletableFuture<String>();
        track(map,"unfinished",unfinished);
        require(map.size() == 1, "Active work remains tracked");
        var newer = new CompletableFuture<String>();
        map.replace("unfinished",unfinished,newer);
        unfinished.complete("old outcome");
        require(map.get("unfinished") == newer, "Old completion cannot delete new mapping");
        map.clear();
    }
    public static void main(String[] args) throws Exception {
        competingCopies();
        waitingForLock();
        retainedWork();
        System.out.println("Assertions passed: " + assertions);
    }
}

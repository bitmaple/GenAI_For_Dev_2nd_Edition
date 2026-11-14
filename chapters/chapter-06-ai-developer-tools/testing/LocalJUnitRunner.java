import java.util.*;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Minimal local harness using the cached real Jupiter engine, not a fake Test annotation. */
public final class LocalJUnitRunner {
    public static void main(String[] args) {
        ConfigurationParameters config = new ConfigurationParameters() {
            public Optional<String> get(String key) { return Optional.empty(); }
            public Optional<Boolean> getBoolean(String key) { return Optional.empty(); }
            public int size() { return 0; }
            public Set<String> keySet() { return Set.of(); }
        };
        EngineDiscoveryRequest request = new EngineDiscoveryRequest() {
            public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> type) {
                return Arrays.stream(args).map(name -> selectClass(name))
                    .filter(type::isInstance).map(type::cast).toList();
            }
            public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> type) {
                return List.of();
            }
            public ConfigurationParameters getConfigurationParameters() { return config; }
        };
        int[] counts = new int[3]; // successful tests, failed tests/containers, aborted/skipped
        EngineExecutionListener listener = new EngineExecutionListener() {
            public void executionSkipped(TestDescriptor d, String reason) {
                counts[2]++;
                System.out.println("SKIPPED " + d.getDisplayName() + ": " + reason);
            }
            public void executionFinished(TestDescriptor d, TestExecutionResult r) {
                if (r.getStatus() == TestExecutionResult.Status.FAILED) {
                    counts[1]++;
                    System.out.println("FAIL " + d.getDisplayName());
                    r.getThrowable().ifPresent(t -> System.out.println(t.toString()));
                } else if (r.getStatus() == TestExecutionResult.Status.ABORTED) {
                    counts[2]++;
                } else if (d.isTest()) {
                    counts[0]++;
                    System.out.println("PASS " + d.getDisplayName());
                }
            }
        };
        var engine = new JupiterTestEngine();
        var root = engine.discover(request, UniqueId.forEngine(engine.getId()));
        engine.execute(new ExecutionRequest(root, listener, config));
        System.out.printf("Tests passed: %d; failures: %d; aborted/skipped: %d%n",
            counts[0], counts[1], counts[2]);
        if (counts[0] + counts[1] == 0 || counts[1] != 0 || counts[2] != 0) System.exit(1);
    }
}

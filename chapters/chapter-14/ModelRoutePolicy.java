import java.util.Map;
import java.util.Set;

class ModelRoutePolicy {
    enum Environment { STAGING, PRODUCTION }
    enum State { EVALUATION, ACTIVE, RETIRED }
    enum DataClass { PUBLIC, INTERNAL, RESTRICTED }
    enum Capability { STRUCTURED_TEXT, TOOL_CALLS }

    enum Decision {
        ALLOW, MISSING_WORKLOAD, INVALID_REQUEST, UNKNOWN_ROUTE,
        ROUTE_NOT_GRANTED, ROUTE_NOT_ACTIVE, WRONG_ENVIRONMENT,
        DATA_CLASS_NOT_ALLOWED, CAPABILITY_NOT_SUPPORTED,
        REGION_NOT_ALLOWED
    }

    record Workload(String id, Environment environment,
                    Set<String> routeRevisions,
                    Set<String> allowedRegions) {
        Workload {
            routeRevisions = Set.copyOf(routeRevisions);
            allowedRegions = Set.copyOf(allowedRegions);
        }
    }

    record Route(String revision, State state, Environment environment,
                 Set<DataClass> allowedData,
                 Set<Capability> capabilities,
                 Set<String> processingRegions) {
        Route {
            allowedData = Set.copyOf(allowedData);
            capabilities = Set.copyOf(capabilities);
            processingRegions = Set.copyOf(processingRegions);
        }
    }

    record Request(String routeRevision, DataClass dataClass,
                   Capability capability) {}

    static Decision decide(Workload workload, Request request,
                           Map<String, Route> catalog) {
        if (workload == null || workload.id() == null
                || workload.id().isBlank()
                || workload.environment() == null) {
            return Decision.MISSING_WORKLOAD;
        }
        if (request == null || request.routeRevision() == null
                || request.routeRevision().isBlank()
                || request.dataClass() == null
                || request.capability() == null) {
            return Decision.INVALID_REQUEST;
        }

        Route route = catalog.get(request.routeRevision());
        if (route == null
                || !request.routeRevision().equals(route.revision())) {
            return Decision.UNKNOWN_ROUTE;
        }
        if (!workload.routeRevisions().contains(route.revision())) {
            return Decision.ROUTE_NOT_GRANTED;
        }
        if (route.state() != State.ACTIVE) {
            return Decision.ROUTE_NOT_ACTIVE;
        }
        if (workload.environment() != route.environment()) {
            return Decision.WRONG_ENVIRONMENT;
        }
        if (!route.allowedData().contains(request.dataClass())) {
            return Decision.DATA_CLASS_NOT_ALLOWED;
        }
        if (!route.capabilities().contains(request.capability())) {
            return Decision.CAPABILITY_NOT_SUPPORTED;
        }
        if (route.processingRegions().isEmpty()
                || !workload.allowedRegions().containsAll(
                        route.processingRegions())) {
            return Decision.REGION_NOT_ALLOWED;
        }
        return Decision.ALLOW;
    }

    public static void main(String[] args) {
        String revision = "internal-summary-r3";
        Environment prod = Environment.PRODUCTION;
        DataClass internal = DataClass.INTERNAL;
        Capability structured = Capability.STRUCTURED_TEXT;
        Set<String> oneRegion = Set.of("region-a");
        Workload workload = new Workload(
                "service-desk", prod, Set.of(revision), oneRegion);
        Route approved = summaryRoute(
                revision, State.ACTIVE, prod, oneRegion);
        Map<String, Route> catalog = Map.of(revision, approved);
        Request request = new Request(revision, internal, structured);

        expect(Decision.ALLOW, decide(workload, request, catalog));
        expect(Decision.MISSING_WORKLOAD, decide(null, request, catalog));
        expect(Decision.UNKNOWN_ROUTE,
                decide(workload, request, Map.of()));

        Route misfiled = summaryRoute("internal-summary-r2", State.ACTIVE,
                prod, oneRegion);
        expect(Decision.UNKNOWN_ROUTE,
                decide(workload, request, Map.of(revision, misfiled)));

        Workload other = new Workload(
                "another-app", prod, Set.of(), oneRegion);
        expect(Decision.ROUTE_NOT_GRANTED,
                decide(other, request, catalog));

        Route retired = summaryRoute(
                revision, State.RETIRED, prod, oneRegion);
        expect(Decision.ROUTE_NOT_ACTIVE,
                decide(workload, request, Map.of(revision, retired)));
        Route staging = summaryRoute(revision, State.ACTIVE,
                Environment.STAGING, oneRegion);
        expect(Decision.WRONG_ENVIRONMENT,
                decide(workload, request, Map.of(revision, staging)));

        Request restricted = new Request(revision, DataClass.RESTRICTED,
                structured);
        expect(Decision.DATA_CLASS_NOT_ALLOWED,
                decide(workload, restricted, catalog));
        Request tool = new Request(
                revision, internal, Capability.TOOL_CALLS);
        expect(Decision.CAPABILITY_NOT_SUPPORTED,
                decide(workload, tool, catalog));

        Route multiRegion = summaryRoute(revision, State.ACTIVE, prod,
                Set.of("region-a", "region-b"));
        Map<String, Route> multiCatalog = Map.of(revision, multiRegion);
        expect(Decision.REGION_NOT_ALLOWED,
                decide(workload, request, multiCatalog));
        Route unknownRegion = summaryRoute(revision, State.ACTIVE,
                prod, Set.of());
        Map<String, Route> unknownCatalog =
                Map.of(revision, unknownRegion);
        expect(Decision.REGION_NOT_ALLOWED,
                decide(workload, request, unknownCatalog));

        Request unclassified = new Request(revision, null, structured);
        expect(Decision.INVALID_REQUEST,
                decide(workload, unclassified, catalog));
        System.out.println("All model route policy checks passed.");
    }

    private static Route summaryRoute(
            String revision, State state, Environment env,
            Set<String> regions) {
        return new Route(revision, state, env, Set.of(DataClass.INTERNAL),
                Set.of(Capability.STRUCTURED_TEXT), regions);
    }

    private static void expect(Decision expected, Decision actual) {
        if (expected != actual) {
            throw new AssertionError(
                    "Expected " + expected + ", got " + actual);
        }
    }
}

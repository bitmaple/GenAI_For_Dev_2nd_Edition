import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

class GatewayComparison {
    static final Gson JSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT).create();

    static final String PROMPT = """
            A customer asks why a return has not progressed.
            S1, order record: RETURN_REQUESTED, reason unavailable.
            G1, policy: Returned items are inspected before a refund.
            Contact support for the reason a return is delayed.
            Give the status and next step. Identify which facts come
            from S1 and G1. Use only the supplied evidence.
            """;

    record Reply(String id, String model, JsonElement usage,
                 List<Choice> choices) {}
    record Choice(Message message, String finish_reason) {}
    record Message(String content, String refusal) {}

    static JsonObject compare(HttpClient client, URI url,
            String token, String route) throws InterruptedException {
        var result = new JsonObject();
        result.addProperty("route", route);
        var payload = Map.of("model", route, "messages",
                List.of(Map.of("role", "user", "content", PROMPT)));
        var request = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        JSON.toJson(payload)))
                .build();
        long started = System.nanoTime();
        try {
            var response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            result.addProperty("elapsed_ms",
                    (System.nanoTime() - started) / 1_000_000);
            result.addProperty("http_status", response.statusCode());
            if (response.statusCode() == 200) {
                var reply = JSON.fromJson(response.body(), Reply.class);
                addReply(result, reply);
            }
        } catch (IOException | JsonParseException failure) {
            result.addProperty("error",
                    failure.getClass().getSimpleName());
        }
        if (!result.has("elapsed_ms")) {
            result.addProperty("elapsed_ms",
                    (System.nanoTime() - started) / 1_000_000);
        }
        return result;
    }

    static void addReply(JsonObject result, Reply reply) {
        if (reply == null || reply.choices() == null
                || reply.choices().isEmpty()) {
            throw new JsonParseException("Missing choices");
        }
        var choice = reply.choices().getFirst();
        if (choice == null || choice.message() == null) {
            throw new JsonParseException("Missing message");
        }
        result.addProperty("model", reply.model());
        result.addProperty("id", reply.id());
        result.add("usage", reply.usage());
        result.addProperty("finish", choice.finish_reason());
        result.addProperty("answer", choice.message().content());
        result.addProperty("refusal", choice.message().refusal());
    }

    static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name);
        }
        return value;
    }

    public static void main(String[] args) throws InterruptedException {
        var url = URI.create(env("EVAL_URL"));
        if (!"https".equalsIgnoreCase(url.getScheme())
                || url.getHost() == null
                || !url.getPath().endsWith("/chat/completions")
                || url.getQuery() != null || url.getFragment() != null
                || url.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "Use the approved HTTPS URL");
        }
        String first = env("EVAL_A");
        String second = env("EVAL_B");
        if (first.equals(second)) {
            throw new IllegalArgumentException(
                    "Use two different routes");
        }
        String token = env("EVAL_TOKEN");
        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build();
        for (String route : List.of(first, second)) {
            var result = compare(client, url, token, route);
            System.out.println(JSON.toJson(result));
        }
    }
}

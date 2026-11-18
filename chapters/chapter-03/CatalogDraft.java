import com.google.gson.*;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

class CatalogDraft {
    static final Gson JSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT).create();
    static final String REVIEW = "Review required.";

    record Product(String name, String sourceId,
                   Map<String, String> features) {}

    interface Catalog {
        Product find(String id) throws TimeoutException;
    }

    interface Generator {
        String propose(Product product) throws TimeoutException;
    }

    static String checkedFeature(String reply, Product product) {
        if (reply == null || reply.length() > 512) {
            return null;
        }
        try {
            JsonObject item = JSON.fromJson(reply, JsonObject.class);
            if (item == null || item.size() != 2
                    || !item.has("feature") || !item.has("source_id")) {
                return null;
            }
            String source = textValue(item.get("source_id"));
            String feature = textValue(item.get("feature"));
            if (!product.sourceId().equals(source) || feature == null) {
                return null;
            }
            return product.features().get(feature);
        } catch (JsonParseException invalid) {
            return null;
        }
    }

    static String textValue(JsonElement value) {
        if (value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString()) {
            return value.getAsString();
        }
        return null;
    }

    static String draft(String id, Catalog catalog, Generator generator) {
        Product product;
        try {
            product = catalog.find(id);
        } catch (TimeoutException unavailable) {
            return "Catalog unavailable. " + REVIEW;
        }
        if (product == null) {
            return "Product unavailable.";
        }

        String wording;
        try {
            wording = checkedFeature(generator.propose(product), product);
        } catch (TimeoutException unavailable) {
            wording = null;
        }
        String detail = wording == null ? REVIEW : wording;
        return product.name() + ". " + detail;
    }

    public static void main(String[] args) {
        var product = new Product("Travel charger", "P17-v1", Map.of(
                "USB_C", "Supports USB-C charging.",
                "WEIGHT", "Weighs 120 grams."));
        Catalog catalog = id -> "P17".equals(id) ? product : null;
        var model = new FakeGenerator();
        String valid = """
                {"feature":"USB_C","source_id":"P17-v1"}
                """;
        String review = "Travel charger. " + REVIEW;

        model.reply = valid;
        expect("Travel charger. Supports USB-C charging.",
                draft("P17", catalog, model));
        model.reply = """
                {"feature":"WATERPROOF","source_id":"P17-v1"}
                """;
        expect(review, draft("P17", catalog, model));
        model.reply = """
                {"feature":"USB_C","source_id":"P99-v1"}
                """;
        expect(review, draft("P17", catalog, model));
        model.reply = "{";
        expect(review, draft("P17", catalog, model));
        model.reply = valid;
        expect("Product unavailable.", draft("P99", catalog, model));
        expect(4, model.calls);
        System.out.println("All five catalog cases passed.");
    }

    static class FakeGenerator implements Generator {
        String reply;
        int calls;

        public String propose(Product product) {
            calls++;
            return reply;
        }
    }

    static void expect(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected
                    + ", got " + actual);
        }
    }
}

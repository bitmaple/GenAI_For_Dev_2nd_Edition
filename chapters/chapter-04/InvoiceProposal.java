import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class InvoiceProposal {
    record Evidence(String purpose, long cents, String currency) {}

    private static final Set<String> FIELDS =
        Set.of("totalMinor", "currency", "evidenceId");
    private static final Set<String> CURRENCIES = Set.of("USD", "CAD");

    static String check(String reply, Map<String, Evidence> approved) {
        if (reply == null || reply.length() > 4096) {
            return "MALFORMED";
        }

        Map<String, String> fields;
        long amount;
        try {
            fields = readFields(reply);
            amount = new BigDecimal(fields.get("totalMinor"))
                .longValueExact();
        } catch (IOException | IllegalStateException |
                 IllegalArgumentException | ArithmeticException invalid) {
            return "MALFORMED";
        }

        String currency = fields.get("currency");
        if (amount <= 0 || !CURRENCIES.contains(currency)) {
            return "MALFORMED";
        }
        return checkEvidence(
            approved, fields.get("evidenceId"), amount, currency);
    }

    private static String checkEvidence(Map<String, Evidence> approved,
                                        String id, long amount,
                                        String currency) {
        Evidence evidence = approved.get(id);
        if (evidence == null) {
            return "UNKNOWN_EVIDENCE";
        }
        if (!evidence.purpose().equals("FINAL_TOTAL")) {
            return "WRONG_EVIDENCE";
        }
        if (evidence.cents() != amount ||
            !evidence.currency().equals(currency)) {
            return "VALUE_MISMATCH";
        }
        return "ACCEPTED";
    }

    private static Map<String, String> readFields(String reply)
        throws IOException {
        Map<String, String> fields = new HashMap<>();
        try (JsonReader input = new JsonReader(new StringReader(reply))) {
            input.setStrictness(Strictness.STRICT);
            input.beginObject();
            while (input.hasNext()) {
                String name = input.nextName();
                JsonToken expected = name.equals("totalMinor")
                    ? JsonToken.NUMBER : JsonToken.STRING;
                if (!FIELDS.contains(name) || fields.containsKey(name)
                    || input.peek() != expected) {
                    throw new IOException("Unexpected field or type");
                }
                fields.put(name, input.nextString());
            }
            input.endObject();
            if (input.peek() != JsonToken.END_DOCUMENT
                || !fields.keySet().equals(FIELDS)) {
                throw new IOException("Incomplete or extra data");
            }
        }
        return fields;
    }

    public static void main(String[] args) {
        Map<String, Evidence> invoice = Map.of(
            "E1", new Evidence("FINAL_TOTAL", 12_500, "USD"),
            "E2", new Evidence("SUBTOTAL", 11_500, "USD"));

        String valid = """
            {"totalMinor":12500,"currency":"USD","evidenceId":"E1"}
            """;
        expect(invoice, valid, "ACCEPTED");
        expect(invoice, valid.replace("12500", "12500.0"), "ACCEPTED");
        expect(invoice, valid.replace("12500", "12500.1"), "MALFORMED");
        expect(invoice, valid.replace("12500", "\"12500\""), "MALFORMED");
        expect(invoice, valid.replace("12500", "0"), "MALFORMED");
        String duplicate = valid.replace(
            "12500", "12500,\"totalMinor\":12500");
        expect(invoice, duplicate, "MALFORMED");
        expect(invoice, valid + "{}", "MALFORMED");
        expect(invoice, valid.replace("E1", "E9"), "UNKNOWN_EVIDENCE");
        String subtotal = valid.replace("E1", "E2")
            .replace("12500", "11500");
        expect(invoice, subtotal, "WRONG_EVIDENCE");
        expect(invoice, valid.replace("USD", "CAD"), "VALUE_MISMATCH");
        System.out.println("All invoice proposal cases passed.");
    }

    private static void expect(Map<String, Evidence> approved,
                               String reply, String expected) {
        String actual = check(reply, approved);
        if (!expected.equals(actual)) {
            String message = "Expected " + expected + ", got " + actual;
            throw new AssertionError(message);
        }
    }
}

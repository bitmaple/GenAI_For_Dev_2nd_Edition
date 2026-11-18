import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TrainingSplitAudit {
    enum Split { TRAIN, VALIDATION, TEST }

    enum Problem {
        MALFORMED, DUPLICATE_ID, GROUP_OVERLAP, TEXT_OVERLAP,
        MISSING_SPLIT
    }

    record Row(String id, String group, Split split, String text) {}
    record Finding(Problem problem, String location) {}

    static List<Finding> audit(List<Row> rows) {
        List<Finding> findings = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<Split> present = EnumSet.noneOf(Split.class);
        Map<String, Split> groupSplit = new HashMap<>();
        Map<String, Split> textSplit = new HashMap<>();

        for (Row row : rows) {
            if (!valid(row)) {
                String location = row == null || blank(row.id())
                    ? "unknown row" : row.id();
                findings.add(new Finding(Problem.MALFORMED, location));
                continue;
            }
            if (!ids.add(row.id())) {
                findings.add(new Finding(Problem.DUPLICATE_ID, row.id()));
                continue;
            }

            present.add(row.split());
            savePlacement(groupSplit, row.group(), row,
                          Problem.GROUP_OVERLAP, findings);
            savePlacement(textSplit, fingerprint(row.text()), row,
                          Problem.TEXT_OVERLAP, findings);
        }

        for (Split split : Split.values()) {
            if (!present.contains(split)) {
                findings.add(
                    new Finding(Problem.MISSING_SPLIT, split.name()));
            }
        }
        return List.copyOf(findings);
    }

    private static boolean valid(Row row) {
        return row != null && !blank(row.id()) && !blank(row.group())
            && row.split() != null && !blank(normalize(row.text()));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void savePlacement(Map<String, Split> first,
                                      String key, Row row,
                                      Problem problem,
                                      List<Finding> findings) {
        Split original = first.putIfAbsent(key, row.split());
        if (original != null && original != row.split()) {
            findings.add(new Finding(problem, row.id()));
        }
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC)
            .replaceAll("(?U)\\s+", " ").strip();
    }

    private static String fingerprint(String text) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            String normalized = normalize(text);
            byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("Missing SHA-256", error);
        }
    }

    public static void main(String[] args) {
        Row train = new Row("R1", "G1", Split.TRAIN, "Lamp head loose");
        Row validate = new Row("R2", "G2", Split.VALIDATION, "No signal");
        Row test = new Row("R3", "G3", Split.TEST, "Door does not close");
        List<Row> clean = List.of(train, validate, test);

        expect(clean);
        Row followUp = new Row("R4", "G1", Split.TRAIN, "Loose lamp");
        expect(with(clean, followUp));
        expect(with(clean, new Row("R4", "G1", Split.TEST, "Loose lamp")),
            new Finding(Problem.GROUP_OVERLAP, "R4"));
        Row repeatedText =
            new Row("R5", "G4", Split.TEST, "  Lamp head    loose ");
        expect(with(clean, repeatedText),
            new Finding(Problem.TEXT_OVERLAP, "R5"));
        expect(with(clean,
                   new Row("R1", "G8", Split.TEST, "Replace cover")),
            new Finding(Problem.DUPLICATE_ID, "R1"));
        expect(with(clean, new Row("R6", "", Split.TEST, "Damaged case")),
            new Finding(Problem.MALFORMED, "R6"));
        expect(with(clean, null),
            new Finding(Problem.MALFORMED, "unknown row"));
        expect(clean.subList(0, 2),
            new Finding(Problem.MISSING_SPLIT, "TEST"));
        expect(with(clean,
                   new Row("R7", "G7", Split.VALIDATION, "\u00a0")),
            new Finding(Problem.MALFORMED, "R7"));

        System.out.println("All training split audit checks passed.");
    }

    private static List<Row> with(List<Row> rows, Row extra) {
        List<Row> copy = new ArrayList<>(rows);
        copy.add(extra);
        return copy;
    }

    private static void expect(List<Row> rows, Finding... expected) {
        List<Finding> actual = audit(rows);
        if (!actual.equals(List.of(expected))) {
            throw new AssertionError("Unexpected findings: " + actual);
        }
    }
}

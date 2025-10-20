package sensors;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Try {
    private static final int LOOKAHEAD = 50;

    public static void main(String[] args) {
        // ✅ Change this to your actual file path, or keep relative to the project root
        Path filePath = Paths.get("data/O4H.txt");

        System.out.println("Reading file: " + filePath.toAbsolutePath());
        try {
            List<String> all = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            if (all.isEmpty()) {
                System.out.println("File is empty.");
                return;
            }

            boolean hasHeader = looksLikeHeader(all.get(0));
            int start = hasHeader ? 1 : 0;

            List<Record> rows = new ArrayList<>(Math.max(0, all.size() - start));
            for (int i = start; i < all.size(); i++) {
                Record r = Record.parse(all.get(i));
                if (r != null) rows.add(r);
            }

            int printedPairs = 0;

            // A) presence(ON/1/true) -> within next 50: light*(0/off/false)
            for (int i = 0; i < rows.size(); i++) {
                if (isKitchenPresenceOn(rows.get(i))) {
                    int end = Math.min(rows.size(), i + 1 + LOOKAHEAD);
                    for (int j = i + 1; j < end; j++) {
                        if (isKitchenLightOff(rows.get(j))) {
                            System.out.println(rows.get(i).originalLine);
                            System.out.println(rows.get(j).originalLine);
                            System.out.println();
                            printedPairs++;
                        }
                    }
                }
            }

            // B) light*(0/off/false) -> within next 50: presence(ON/1/true)
            for (int i = 0; i < rows.size(); i++) {
                if (isKitchenLightOff(rows.get(i))) {
                    int end = Math.min(rows.size(), i + 1 + LOOKAHEAD);
                    for (int j = i + 1; j < end; j++) {
                        if (isKitchenPresenceOn(rows.get(j))) {
                            System.out.println(rows.get(i).originalLine);
                            System.out.println(rows.get(j).originalLine);
                            System.out.println();
                            printedPairs++;
                        }
                    }
                }
            }

            System.out.println("Done. Records parsed: " + rows.size()
                    + " | Pairs printed (may include overlaps): " + printedPairs);

        } catch (IOException e) {
            System.err.println("❌ Could not read: " + filePath.toAbsolutePath());
            e.printStackTrace();
        }
    }

    private static boolean looksLikeHeader(String line0) {
        String l = line0.toLowerCase(Locale.ROOT);
        return l.contains("time") && (l.contains("itemname") || l.contains("item") || l.contains("name"))
               && l.contains("value");
    }

    // presence is ON/1/true (case-insensitive)
    private static boolean isKitchenPresenceOn(Record r) {
        if (!"kitchen".equalsIgnoreCase(r.location)) return false;
        if (!"presence".equalsIgnoreCase(r.name) && !r.name.toLowerCase(Locale.ROOT).contains("presence")) return false;
        String v = r.value.trim().toLowerCase(Locale.ROOT);
        return v.equals("on") || v.equals("1") || v.equals("true");
    }

    // light name starts with "light" (e.g., light1, light_1, light1_state) and value is OFF/0/false
    private static boolean isKitchenLightOff(Record r) {
        if (!"kitchen".equalsIgnoreCase(r.location)) return false;
        String n = r.name.toLowerCase(Locale.ROOT);
        if (!n.startsWith("light")) return false;
        String v = r.value.trim().toLowerCase(Locale.ROOT);
        return v.equals("0") || v.equals("off") || v.equals("false");
    }

    static class Record {
        final String originalLine;
        final String time;
        final String itemName;
        final String value;
        final String location; // before first "_"
        final String name;     // after second "_" if present, else after first "_"

        private Record(String originalLine, String time, String itemName, String value,
                       String location, String name) {
            this.originalLine = originalLine;
            this.time = time;
            this.itemName = itemName;
            this.value = value;
            this.location = location;
            this.name = name;
        }

        static Record parse(String line) {
            if (line == null || line.isBlank()) return null;

            // Some logs may contain extra commas in comments; we assume 3 columns max
            String[] cols = line.split(",", 3);
            if (cols.length < 3) return null;

            String time = cols[0].trim();
            String item = cols[1].trim();
            String val  = cols[2].trim();

            // Accept label/meta lines but ignore if we can't extract a location/name
            String[] tokens = item.split("_");
            if (tokens.length < 2) {
                // e.g., "label" without underscores: we cannot derive location+name → skip
                return null;
            }

            String location = tokens[0].trim();               // first token
            String name;
            if (tokens.length >= 3) {
                name = join(tokens, 2, "_");                  // after second underscore
            } else {
                name = tokens[1].trim();                      // fallback: after first underscore
            }
            return new Record(line, time, item, val, location, name);
        }

        private static String join(String[] arr, int start, String sep) {
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < arr.length; i++) {
                if (i > start) sb.append(sep);
                sb.append(arr[i]);
            }
            return sb.toString();
        }
    }
}

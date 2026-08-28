package fastcontacts;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Ultra-fast, low-allocation streaming vCard parser supporting vCard 2.1, 3.0, and 4.0 specifications.
 * Handles RFC-compliant line unfolding, parameter parsing, escaped characters, multi-line structures,
 * and bulk .vcf stream ingestion.
 */
public final class VCardParser {

    private VCardParser() {}

    /**
     * Parses a single vCard string into a Contact.
     */
    public static Contact parse(String vcardText) {
        if (vcardText == null || vcardText.trim().isEmpty()) {
            throw new IllegalArgumentException("vCard text cannot be empty");
        }
        List<Contact> contacts = parseAll(new StringReader(vcardText));
        if (contacts.isEmpty()) {
            throw new IllegalArgumentException("No valid vCard found in input");
        }
        return contacts.get(0);
    }

    /**
     * Parses all vCards in a .vcf formatted string.
     */
    public static List<Contact> parseAll(String vcfContent) {
        if (vcfContent == null || vcfContent.isEmpty()) {
            return Collections.emptyList();
        }
        return parseAll(new StringReader(vcfContent));
    }

    /**
     * Parses all vCards from an InputStream (UTF-8).
     */
    public static List<Contact> parseAll(InputStream inputStream) throws IOException {
        return parseAll(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * Parses all vCards from a Reader with streaming line unfolding.
     */
    public static List<Contact> parseAll(Reader reader) {
        List<Contact> result = new ArrayList<>();
        BufferedReader br = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);

        try {
            String unfoldedLine = null;
            Contact.Builder currentBuilder = null;
            boolean inVCard = false;
            VCardVersion version = VCardVersion.V3_0;

            String rawLine;
            StringBuilder lineAccumulator = new StringBuilder(128);

            while ((rawLine = br.readLine()) != null) {
                // Check for line folding (starts with space or tab)
                if (!rawLine.isEmpty() && (rawLine.charAt(0) == ' ' || rawLine.charAt(0) == '\t')) {
                    if (lineAccumulator.length() > 0) {
                        lineAccumulator.append(rawLine.substring(1));
                    }
                    continue;
                }

                // Process previous accumulated line
                if (lineAccumulator.length() > 0) {
                    unfoldedLine = lineAccumulator.toString().trim();
                    lineAccumulator.setLength(0);

                    if (!unfoldedLine.isEmpty()) {
                        if (unfoldedLine.equalsIgnoreCase("BEGIN:VCARD")) {
                            inVCard = true;
                            currentBuilder = Contact.builder();
                            version = VCardVersion.V3_0;
                        } else if (unfoldedLine.equalsIgnoreCase("END:VCARD")) {
                            if (inVCard && currentBuilder != null) {
                                result.add(currentBuilder.build());
                            }
                            inVCard = false;
                            currentBuilder = null;
                        } else if (inVCard && currentBuilder != null) {
                            processPropertyLine(unfoldedLine, currentBuilder, version);
                        }
                    }
                }

                lineAccumulator.append(rawLine);
            }

            // Process any trailing accumulated line
            if (lineAccumulator.length() > 0) {
                unfoldedLine = lineAccumulator.toString().trim();
                if (unfoldedLine.equalsIgnoreCase("END:VCARD") && inVCard && currentBuilder != null) {
                    result.add(currentBuilder.build());
                } else if (inVCard && currentBuilder != null) {
                    processPropertyLine(unfoldedLine, currentBuilder, version);
                    result.add(currentBuilder.build());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading vCard stream", e);
        }

        return result;
    }

    private static void processPropertyLine(String line, Contact.Builder builder, VCardVersion version) {
        int colonIdx = line.indexOf(':');
        if (colonIdx <= 0) return;

        String propSpec = line.substring(0, colonIdx).trim();
        String rawValue = line.substring(colonIdx + 1);

        // Strip group prefix (e.g., "item1.ADR" -> "ADR")
        int dotIdx = propSpec.indexOf('.');
        if (dotIdx > 0 && dotIdx < propSpec.length() - 1) {
            propSpec = propSpec.substring(dotIdx + 1);
        }

        String propName;
        Map<String, String> params = new LinkedHashMap<>();

        int semiIdx = propSpec.indexOf(';');
        if (semiIdx >= 0) {
            propName = propSpec.substring(0, semiIdx).toUpperCase(Locale.ROOT);
            parseParameters(propSpec.substring(semiIdx + 1), params);
        } else {
            propName = propSpec.toUpperCase(Locale.ROOT);
        }

        String value = unescape(rawValue);

        switch (propName) {
            case "VERSION":
                // version indicator
                break;
            case "UID":
                builder.uid(value);
                break;
            case "FN":
                builder.formattedName(value);
                break;
            case "N":
                parseStructuredName(rawValue, builder);
                break;
            case "NICKNAME":
                builder.nickname(value);
                break;
            case "ORG":
                parseOrganization(rawValue, builder);
                break;
            case "TITLE":
                builder.jobTitle(value);
                break;
            case "ROLE":
                builder.role(value);
                break;
            case "NOTE":
                builder.note(value);
                break;
            case "BDAY":
                builder.birthday(value);
                break;
            case "URL":
                builder.url(value);
                break;
            case "REV":
                builder.revision(value);
                break;
            case "EMAIL":
                builder.addEmail(new ContactProperty("EMAIL", value, params));
                break;
            case "TEL":
                builder.addPhone(new ContactProperty("TEL", value, params));
                break;
            case "ADR":
                parseAddress(rawValue, params, builder);
                break;
            case "CATEGORIES":
                parseCategories(rawValue, builder);
                break;
            case "PHOTO":
                parsePhoto(rawValue, params, builder);
                break;
            default:
                if (propName.startsWith("X-")) {
                    builder.addCustomProperty(propName, value);
                }
                break;
        }
    }

    private static void parseParameters(String paramString, Map<String, String> targetMap) {
        String[] tokens = paramString.split(";");
        for (String token : tokens) {
            int eq = token.indexOf('=');
            if (eq > 0) {
                String k = token.substring(0, eq).trim().toUpperCase(Locale.ROOT);
                String v = token.substring(eq + 1).trim();
                // strip quotes if present
                if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                    v = v.substring(1, v.length() - 1);
                }
                targetMap.put(k, v);
            } else {
                // vCard 2.1 shorthand parameter e.g., ADR;WORK;POSTAL:
                String v = token.trim();
                if (!v.isEmpty()) {
                    String existing = targetMap.get("TYPE");
                    targetMap.put("TYPE", existing != null ? existing + "," + v : v);
                }
            }
        }
    }

    private static void parseStructuredName(String rawN, Contact.Builder builder) {
        // N:Family;Given;Additional;Prefix;Suffix
        String[] parts = splitUnescapedSemicolons(rawN);
        if (parts.length > 0 && !parts[0].isEmpty()) builder.familyName(unescape(parts[0]));
        if (parts.length > 1 && !parts[1].isEmpty()) builder.givenName(unescape(parts[1]));
        if (parts.length > 2 && !parts[2].isEmpty()) builder.additionalNames(unescape(parts[2]));
        if (parts.length > 3 && !parts[3].isEmpty()) builder.honorificPrefix(unescape(parts[3]));
        if (parts.length > 4 && !parts[4].isEmpty()) builder.honorificSuffix(unescape(parts[4]));
    }

    private static void parseOrganization(String rawOrg, Contact.Builder builder) {
        String[] parts = splitUnescapedSemicolons(rawOrg);
        if (parts.length > 0) builder.organization(unescape(parts[0]));
        if (parts.length > 1) builder.department(unescape(parts[1]));
    }

    private static void parseAddress(String rawAdr, Map<String, String> params, Contact.Builder builder) {
        // ADR:po-box;extended;street;locality;region;postal-code;country
        String[] parts = splitUnescapedSemicolons(rawAdr);
        String po = parts.length > 0 ? unescape(parts[0]) : "";
        String ext = parts.length > 1 ? unescape(parts[1]) : "";
        String st = parts.length > 2 ? unescape(parts[2]) : "";
        String city = parts.length > 3 ? unescape(parts[3]) : "";
        String reg = parts.length > 4 ? unescape(parts[4]) : "";
        String zip = parts.length > 5 ? unescape(parts[5]) : "";
        String country = parts.length > 6 ? unescape(parts[6]) : "";

        List<String> types = new ArrayList<>();
        String typeParam = params.get("TYPE");
        if (typeParam != null) {
            for (String t : typeParam.split(",")) {
                types.add(t.trim());
            }
        }
        builder.addAddress(new ContactAddress(po, ext, st, city, reg, zip, country, types));
    }

    private static void parseCategories(String rawCats, Contact.Builder builder) {
        String[] cats = rawCats.split(",");
        for (String c : cats) {
            String cat = unescape(c.trim());
            if (!cat.isEmpty()) {
                builder.addCategory(cat);
            }
        }
    }

    private static void parsePhoto(String rawPhoto, Map<String, String> params, Contact.Builder builder) {
        String mediatype = params.get("TYPE");
        if (mediatype == null) mediatype = params.get("MEDIATYPE");
        if (mediatype != null && !mediatype.contains("/")) {
            mediatype = "image/" + mediatype.toLowerCase(Locale.ROOT);
        }
        builder.photoBase64(rawPhoto.trim().replaceAll("\\s+", ""), mediatype);
    }

    private static String[] splitUnescapedSemicolons(String str) {
        List<String> list = new ArrayList<>(7);
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
                sb.append(c);
            } else if (c == ';') {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }

    public static String unescape(String input) {
        if (input == null || !input.contains("\\")) return input;
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == 'n' || next == 'N') {
                    sb.append('\n');
                    i++;
                } else if (next == ',' || next == ';' || next == '\\' || next == ':') {
                    sb.append(next);
                    i++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

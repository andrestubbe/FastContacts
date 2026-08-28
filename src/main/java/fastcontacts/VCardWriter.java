package fastcontacts;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * High-performance vCard serializer supporting vCard 3.0 and 4.0 specifications.
 * Implements strict RFC line folding (75 octets) and proper character escaping.
 */
public final class VCardWriter {

    private static final String CRLF = "\r\n";
    private static final int MAX_LINE_LENGTH = 75;

    private VCardWriter() {}

    /**
     * Serializes a single contact to a vCard string (vCard 3.0 by default).
     */
    public static String toVCard(Contact contact) {
        return toVCard(contact, VCardVersion.V3_0);
    }

    /**
     * Serializes a single contact to a vCard string in the specified version.
     */
    public static String toVCard(Contact contact, VCardVersion version) {
        StringBuilder sb = new StringBuilder(512);
        writeContact(contact, version, sb);
        return sb.toString();
    }

    /**
     * Serializes a collection of contacts to a .vcf formatted string.
     */
    public static String toVcf(Collection<Contact> contacts) {
        return toVcf(contacts, VCardVersion.V3_0);
    }

    /**
     * Serializes a collection of contacts to a .vcf formatted string with the specified version.
     */
    public static String toVcf(Collection<Contact> contacts, VCardVersion version) {
        StringBuilder sb = new StringBuilder(contacts.size() * 512);
        for (Contact contact : contacts) {
            writeContact(contact, version, sb);
        }
        return sb.toString();
    }

    /**
     * Writes all contacts directly to a Writer.
     */
    public static void writeTo(Collection<Contact> contacts, VCardVersion version, Writer writer) throws IOException {
        for (Contact contact : contacts) {
            StringBuilder sb = new StringBuilder(512);
            writeContact(contact, version, sb);
            writer.write(sb.toString());
        }
        writer.flush();
    }

    private static void writeContact(Contact c, VCardVersion version, StringBuilder out) {
        appendFoldedLine(out, "BEGIN:VCARD");
        appendFoldedLine(out, "VERSION:" + version.getVersionString());
        if (c.getUid() != null && !c.getUid().isEmpty()) {
            appendFoldedLine(out, "UID:" + escapeText(c.getUid()));
        }
        if (c.getFormattedName() != null && !c.getFormattedName().isEmpty()) {
            appendFoldedLine(out, "FN:" + escapeText(c.getFormattedName()));
        }

        // Structured Name N:Family;Given;Additional;Prefix;Suffix
        String nValue = escapeValue(c.getFamilyName()) + ";" +
                        escapeValue(c.getGivenName()) + ";" +
                        escapeValue(c.getAdditionalNames()) + ";" +
                        escapeValue(c.getHonorificPrefix()) + ";" +
                        escapeValue(c.getHonorificSuffix());
        appendFoldedLine(out, "N:" + nValue);

        if (!c.getNickname().isEmpty()) {
            appendFoldedLine(out, "NICKNAME:" + escapeText(c.getNickname()));
        }

        if (!c.getOrganization().isEmpty() || !c.getDepartment().isEmpty()) {
            String orgVal = escapeValue(c.getOrganization());
            if (!c.getDepartment().isEmpty()) {
                orgVal += ";" + escapeValue(c.getDepartment());
            }
            appendFoldedLine(out, "ORG:" + orgVal);
        }

        if (!c.getJobTitle().isEmpty()) {
            appendFoldedLine(out, "TITLE:" + escapeText(c.getJobTitle()));
        }
        if (!c.getRole().isEmpty()) {
            appendFoldedLine(out, "ROLE:" + escapeText(c.getRole()));
        }

        for (ContactProperty email : c.getEmails()) {
            writeProperty(out, email);
        }

        for (ContactProperty phone : c.getPhones()) {
            writeProperty(out, phone);
        }

        for (ContactAddress addr : c.getAddresses()) {
            writeAddress(out, addr);
        }

        if (!c.getCategories().isEmpty()) {
            StringBuilder cats = new StringBuilder();
            for (int i = 0; i < c.getCategories().size(); i++) {
                if (i > 0) cats.append(",");
                cats.append(escapeText(c.getCategories().get(i)));
            }
            appendFoldedLine(out, "CATEGORIES:" + cats.toString());
        }

        if (!c.getNote().isEmpty()) {
            appendFoldedLine(out, "NOTE:" + escapeText(c.getNote()));
        }

        if (!c.getBirthday().isEmpty()) {
            appendFoldedLine(out, "BDAY:" + escapeText(c.getBirthday()));
        }

        if (!c.getUrl().isEmpty()) {
            appendFoldedLine(out, "URL:" + escapeText(c.getUrl()));
        }

        if (!c.getPhotoBase64().isEmpty()) {
            String prop = "PHOTO;ENCODING=b;TYPE=" + c.getPhotoMediaType().replace("image/", "").toUpperCase(Locale.ROOT) + ":" + c.getPhotoBase64();
            appendFoldedLine(out, prop);
        }

        if (!c.getRevision().isEmpty()) {
            appendFoldedLine(out, "REV:" + c.getRevision());
        }

        for (Map.Entry<String, String> custom : c.getCustomProperties().entrySet()) {
            appendFoldedLine(out, custom.getKey() + ":" + escapeText(custom.getValue()));
        }

        appendFoldedLine(out, "END:VCARD");
    }

    private static void writeProperty(StringBuilder out, ContactProperty prop) {
        StringBuilder sb = new StringBuilder(prop.getName());
        for (Map.Entry<String, String> param : prop.getParameters().entrySet()) {
            sb.append(";").append(param.getKey()).append("=").append(param.getValue());
        }
        sb.append(":").append(escapeText(prop.getValue()));
        appendFoldedLine(out, sb.toString());
    }

    private static void writeAddress(StringBuilder out, ContactAddress addr) {
        StringBuilder sb = new StringBuilder("ADR");
        if (!addr.getTypes().isEmpty()) {
            sb.append(";TYPE=").append(String.join(",", addr.getTypes()));
        }
        sb.append(":")
          .append(escapeValue(addr.getPoBox())).append(";")
          .append(escapeValue(addr.getExtended())).append(";")
          .append(escapeValue(addr.getStreet())).append(";")
          .append(escapeValue(addr.getCity())).append(";")
          .append(escapeValue(addr.getRegion())).append(";")
          .append(escapeValue(addr.getPostalCode())).append(";")
          .append(escapeValue(addr.getCountry()));
        appendFoldedLine(out, sb.toString());
    }

    private static void appendFoldedLine(StringBuilder out, String line) {
        if (line.length() <= MAX_LINE_LENGTH) {
            out.append(line).append(CRLF);
            return;
        }

        int index = 0;
        int len = line.length();
        boolean first = true;

        while (index < len) {
            int chunkLimit = first ? MAX_LINE_LENGTH : MAX_LINE_LENGTH - 1;
            int nextIndex = Math.min(index + chunkLimit, len);
            if (!first) {
                out.append(" ");
            }
            out.append(line, index, nextIndex).append(CRLF);
            index = nextIndex;
            first = false;
        }
    }

    private static String escapeValue(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.replace("\\", "\\\\")
                  .replace(";", "\\;")
                  .replace(",", "\\,")
                  .replace("\r\n", "\\n")
                  .replace("\n", "\\n");
    }

    private static String escapeText(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.replace("\\", "\\\\")
                  .replace("\r\n", "\\n")
                  .replace("\n", "\\n");
    }
}

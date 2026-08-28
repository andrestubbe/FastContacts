package fastcontacts.ansi;

import fastcontacts.Contact;
import fastcontacts.ContactAddress;
import fastcontacts.ContactProperty;

import java.util.*;

/**
 * 120-Column FastANSI Terminal HUD for Contact Visualizations, Metrics, and CardDAV Sync Trees.
 * Adheres strictly to FastJava formatting standards:
 * - 120-column terminal framing
 * - Dark gray tree branches (├──, └──, │)
 * - Bold white values
 * - Middle-path / middle-string truncation for long identifiers and URLs
 */
public final class FastAnsiContactsHUD {

    public static final int TERM_WIDTH = 120;

    // ANSI Color and Style Codes
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";

    // Standard FastJava Palette
    public static final String COLOR_DARK_GRAY = "\u001B[90m";
    public static final String COLOR_BOLD_WHITE = "\u001B[1;97m";
    public static final String COLOR_CYAN = "\u001B[36m";
    public static final String COLOR_BOLD_CYAN = "\u001B[1;36m";
    public static final String COLOR_GREEN = "\u001B[92m";
    public static final String COLOR_BOLD_GREEN = "\u001B[1;92m";
    public static final String COLOR_YELLOW = "\u001B[93m";
    public static final String COLOR_BOLD_YELLOW = "\u001B[1;93m";
    public static final String COLOR_MAGENTA = "\u001B[95m";
    public static final String COLOR_BOLD_MAGENTA = "\u001B[1;95m";
    public static final String COLOR_BLUE = "\u001B[34m";
    public static final String COLOR_RED = "\u001B[91m";

    private FastAnsiContactsHUD() {}

    /**
     * Renders a 120-column header banner.
     */
    public static void printBanner(String title, String subtitle) {
        String top = COLOR_DARK_GRAY + "┌" + "─".repeat(TERM_WIDTH - 2) + "┐" + RESET;
        String bottom = COLOR_DARK_GRAY + "└" + "─".repeat(TERM_WIDTH - 2) + "┘" + RESET;

        System.out.println(top);
        printCenteredLine(COLOR_BOLD_CYAN + title + RESET);
        if (subtitle != null && !subtitle.isEmpty()) {
            printCenteredLine(COLOR_DARK_GRAY + subtitle + RESET);
        }
        System.out.println(bottom);
    }

    /**
     * Renders a section divider header across 120 columns.
     */
    public static void printSection(String sectionTitle) {
        String titleFormatted = " " + COLOR_BOLD_YELLOW + "⚡ " + sectionTitle + " " + RESET + COLOR_DARK_GRAY;
        int rawLen = stripAnsi(titleFormatted).length();
        int remaining = TERM_WIDTH - 4 - rawLen;
        int left = 3;
        int right = Math.max(0, remaining - left);

        System.out.println(COLOR_DARK_GRAY + "├" + "─".repeat(left) + titleFormatted + "─".repeat(right) + "┤" + RESET);
    }

    /**
     * Renders a 120-column formatted Contact Card with dark gray tree branches and bold white values.
     */
    public static void printContactCard(Contact contact, int index, int total) {
        String countBadge = total > 0 ? String.format("[%d/%d] ", index, total) : "";
        String title = COLOR_BOLD_WHITE + countBadge + contact.getFormattedName() + RESET;
        if (!contact.getOrganization().isEmpty()) {
            title += " " + COLOR_DARK_GRAY + "@ " + COLOR_BOLD_CYAN + contact.getOrganization() + RESET;
        }
        if (!contact.getJobTitle().isEmpty()) {
            title += " " + COLOR_DARK_GRAY + "(" + COLOR_YELLOW + contact.getJobTitle() + COLOR_DARK_GRAY + ")" + RESET;
        }

        System.out.println(COLOR_DARK_GRAY + "┌─ " + title);

        // UID and ETag line
        String uidStr = truncateMiddle(contact.getUid(), 36);
        System.out.println(treeBranch(false) + "UID: " + COLOR_BOLD_WHITE + uidStr + RESET +
                COLOR_DARK_GRAY + " │ ETag: " + COLOR_YELLOW + contact.getEtag() + RESET +
                COLOR_DARK_GRAY + " │ Rev: " + COLOR_DARK_GRAY + contact.getRevision() + RESET);

        // Emails
        if (!contact.getEmails().isEmpty()) {
            for (int i = 0; i < contact.getEmails().size(); i++) {
                ContactProperty email = contact.getEmails().get(i);
                boolean isLast = i == contact.getEmails().size() - 1 && contact.getPhones().isEmpty() &&
                        contact.getAddresses().isEmpty() && contact.getCategories().isEmpty() && contact.getNote().isEmpty();
                String pref = email.isPreferred() ? " " + COLOR_BOLD_GREEN + "[PRIMARY]" + RESET : "";
                String types = email.getParameter("TYPE") != null ? COLOR_DARK_GRAY + " (" + email.getParameter("TYPE") + ")" + RESET : "";
                System.out.println(treeBranch(isLast) + "Email: " + COLOR_BOLD_WHITE + email.getValue() + RESET + types + pref);
            }
        }

        // Phones
        if (!contact.getPhones().isEmpty()) {
            for (int i = 0; i < contact.getPhones().size(); i++) {
                ContactProperty phone = contact.getPhones().get(i);
                boolean isLast = i == contact.getPhones().size() - 1 && contact.getAddresses().isEmpty() &&
                        contact.getCategories().isEmpty() && contact.getNote().isEmpty();
                String pref = phone.isPreferred() ? " " + COLOR_BOLD_GREEN + "[PRIMARY]" + RESET : "";
                String types = phone.getParameter("TYPE") != null ? COLOR_DARK_GRAY + " (" + phone.getParameter("TYPE") + ")" + RESET : "";
                System.out.println(treeBranch(isLast) + "Phone: " + COLOR_BOLD_WHITE + phone.getValue() + RESET + types + pref);
            }
        }

        // Addresses
        if (!contact.getAddresses().isEmpty()) {
            for (int i = 0; i < contact.getAddresses().size(); i++) {
                ContactAddress addr = contact.getAddresses().get(i);
                boolean isLast = i == contact.getAddresses().size() - 1 && contact.getCategories().isEmpty() && contact.getNote().isEmpty();
                String formatted = addr.getFormattedSingleLine();
                System.out.println(treeBranch(isLast) + "Address: " + COLOR_BOLD_WHITE + formatted + RESET);
            }
        }

        // Categories
        if (!contact.getCategories().isEmpty()) {
            boolean isLast = contact.getNote().isEmpty();
            StringBuilder sb = new StringBuilder();
            for (String cat : contact.getCategories()) {
                sb.append(COLOR_BOLD_MAGENTA).append("[").append(cat).append("] ").append(RESET);
            }
            System.out.println(treeBranch(isLast) + "Tags: " + sb.toString());
        }

        // Note
        if (!contact.getNote().isEmpty()) {
            String note = truncateMiddle(contact.getNote(), 80);
            System.out.println(treeBranch(true) + "Note: " + COLOR_DARK_GRAY + "\"" + COLOR_BOLD_WHITE + note + COLOR_DARK_GRAY + "\"" + RESET);
        }

        System.out.println(COLOR_DARK_GRAY + "└" + "─".repeat(TERM_WIDTH - 2) + RESET);
    }

    /**
     * Renders a live performance metrics dashboard row.
     */
    public static void printMetricRow(String label, String value, String unit, String speedBadge) {
        String lbl = COLOR_DARK_GRAY + "  ├── " + COLOR_BOLD_WHITE + padRight(label, 32) + RESET;
        String val = COLOR_BOLD_GREEN + padRight(value, 18) + RESET;
        String u = COLOR_DARK_GRAY + padRight(unit, 16) + RESET;
        String badge = speedBadge != null ? COLOR_BOLD_YELLOW + "[" + speedBadge + "]" + RESET : "";

        String raw = stripAnsi(lbl + val + u + badge);
        int pad = Math.max(0, TERM_WIDTH - 2 - raw.length());
        System.out.println(lbl + val + u + badge + " ".repeat(pad));
    }

    /**
     * Truncates string in the middle with "..." if exceeding maxWidth.
     */
    public static String truncateMiddle(String str, int maxWidth) {
        if (str == null) return "";
        if (str.length() <= maxWidth) return str;
        if (maxWidth <= 5) return str.substring(0, maxWidth);
        int half = (maxWidth - 3) / 2;
        int remainder = (maxWidth - 3) - half;
        return str.substring(0, half) + "..." + str.substring(str.length() - remainder);
    }

    private static String treeBranch(boolean isLast) {
        return COLOR_DARK_GRAY + (isLast ? "  └── " : "  ├── ") + RESET;
    }

    private static void printCenteredLine(String textWithAnsi) {
        int visibleLength = stripAnsi(textWithAnsi).length();
        int totalPadding = TERM_WIDTH - 2 - visibleLength;
        int leftPad = Math.max(0, totalPadding / 2);
        int rightPad = Math.max(0, totalPadding - leftPad);

        System.out.println(COLOR_DARK_GRAY + "│" + RESET + " ".repeat(leftPad) + textWithAnsi + " ".repeat(rightPad) + COLOR_DARK_GRAY + "│" + RESET);
    }

    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public static String stripAnsi(String str) {
        if (str == null) return "";
        return str.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}

package fastcontacts;

/**
 * Supported vCard specification versions.
 */
public enum VCardVersion {
    V2_1("2.1"),
    V3_0("3.0"),
    V4_0("4.0");

    private final String versionString;

    VCardVersion(String versionString) {
        this.versionString = versionString;
    }

    public String getVersionString() {
        return versionString;
    }

    public static VCardVersion fromString(String str) {
        if (str == null) return V3_0;
        String trimmed = str.trim();
        if (trimmed.equals("4.0")) return V4_0;
        if (trimmed.equals("2.1")) return V2_1;
        return V3_0;
    }
}

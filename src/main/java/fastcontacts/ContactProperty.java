package fastcontacts;

import java.util.*;

/**
 * Represents a generic vCard property with associated parameters (e.g. TYPE=WORK,VOICE;PREF=1).
 */
public final class ContactProperty {
    private final String name;
    private final String value;
    private final Map<String, String> parameters;

    public ContactProperty(String name, String value) {
        this(name, value, Collections.emptyMap());
    }

    public ContactProperty(String name, String value, Map<String, String> parameters) {
        this.name = Objects.requireNonNull(name, "name must not be null").toUpperCase(Locale.ROOT);
        this.value = value != null ? value : "";
        if (parameters == null || parameters.isEmpty()) {
            this.parameters = Collections.emptyMap();
        } else {
            Map<String, String> map = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                map.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue());
            }
            this.parameters = Collections.unmodifiableMap(map);
        }
    }

    public static ContactProperty of(String name, String value, String... typeParams) {
        if (typeParams == null || typeParams.length == 0) {
            return new ContactProperty(name, value);
        }
        Map<String, String> params = new LinkedHashMap<>(2);
        params.put("TYPE", String.join(",", typeParams));
        return new ContactProperty(name, value, params);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameter(String paramName) {
        if (paramName == null) return null;
        return parameters.get(paramName.toUpperCase(Locale.ROOT));
    }

    public boolean hasType(String typeName) {
        if (typeName == null) return false;
        String typeParam = getParameter("TYPE");
        if (typeParam == null) return false;
        String[] types = typeParam.split(",");
        for (String t : types) {
            if (t.trim().equalsIgnoreCase(typeName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPreferred() {
        String pref = getParameter("PREF");
        if (pref != null && (pref.equals("1") || pref.equalsIgnoreCase("true"))) return true;
        return hasType("PREF");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactProperty)) return false;
        ContactProperty that = (ContactProperty) o;
        return name.equals(that.name) && value.equals(that.value) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, parameters);
    }

    @Override
    public String toString() {
        if (parameters.isEmpty()) {
            return name + ":" + value;
        }
        StringBuilder sb = new StringBuilder(name);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            sb.append(';').append(entry.getKey()).append('=').append(entry.getValue());
        }
        sb.append(':').append(value);
        return sb.toString();
    }
}

package fastcontacts;

import java.util.*;

/**
 * Structured Postal Address representing the vCard ADR property.
 * RFC 6352 / RFC 6350 / RFC 2426 format:
 * ADR:po-box;extended;street;locality;region;postal-code;country
 */
public final class ContactAddress {
    private final String poBox;
    private final String extended;
    private final String street;
    private final String city;
    private final String region;
    private final String postalCode;
    private final String country;
    private final List<String> types;

    public ContactAddress(String poBox, String extended, String street, String city, String region, String postalCode, String country, List<String> types) {
        this.poBox = poBox != null ? poBox : "";
        this.extended = extended != null ? extended : "";
        this.street = street != null ? street : "";
        this.city = city != null ? city : "";
        this.region = region != null ? region : "";
        this.postalCode = postalCode != null ? postalCode : "";
        this.country = country != null ? country : "";
        this.types = types != null ? Collections.unmodifiableList(new ArrayList<>(types)) : Collections.emptyList();
    }

    public static ContactAddress of(String street, String city, String region, String postalCode, String country, String... types) {
        return new ContactAddress("", "", street, city, region, postalCode, country, types != null ? Arrays.asList(types) : Collections.emptyList());
    }

    public String getPoBox() { return poBox; }
    public String getExtended() { return extended; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public List<String> getTypes() { return types; }

    public String getFormattedSingleLine() {
        StringBuilder sb = new StringBuilder();
        if (!street.isEmpty()) sb.append(street);
        if (!city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (!region.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(region);
        }
        if (!postalCode.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(postalCode);
        }
        if (!country.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactAddress)) return false;
        ContactAddress that = (ContactAddress) o;
        return poBox.equals(that.poBox) && extended.equals(that.extended) && street.equals(that.street) &&
                city.equals(that.city) && region.equals(that.region) && postalCode.equals(that.postalCode) &&
                country.equals(that.country) && types.equals(that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poBox, extended, street, city, region, postalCode, country, types);
    }

    @Override
    public String toString() {
        return getFormattedSingleLine();
    }
}

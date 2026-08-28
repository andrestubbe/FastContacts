package fastcontacts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Immutable, high-performance contact model representing a complete vCard entry.
 */
public final class Contact {
    private final String uid;
    private final String formattedName;
    private final String familyName;
    private final String givenName;
    private final String additionalNames;
    private final String honorificPrefix;
    private final String honorificSuffix;
    private final String nickname;
    private final String organization;
    private final String department;
    private final String jobTitle;
    private final String role;
    private final String note;
    private final String birthday; // YYYY-MM-DD or ISO string
    private final String url;
    private final String photoBase64;
    private final String photoMediaType;
    private final String revision;
    private final List<ContactProperty> emails;
    private final List<ContactProperty> phones;
    private final List<ContactAddress> addresses;
    private final List<String> categories;
    private final Map<String, String> customProperties;
    private final String etag;

    private Contact(Builder builder) {
        this.uid = builder.uid != null && !builder.uid.isEmpty() ? builder.uid : UUID.randomUUID().toString();
        this.formattedName = builder.formattedName != null ? builder.formattedName : computeFormattedName(builder);
        this.familyName = builder.familyName != null ? builder.familyName : "";
        this.givenName = builder.givenName != null ? builder.givenName : "";
        this.additionalNames = builder.additionalNames != null ? builder.additionalNames : "";
        this.honorificPrefix = builder.honorificPrefix != null ? builder.honorificPrefix : "";
        this.honorificSuffix = builder.honorificSuffix != null ? builder.honorificSuffix : "";
        this.nickname = builder.nickname != null ? builder.nickname : "";
        this.organization = builder.organization != null ? builder.organization : "";
        this.department = builder.department != null ? builder.department : "";
        this.jobTitle = builder.jobTitle != null ? builder.jobTitle : "";
        this.role = builder.role != null ? builder.role : "";
        this.note = builder.note != null ? builder.note : "";
        this.birthday = builder.birthday != null ? builder.birthday : "";
        this.url = builder.url != null ? builder.url : "";
        this.photoBase64 = builder.photoBase64 != null ? builder.photoBase64 : "";
        this.photoMediaType = builder.photoMediaType != null ? builder.photoMediaType : "image/jpeg";
        this.revision = builder.revision != null ? builder.revision : Instant.now().toString();
        this.emails = Collections.unmodifiableList(new ArrayList<>(builder.emails));
        this.phones = Collections.unmodifiableList(new ArrayList<>(builder.phones));
        this.addresses = Collections.unmodifiableList(new ArrayList<>(builder.addresses));
        this.categories = Collections.unmodifiableList(new ArrayList<>(builder.categories));
        this.customProperties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.customProperties));
        this.etag = builder.etag != null ? builder.etag : computeEtag();
    }

    private static String computeFormattedName(Builder b) {
        StringBuilder sb = new StringBuilder();
        if (b.honorificPrefix != null && !b.honorificPrefix.isEmpty()) sb.append(b.honorificPrefix).append(" ");
        if (b.givenName != null && !b.givenName.isEmpty()) sb.append(b.givenName).append(" ");
        if (b.additionalNames != null && !b.additionalNames.isEmpty()) sb.append(b.additionalNames).append(" ");
        if (b.familyName != null && !b.familyName.isEmpty()) sb.append(b.familyName);
        if (b.honorificSuffix != null && !b.honorificSuffix.isEmpty()) sb.append(", ").append(b.honorificSuffix);
        String name = sb.toString().trim();
        if (name.isEmpty() && b.organization != null && !b.organization.isEmpty()) {
            return b.organization;
        }
        return name.isEmpty() ? "Unnamed Contact" : name;
    }

    private String computeEtag() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(uid.getBytes(StandardCharsets.UTF_8));
            md.update(formattedName.getBytes(StandardCharsets.UTF_8));
            md.update(organization.getBytes(StandardCharsets.UTF_8));
            md.update(revision.getBytes(StandardCharsets.UTF_8));
            for (ContactProperty cp : emails) md.update(cp.getValue().getBytes(StandardCharsets.UTF_8));
            for (ContactProperty cp : phones) md.update(cp.getValue().getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder hex = new StringBuilder(18);
            hex.append('"');
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            hex.append('"');
            return hex.toString();
        } catch (Exception e) {
            return "\"" + Integer.toHexString(hashCode()) + "\"";
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public String getUid() { return uid; }
    public String getFormattedName() { return formattedName; }
    public String getFamilyName() { return familyName; }
    public String getGivenName() { return givenName; }
    public String getAdditionalNames() { return additionalNames; }
    public String getHonorificPrefix() { return honorificPrefix; }
    public String getHonorificSuffix() { return honorificSuffix; }
    public String getNickname() { return nickname; }
    public String getOrganization() { return organization; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public String getRole() { return role; }
    public String getNote() { return note; }
    public String getBirthday() { return birthday; }
    public String getUrl() { return url; }
    public String getPhotoBase64() { return photoBase64; }
    public String getPhotoMediaType() { return photoMediaType; }
    public String getRevision() { return revision; }
    public List<ContactProperty> getEmails() { return emails; }
    public List<ContactProperty> getPhones() { return phones; }
    public List<ContactAddress> getAddresses() { return addresses; }
    public List<String> getCategories() { return categories; }
    public Map<String, String> getCustomProperties() { return customProperties; }
    public String getEtag() { return etag; }

    public Optional<String> getPrimaryEmail() {
        if (emails.isEmpty()) return Optional.empty();
        for (ContactProperty p : emails) {
            if (p.isPreferred()) return Optional.of(p.getValue());
        }
        return Optional.of(emails.get(0).getValue());
    }

    public Optional<String> getPrimaryPhone() {
        if (phones.isEmpty()) return Optional.empty();
        for (ContactProperty p : phones) {
            if (p.isPreferred()) return Optional.of(p.getValue());
        }
        return Optional.of(phones.get(0).getValue());
    }

    public Optional<ContactAddress> getPrimaryAddress() {
        if (addresses.isEmpty()) return Optional.empty();
        return Optional.of(addresses.get(0));
    }

    public static final class Builder {
        private String uid;
        private String formattedName;
        private String familyName;
        private String givenName;
        private String additionalNames;
        private String honorificPrefix;
        private String honorificSuffix;
        private String nickname;
        private String organization;
        private String department;
        private String jobTitle;
        private String role;
        private String note;
        private String birthday;
        private String url;
        private String photoBase64;
        private String photoMediaType = "image/jpeg";
        private String revision;
        private String etag;
        private final List<ContactProperty> emails = new ArrayList<>();
        private final List<ContactProperty> phones = new ArrayList<>();
        private final List<ContactAddress> addresses = new ArrayList<>();
        private final List<String> categories = new ArrayList<>();
        private final Map<String, String> customProperties = new LinkedHashMap<>();

        public Builder() {}

        public Builder(Contact c) {
            this.uid = c.uid;
            this.formattedName = c.formattedName;
            this.familyName = c.familyName;
            this.givenName = c.givenName;
            this.additionalNames = c.additionalNames;
            this.honorificPrefix = c.honorificPrefix;
            this.honorificSuffix = c.honorificSuffix;
            this.nickname = c.nickname;
            this.organization = c.organization;
            this.department = c.department;
            this.jobTitle = c.jobTitle;
            this.role = c.role;
            this.note = c.note;
            this.birthday = c.birthday;
            this.url = c.url;
            this.photoBase64 = c.photoBase64;
            this.photoMediaType = c.photoMediaType;
            this.revision = c.revision;
            this.etag = c.etag;
            this.emails.addAll(c.emails);
            this.phones.addAll(c.phones);
            this.addresses.addAll(c.addresses);
            this.categories.addAll(c.categories);
            this.customProperties.putAll(c.customProperties);
        }

        public Builder uid(String uid) { this.uid = uid; return this; }
        public Builder formattedName(String formattedName) { this.formattedName = formattedName; return this; }
        public Builder familyName(String familyName) { this.familyName = familyName; return this; }
        public Builder givenName(String givenName) { this.givenName = givenName; return this; }
        public Builder additionalNames(String additionalNames) { this.additionalNames = additionalNames; return this; }
        public Builder honorificPrefix(String honorificPrefix) { this.honorificPrefix = honorificPrefix; return this; }
        public Builder honorificSuffix(String honorificSuffix) { this.honorificSuffix = honorificSuffix; return this; }
        public Builder nickname(String nickname) { this.nickname = nickname; return this; }
        public Builder organization(String organization) { this.organization = organization; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public Builder jobTitle(String jobTitle) { this.jobTitle = jobTitle; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder note(String note) { this.note = note; return this; }
        public Builder birthday(String birthday) { this.birthday = birthday; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder photoBase64(String photoBase64, String mediaType) {
            this.photoBase64 = photoBase64;
            this.photoMediaType = mediaType != null ? mediaType : "image/jpeg";
            return this;
        }
        public Builder revision(String revision) { this.revision = revision; return this; }
        public Builder etag(String etag) { this.etag = etag; return this; }

        public Builder addEmail(String email, String... types) {
            if (email != null && !email.trim().isEmpty()) {
                this.emails.add(ContactProperty.of("EMAIL", email.trim(), types));
            }
            return this;
        }

        public Builder addEmail(ContactProperty emailProp) {
            if (emailProp != null) this.emails.add(emailProp);
            return this;
        }

        public Builder addPhone(String phone, String... types) {
            if (phone != null && !phone.trim().isEmpty()) {
                this.phones.add(ContactProperty.of("TEL", phone.trim(), types));
            }
            return this;
        }

        public Builder addPhone(ContactProperty phoneProp) {
            if (phoneProp != null) this.phones.add(phoneProp);
            return this;
        }

        public Builder addAddress(ContactAddress address) {
            if (address != null) this.addresses.add(address);
            return this;
        }

        public Builder addCategory(String category) {
            if (category != null && !category.trim().isEmpty()) {
                this.categories.add(category.trim());
            }
            return this;
        }

        public Builder addCustomProperty(String key, String value) {
            if (key != null && value != null) {
                this.customProperties.put(key, value);
            }
            return this;
        }

        public Contact build() {
            return new Contact(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;
        Contact contact = (Contact) o;
        return uid.equals(contact.uid);
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }

    @Override
    public String toString() {
        return "Contact{" +
                "uid='" + uid + '\'' +
                ", formattedName='" + formattedName + '\'' +
                ", organization='" + organization + '\'' +
                ", emails=" + emails +
                ", phones=" + phones +
                '}';
    }
}

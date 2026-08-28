package fastcontacts.carddav;

import fastcontacts.Contact;
import fastcontacts.FastContactsRegistry;
import fastcontacts.VCardVersion;
import fastcontacts.VCardWriter;

import java.util.*;

/**
 * High-speed XML generator and lightweight parser for CardDAV (RFC 6352) and WebDAV (RFC 4918).
 * Optimized to avoid heavy DOM dependencies while maintaining strict RFC compliance.
 */
public final class CardDavXml {

    private CardDavXml() {}

    /**
     * Builds a WebDAV Multi-Status response for Address Book Discovery (PROPFIND).
     */
    public static String buildDiscoveryResponse(String basePath, String syncToken) {
        StringBuilder xml = new StringBuilder(1024);
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
        xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">\n");

        // Root addressbook collection response
        xml.append("  <D:response>\n");
        xml.append("    <D:href>").append(basePath).append("/</D:href>\n");
        xml.append("    <D:propstat>\n");
        xml.append("      <D:prop>\n");
        xml.append("        <D:resourcetype><D:collection/><C:addressbook/></D:resourcetype>\n");
        xml.append("        <D:displayname>FastContacts Address Book</D:displayname>\n");
        xml.append("        <C:addressbook-description>High-Speed FastJava Contacts Registry</C:addressbook-description>\n");
        xml.append("        <C:supported-address-data>\n");
        xml.append("          <C:address-data-type content-type=\"text/vcard\" version=\"3.0\"/>\n");
        xml.append("          <C:address-data-type content-type=\"text/vcard\" version=\"4.0\"/>\n");
        xml.append("        </C:supported-address-data>\n");
        xml.append("        <D:sync-token>").append(syncToken).append("</D:sync-token>\n");
        xml.append("      </D:prop>\n");
        xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
        xml.append("    </D:propstat>\n");
        xml.append("  </D:response>\n");

        xml.append("</D:multistatus>");
        return xml.toString();
    }

    /**
     * Builds a Multi-Status response for addressbook-query or addressbook-multiget REPORT.
     */
    public static String buildAddressbookQueryResponse(String basePath, Collection<Contact> contacts, boolean includeAddressData, VCardVersion version) {
        StringBuilder xml = new StringBuilder(contacts.size() * 1024 + 256);
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
        xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">\n");

        for (Contact contact : contacts) {
            String href = basePath + (basePath.endsWith("/") ? "" : "/") + contact.getUid() + ".vcf";
            xml.append("  <D:response>\n");
            xml.append("    <D:href>").append(href).append("</D:href>\n");
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            xml.append("        <D:getetag>").append(contact.getEtag()).append("</D:getetag>\n");
            xml.append("        <D:getcontenttype>text/vcard; charset=utf-8</D:getcontenttype>\n");
            if (includeAddressData) {
                String vcard = VCardWriter.toVCard(contact, version);
                xml.append("        <C:address-data><![CDATA[").append(vcard).append("]]></C:address-data>\n");
            }
            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");
        }

        xml.append("</D:multistatus>");
        return xml.toString();
    }

    /**
     * Builds a Multi-Status response for RFC 6578 sync-collection REPORT.
     */
    public static String buildSyncCollectionResponse(String basePath, FastContactsRegistry.SyncDelta delta, boolean includeAddressData, VCardVersion version) {
        StringBuilder xml = new StringBuilder(1024);
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
        xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">\n");

        // Updated / Added contacts
        for (Contact contact : delta.getUpdatedContacts()) {
            String href = basePath + (basePath.endsWith("/") ? "" : "/") + contact.getUid() + ".vcf";
            xml.append("  <D:response>\n");
            xml.append("    <D:href>").append(href).append("</D:href>\n");
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            xml.append("        <D:getetag>").append(contact.getEtag()).append("</D:getetag>\n");
            if (includeAddressData) {
                String vcard = VCardWriter.toVCard(contact, version);
                xml.append("        <C:address-data><![CDATA[").append(vcard).append("]]></C:address-data>\n");
            }
            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");
        }

        // Deleted contacts
        for (String deletedUid : delta.getDeletedUids()) {
            String href = basePath + (basePath.endsWith("/") ? "" : "/") + deletedUid + ".vcf";
            xml.append("  <D:response>\n");
            xml.append("    <D:href>").append(href).append("</D:href>\n");
            xml.append("    <D:status>HTTP/1.1 404 Not Found</D:status>\n");
            xml.append("  </D:response>\n");
        }

        // New sync token
        xml.append("  <D:sync-token>").append(delta.getNewSyncToken()).append("</D:sync-token>\n");
        xml.append("</D:multistatus>");
        return xml.toString();
    }

    /**
     * Extracts sync-token from a sync-collection REPORT XML request.
     */
    public static String extractSyncToken(String reportXml) {
        if (reportXml == null) return null;
        int start = reportXml.indexOf("<sync-token>");
        if (start < 0) start = reportXml.indexOf("<D:sync-token>");
        if (start < 0) return "";

        int contentStart = reportXml.indexOf('>', start) + 1;
        int end = reportXml.indexOf("</", contentStart);
        if (end > contentStart) {
            return reportXml.substring(contentStart, end).trim();
        }
        return "";
    }

    /**
     * Extracts requested hrefs from an addressbook-multiget REPORT XML request.
     */
    public static List<String> extractHrefs(String reportXml) {
        List<String> hrefs = new ArrayList<>();
        if (reportXml == null) return hrefs;

        int pos = 0;
        while (true) {
            int hrefIdx = reportXml.indexOf("<href>", pos);
            if (hrefIdx < 0) hrefIdx = reportXml.indexOf("<D:href>", pos);
            if (hrefIdx < 0) break;

            int openTagEnd = reportXml.indexOf('>', hrefIdx);
            int closeTag = reportXml.indexOf("</", openTagEnd);
            if (openTagEnd > 0 && closeTag > openTagEnd) {
                String href = reportXml.substring(openTagEnd + 1, closeTag).trim();
                hrefs.add(href);
                pos = closeTag + 2;
            } else {
                break;
            }
        }
        return hrefs;
    }
}

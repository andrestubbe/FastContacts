package fastcontacts.carddav;

import fastcontacts.Contact;
import fastcontacts.FastContactsRegistry;
import fastcontacts.VCardParser;
import fastcontacts.VCardVersion;
import fastcontacts.VCardWriter;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * High-speed HTTP CardDAV client supporting RFC 6352, sync-collection delta synchronization,
 * and batch address book synchronization.
 */
public final class CardDavClient {

    private final HttpClient httpClient;
    private final String serverUrl;

    public CardDavClient(String serverUrl) {
        this(serverUrl, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public CardDavClient(String serverUrl, HttpClient httpClient) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient cannot be null");
    }

    /**
     * Uploads or updates a contact on the CardDAV server.
     */
    public HttpResponse<String> putContact(Contact contact) throws IOException, InterruptedException {
        String vcard = VCardWriter.toVCard(contact, VCardVersion.V3_0);
        String url = serverUrl + contact.getUid() + ".vcf";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/vcard; charset=utf-8")
                .method("PUT", HttpRequest.BodyPublishers.ofString(vcard, StandardCharsets.UTF_8));

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Fetches a single contact by UID from the server.
     */
    public Optional<Contact> getContact(String uid) throws IOException, InterruptedException {
        String url = serverUrl + uid + ".vcf";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return Optional.of(VCardParser.parse(response.body()));
        }
        return Optional.empty();
    }

    /**
     * Deletes a contact from the server.
     */
    public boolean deleteContact(String uid) throws IOException, InterruptedException {
        String url = serverUrl + uid + ".vcf";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() == 200 || response.statusCode() == 204;
    }

    /**
     * Performs a sync-collection REPORT to retrieve updated and deleted contacts since a sync token.
     */
    public FastContactsRegistry.SyncDelta syncCollection(String syncToken) throws IOException, InterruptedException {
        String reportXml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<D:sync-collection xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">\n" +
                "  <D:sync-token>" + (syncToken != null ? syncToken : "") + "</D:sync-token>\n" +
                "  <D:sync-level>1</D:sync-level>\n" +
                "  <D:prop>\n" +
                "    <D:getetag/>\n" +
                "    <C:address-data/>\n" +
                "  </D:prop>\n" +
                "</D:sync-collection>";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Depth", "0")
                .method("REPORT", HttpRequest.BodyPublishers.ofString(reportXml, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseSyncCollectionResponse(response.body());
    }

    private FastContactsRegistry.SyncDelta parseSyncCollectionResponse(String xml) {
        List<Contact> updated = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        String newSyncToken = "";

        if (xml == null || xml.isEmpty()) {
            return new FastContactsRegistry.SyncDelta(updated, deleted, newSyncToken);
        }

        int stIdx = xml.indexOf("<D:sync-token>");
        if (stIdx < 0) stIdx = xml.indexOf("<sync-token>");
        if (stIdx >= 0) {
            int valStart = xml.indexOf('>', stIdx) + 1;
            int valEnd = xml.indexOf("</", valStart);
            if (valStart > 0 && valEnd > valStart) {
                newSyncToken = xml.substring(valStart, valEnd).trim();
            }
        }

        int pos = 0;
        while (true) {
            int respIdx = xml.indexOf("<D:response>", pos);
            if (respIdx < 0) respIdx = xml.indexOf("<response>", pos);
            if (respIdx < 0) break;

            int respEnd = xml.indexOf("</D:response>", respIdx);
            if (respEnd < 0) respEnd = xml.indexOf("</response>", respIdx);
            if (respEnd < 0) break;

            String respBlock = xml.substring(respIdx, respEnd);
            if (respBlock.contains("404 Not Found")) {
                int hrefStart = respBlock.indexOf("<D:href>");
                if (hrefStart < 0) hrefStart = respBlock.indexOf("<href>");
                if (hrefStart >= 0) {
                    int h1 = respBlock.indexOf('>', hrefStart) + 1;
                    int h2 = respBlock.indexOf("</", h1);
                    if (h1 > 0 && h2 > h1) {
                        String href = respBlock.substring(h1, h2).trim();
                        String uid = href.substring(href.lastIndexOf('/') + 1).replace(".vcf", "");
                        deleted.add(uid);
                    }
                }
            } else {
                int cdataStart = respBlock.indexOf("<![CDATA[");
                if (cdataStart >= 0) {
                    int cdataEnd = respBlock.indexOf("]]>", cdataStart);
                    if (cdataEnd > cdataStart) {
                        String vcard = respBlock.substring(cdataStart + 9, cdataEnd).trim();
                        try {
                            Contact c = VCardParser.parse(vcard);
                            updated.add(c);
                        } catch (Exception ignored) {}
                    }
                }
            }

            pos = respEnd + 10;
        }

        return new FastContactsRegistry.SyncDelta(updated, deleted, newSyncToken);
    }
}

package fastcontacts.carddav;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fastcontacts.Contact;
import fastcontacts.FastContactsRegistry;
import fastcontacts.VCardParser;
import fastcontacts.VCardVersion;
import fastcontacts.VCardWriter;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded, ultra-high-throughput CardDAV Server (RFC 6352, RFC 4791, RFC 4918).
 * Enables native integration with macOS Contacts, iOS, Thunderbird, Nextcloud, and FastContacts clients.
 */
public final class CardDavServer implements AutoCloseable {

    private final HttpServer server;
    private final FastContactsRegistry registry;
    private final int port;
    private final String basePath;

    public CardDavServer(int port, FastContactsRegistry registry) throws IOException {
        this(port, "/addressbooks/default", registry);
    }

    public CardDavServer(int port, String basePath, FastContactsRegistry registry) throws IOException {
        this.port = port;
        this.basePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        setupHandlers();
    }

    private void setupHandlers() {
        // Well-known CardDAV discovery redirect
        server.createContext("/.well-known/carddav", exchange -> {
            exchange.getResponseHeaders().set("Location", basePath + "/");
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });

        // Principal handler
        server.createContext("/principals/", exchange -> {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if (method.equals("PROPFIND") || method.equals("OPTIONS")) {
                handleOptions(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });

        // Main Addressbook Handler
        server.createContext(basePath, new CardDavHandler());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    @Override
    public void close() {
        stop();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public String getBaseUrl() {
        return "http://localhost:" + getPort() + basePath + "/";
    }

    public FastContactsRegistry getRegistry() {
        return registry;
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("DAV", "1, 2, 3, addressbook, access-control, sync-collection");
        exchange.getResponseHeaders().set("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, PROPFIND, PROPPATCH, REPORT");
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private final class CardDavHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            String path = exchange.getRequestURI().getPath();

            exchange.getResponseHeaders().set("DAV", "1, 2, 3, addressbook, access-control, sync-collection");

            try {
                switch (method) {
                    case "OPTIONS":
                        handleOptions(exchange);
                        break;
                    case "PROPFIND":
                        handlePropfind(exchange);
                        break;
                    case "REPORT":
                        handleReport(exchange);
                        break;
                    case "GET":
                        handleGet(exchange, path);
                        break;
                    case "PUT":
                        handlePut(exchange, path);
                        break;
                    case "DELETE":
                        handleDelete(exchange, path);
                        break;
                    default:
                        exchange.sendResponseHeaders(405, -1);
                        exchange.close();
                        break;
                }
            } catch (Exception e) {
                byte[] err = ("Internal Server Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, err.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(err);
                }
            }
        }

        private void handlePropfind(HttpExchange exchange) throws IOException {
            String responseXml = CardDavXml.buildDiscoveryResponse(basePath, registry.getCurrentSyncToken());
            byte[] bytes = responseXml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=utf-8");
            exchange.sendResponseHeaders(207, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void handleReport(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String responseXml;

            if (body.contains("sync-collection")) {
                String token = CardDavXml.extractSyncToken(body);
                FastContactsRegistry.SyncDelta delta = registry.getDeltaSince(token);
                responseXml = CardDavXml.buildSyncCollectionResponse(basePath, delta, true, VCardVersion.V3_0);
            } else if (body.contains("addressbook-multiget")) {
                List<String> hrefs = CardDavXml.extractHrefs(body);
                List<Contact> matched = new ArrayList<>();
                for (String href : hrefs) {
                    String uid = extractUidFromHref(href);
                    registry.get(uid).ifPresent(matched::add);
                }
                responseXml = CardDavXml.buildAddressbookQueryResponse(basePath, matched, true, VCardVersion.V3_0);
            } else {
                // Default addressbook-query: return all active contacts
                responseXml = CardDavXml.buildAddressbookQueryResponse(basePath, registry.getAll(), true, VCardVersion.V3_0);
            }

            byte[] bytes = responseXml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=utf-8");
            exchange.sendResponseHeaders(207, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void handleGet(HttpExchange exchange, String path) throws IOException {
            String uid = extractUidFromPath(path);
            if (uid == null || uid.isEmpty()) {
                // Export entire address book
                String vcf = registry.exportVcf();
                byte[] bytes = vcf.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/vcard; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            Optional<Contact> contactOpt = registry.get(uid);
            if (contactOpt.isPresent()) {
                Contact c = contactOpt.get();
                String vcard = VCardWriter.toVCard(c, VCardVersion.V3_0);
                byte[] bytes = vcard.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/vcard; charset=utf-8");
                exchange.getResponseHeaders().set("ETag", c.getEtag());
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        }

        private void handlePut(HttpExchange exchange, String path) throws IOException {
            String uid = extractUidFromPath(path);
            String vcardText = readRequestBody(exchange);

            if (vcardText.trim().isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            Contact parsed = VCardParser.parse(vcardText);
            if (uid != null && !uid.isEmpty()) {
                parsed = parsed.toBuilder().uid(uid).build();
            }

            // Check If-Match header for optimistic concurrency
            String ifMatch = exchange.getRequestHeaders().getFirst("If-Match");
            if (ifMatch != null && !ifMatch.equals("*")) {
                Optional<Contact> existing = registry.get(parsed.getUid());
                if (existing.isPresent() && !existing.get().getEtag().equals(ifMatch)) {
                    exchange.sendResponseHeaders(412, -1); // Precondition Failed
                    exchange.close();
                    return;
                }
            }

            boolean isNew = !registry.contains(parsed.getUid());
            Contact saved = registry.put(parsed);

            exchange.getResponseHeaders().set("ETag", saved.getEtag());
            exchange.sendResponseHeaders(isNew ? 201 : 204, -1);
            exchange.close();
        }

        private void handleDelete(HttpExchange exchange, String path) throws IOException {
            String uid = extractUidFromPath(path);
            if (uid == null || uid.isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            Optional<Contact> removed = registry.remove(uid);
            if (removed.isPresent()) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
            exchange.close();
        }

        private String extractUidFromPath(String path) {
            if (path == null) return null;
            String clean = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            int lastSlash = clean.lastIndexOf('/');
            if (lastSlash >= 0) {
                String segment = clean.substring(lastSlash + 1);
                if (segment.endsWith(".vcf")) {
                    return segment.substring(0, segment.length() - 4);
                }
                if (!segment.equalsIgnoreCase("default") && !segment.equalsIgnoreCase("addressbooks")) {
                    return segment;
                }
            }
            return null;
        }

        private String extractUidFromHref(String href) {
            return extractUidFromPath(href);
        }

        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = is.read(buf)) != -1) {
                    bos.write(buf, 0, r);
                }
                return bos.toString(StandardCharsets.UTF_8);
            }
        }
    }
}

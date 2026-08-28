package fastcontacts;

import fastcontacts.carddav.CardDavClient;
import fastcontacts.carddav.CardDavServer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FastContactsTest {

    @Test
    public void testVCardParserAndWriterRoundtrip() {
        Contact original = Contact.builder()
                .uid("test-uid-12345")
                .formattedName("Linus Benedict Torvalds")
                .givenName("Linus")
                .additionalNames("Benedict")
                .familyName("Torvalds")
                .organization("Linux Foundation")
                .department("Kernel Development")
                .jobTitle("Chief Kernel Architect")
                .role("Lead Maintainer")
                .addEmail("torvalds@linux-foundation.org", "WORK", "PREF")
                .addEmail("linus@kernel.org", "HOME")
                .addPhone("+1 503 555 0199", "WORK", "CELL")
                .addAddress(ContactAddress.of("100 SW Main St", "Portland", "OR", "97204", "USA", "WORK"))
                .addCategory("Kernel")
                .addCategory("OpenSource")
                .note("Creator of Linux and Git.")
                .birthday("1969-12-28")
                .url("https://kernel.org")
                .addCustomProperty("X-GITHUB", "https://github.com/torvalds")
                .build();

        String vcard = VCardWriter.toVCard(original, VCardVersion.V3_0);
        assertNotNull(vcard);
        assertTrue(vcard.contains("BEGIN:VCARD"));
        assertTrue(vcard.contains("END:VCARD"));
        assertTrue(vcard.contains("FN:Linus Benedict Torvalds"));
        assertTrue(vcard.contains("N:Torvalds;Linus;Benedict;;"));

        Contact parsed = VCardParser.parse(vcard);
        assertEquals(original.getUid(), parsed.getUid());
        assertEquals(original.getFormattedName(), parsed.getFormattedName());
        assertEquals(original.getGivenName(), parsed.getGivenName());
        assertEquals(original.getFamilyName(), parsed.getFamilyName());
        assertEquals(original.getOrganization(), parsed.getOrganization());
        assertEquals(original.getDepartment(), parsed.getDepartment());
        assertEquals(original.getJobTitle(), parsed.getJobTitle());
        assertEquals(2, parsed.getEmails().size());
        assertEquals("torvalds@linux-foundation.org", parsed.getPrimaryEmail().orElse(""));
        assertEquals("+1 503 555 0199", parsed.getPrimaryPhone().orElse(""));
        assertEquals("USA", parsed.getPrimaryAddress().map(ContactAddress::getCountry).orElse(""));
        assertTrue(parsed.getCategories().contains("Kernel"));
        assertEquals("https://github.com/torvalds", parsed.getCustomProperties().get("X-GITHUB"));
    }

    @Test
    public void testFastContactsRegistryIndexingAndSearch() {
        FastContactsRegistry registry = new FastContactsRegistry();

        Contact c1 = Contact.builder()
                .uid("c-1")
                .formattedName("James Gosling")
                .givenName("James")
                .familyName("Gosling")
                .organization("Amazon")
                .addEmail("gosling@amazon.com", "WORK")
                .addPhone("+1 (206) 555-0142", "WORK")
                .addCategory("Java")
                .build();

        Contact c2 = Contact.builder()
                .uid("c-2")
                .formattedName("Ada Lovelace")
                .givenName("Ada")
                .familyName("Lovelace")
                .organization("Analytical Lab")
                .addEmail("ada@lovelace.org", "WORK")
                .addPhone("+44 20 7946 0912", "WORK")
                .addCategory("Pioneers")
                .build();

        registry.put(c1);
        registry.put(c2);

        assertEquals(2, registry.size());

        // Email lookup
        List<Contact> byEmail = registry.findByEmail("gosling@amazon.com");
        assertEquals(1, byEmail.size());
        assertEquals("c-1", byEmail.get(0).getUid());

        // Phone normalized lookup
        List<Contact> byPhone = registry.findByPhone("+12065550142");
        assertEquals(1, byPhone.size());
        assertEquals("c-1", byPhone.get(0).getUid());

        // Category lookup
        List<Contact> byCat = registry.findByCategory("Java");
        assertEquals(1, byCat.size());
        assertEquals("c-1", byCat.get(0).getUid());

        // Token Search
        List<Contact> search1 = registry.search(ContactSearchQuery.builder()
                .textQuery("Lovelace")
                .build());
        assertEquals(1, search1.size());
        assertEquals("c-2", search1.get(0).getUid());

        // Removal
        registry.remove("c-1");
        assertEquals(1, registry.size());
        assertTrue(registry.findByEmail("gosling@amazon.com").isEmpty());
    }

    @Test
    public void testCardDavServerAndClientSync() throws Exception {
        FastContactsRegistry registry = new FastContactsRegistry();

        Contact contact = Contact.builder()
                .uid("sync-test-001")
                .formattedName("Grace Hopper")
                .givenName("Grace")
                .familyName("Hopper")
                .organization("US Navy")
                .jobTitle("Rear Admiral")
                .addEmail("hopper@navy.mil", "WORK")
                .addCategory("Compilers")
                .build();

        registry.put(contact);

        int testPort = 19443;
        try (CardDavServer server = new CardDavServer(testPort, registry)) {
            server.start();

            CardDavClient client = new CardDavClient(server.getBaseUrl());

            // 1. Initial Sync
            FastContactsRegistry.SyncDelta delta1 = client.syncCollection(null);
            assertNotNull(delta1.getNewSyncToken());
            assertEquals(1, delta1.getUpdatedContacts().size());
            assertEquals("sync-test-001", delta1.getUpdatedContacts().get(0).getUid());

            // 2. Put new contact via client
            Contact contact2 = Contact.builder()
                    .uid("sync-test-002")
                    .formattedName("Alan Turing")
                    .organization("Bletchley Park")
                    .addEmail("turing@bletchley.gov.uk", "WORK")
                    .build();

            var putResponse = client.putContact(contact2);
            assertTrue(putResponse.statusCode() == 201 || putResponse.statusCode() == 204);
            assertEquals(2, registry.size());

            // 3. Delta Sync
            FastContactsRegistry.SyncDelta delta2 = client.syncCollection(delta1.getNewSyncToken());
            assertEquals(1, delta2.getUpdatedContacts().size());
            assertEquals("sync-test-002", delta2.getUpdatedContacts().get(0).getUid());
            assertEquals(0, delta2.getDeletedUids().size());

            // 4. Delete contact via client
            boolean deleted = client.deleteContact("sync-test-001");
            assertTrue(deleted);
            assertEquals(1, registry.size());

            // 5. Delta Sync after delete
            FastContactsRegistry.SyncDelta delta3 = client.syncCollection(delta2.getNewSyncToken());
            assertEquals(0, delta3.getUpdatedContacts().size());
            assertEquals(1, delta3.getDeletedUids().size());
            assertEquals("sync-test-001", delta3.getDeletedUids().get(0));
        }
    }
}

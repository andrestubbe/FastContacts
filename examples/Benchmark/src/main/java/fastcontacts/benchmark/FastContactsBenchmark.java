package fastcontacts.benchmark;

import fastcontacts.*;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class FastContactsBenchmark {

    private String sampleVCard30;
    private String sampleVCard40;
    private Contact sampleContact;
    private FastContactsRegistry registry;
    private ContactSearchQuery searchQuery;

    @Setup(Level.Trial)
    public void setup() {
        sampleContact = Contact.builder()
                .uid("bench-linus-001")
                .formattedName("Linus Benedict Torvalds")
                .givenName("Linus")
                .additionalNames("Benedict")
                .familyName("Torvalds")
                .organization("Linux Foundation")
                .jobTitle("Fellow")
                .addEmail("torvalds@linux-foundation.org", "WORK", "PREF")
                .addPhone("+1 503 555 0199", "WORK")
                .addAddress(ContactAddress.of("100 SW Main St", "Portland", "OR", "97204", "USA", "WORK"))
                .addCategory("Kernel")
                .addCategory("OpenSource")
                .note("Creator of Linux and Git.")
                .build();

        sampleVCard30 = VCardWriter.toVCard(sampleContact, VCardVersion.V3_0);
        sampleVCard40 = VCardWriter.toVCard(sampleContact, VCardVersion.V4_0);

        registry = new FastContactsRegistry();
        for (int i = 1; i <= 50_000; i++) {
            registry.put(Contact.builder()
                    .uid("uid-" + i)
                    .formattedName("Contact " + i)
                    .givenName("Contact")
                    .familyName("Num" + i)
                    .organization("Org " + (i % 50))
                    .jobTitle("Engineer")
                    .addEmail("user" + i + "@company" + (i % 50) + ".com", "WORK")
                    .addPhone("+1 (555) " + String.format("%03d-%04d", i / 1000, i % 1000), "WORK")
                    .addCategory(i % 2 == 0 ? "FastJava" : "Dev")
                    .build());
        }

        searchQuery = ContactSearchQuery.builder()
                .textQuery("Num25000")
                .category("FastJava")
                .build();
    }

    @Benchmark
    public Contact benchmarkVCard30Parse() {
        return VCardParser.parse(sampleVCard30);
    }

    @Benchmark
    public Contact benchmarkVCard40Parse() {
        return VCardParser.parse(sampleVCard40);
    }

    @Benchmark
    public String benchmarkVCard30Serialize() {
        return VCardWriter.toVCard(sampleContact, VCardVersion.V3_0);
    }

    @Benchmark
    public List<Contact> benchmarkEmailLookup() {
        return registry.findByEmail("user25000@company0.com");
    }

    @Benchmark
    public List<Contact> benchmarkPhoneLookup() {
        return registry.findByPhone("+1 555 025-0000");
    }

    @Benchmark
    public List<Contact> benchmarkSearchQuery() {
        return registry.search(searchQuery);
    }
}

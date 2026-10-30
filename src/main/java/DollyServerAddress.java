import java.net.IDN;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;

/** SRV only: does not resolve gameplay traffic or contact the CMPack backend. */
public final class DollyServerAddress {
    private static final long TTL = 60_000_000_000L;
    private static final Map<String, Entry> CACHE = new LinkedHashMap<String, Entry>(32, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Entry> entry) { return size() > 256; }
    };
    interface Lookup { List<String> records(String host, boolean alternate) throws Exception; }
    private static final class Entry {
        final String[] address;
        final long expires;
        Entry(String[] address, long expires) { this.address = address; this.expires = expires; }
    }
    private static final class Record {
        final int priority, weight, port;
        final String host;
        Record(int priority, int weight, int port, String host) {
            this.priority = priority; this.weight = weight; this.port = port; this.host = host;
        }
    }
    public static String[] resolve(String host) {
        return resolve(host, DollySettings.config.alternateDns, DollySettings.config.fallbackDns, System.nanoTime(), DollyServerAddress::query);
    }
    static String[] resolve(String host, boolean alternate, boolean fallback, long now, Lookup lookup) {
        String[] original = {host, "25565"};
        if (host == null || host.indexOf(':') >= 0 || host.matches("[0-9.]+")) return original;
        final String ascii;
        try {
            ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            if (ascii.isEmpty() || ascii.length() > 253) return original;
        } catch (IllegalArgumentException invalid) { return original; }
        String key = ascii + ":" + alternate + ":" + fallback;
        synchronized (CACHE) {
            Entry cached = CACHE.get(key);
            if (cached != null && cached.expires - now > 0) return cached.address.clone();
        }
        String[] result = null;
        try { result = select(lookup.records(ascii, alternate)); }
        catch (Exception unavailable) { /* A failed optional SRV lookup retains the direct server address. */ }
        if (result == null && alternate && fallback) {
            try { result = select(lookup.records(ascii, false)); }
            catch (Exception unavailable) { /* Preserve the direct address when both DNS providers fail. */ }
        }
        if (result == null) result = original;
        synchronized (CACHE) { CACHE.put(key, new Entry(result.clone(), now + TTL)); }
        return result.clone();
    }
    static String[] select(List<String> values) {
        List<Record> records = new ArrayList<Record>();
        int priority = Integer.MAX_VALUE;
        for (String value : values) {
            try {
                String[] parts = value.trim().split("\\s+");
                if (parts.length != 4) continue;
                int p = Integer.parseInt(parts[0]), weight = Integer.parseInt(parts[1]), port = Integer.parseInt(parts[2]);
                String host = parts[3];
                if (host.equals(".")) continue;
                if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
                host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES);
                if (p < 0 || p > 65535 || weight < 0 || weight > 65535 || port < 1 || port > 65535 || host.isEmpty() || host.length() > 253) continue;
                if (p > priority) continue;
                if (p < priority) { records.clear(); priority = p; }
                records.add(new Record(p, weight, port, host));
            } catch (IllegalArgumentException malformed) { }
        }
        if (records.isEmpty()) return null;
        long weight = 0;
        for (Record record : records) weight += record.weight;
        Record chosen = records.get(ThreadLocalRandom.current().nextInt(records.size()));
        if (weight > 0) {
            long ticket = ThreadLocalRandom.current().nextLong(weight);
            for (Record record : records) {
                ticket -= record.weight;
                if (ticket < 0) { chosen = record; break; }
            }
        }
        return new String[] {chosen.host, Integer.toString(chosen.port)};
    }
    private static List<String> query(String host, boolean alternate) throws Exception {
        Hashtable<String, String> environment = new Hashtable<String, String>();
        environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        environment.put("java.naming.provider.url", alternate ? "dns://1.1.1.1/" : "dns:");
        environment.put("com.sun.jndi.dns.timeout.initial", "1000");
        environment.put("com.sun.jndi.dns.timeout.retries", "1");
        DirContext context = new InitialDirContext(environment);
        try {
            Attributes attributes = context.getAttributes("_minecraft._tcp." + host + (host.endsWith(".") ? "" : "."), new String[] {"SRV"});
            Attribute srv = attributes.get("SRV");
            if (srv == null) srv = attributes.get("srv");
            List<String> records = new ArrayList<String>();
            if (srv != null) {
                NamingEnumeration<?> values = srv.getAll();
                try { while (values.hasMore()) records.add(values.next().toString()); }
                finally { values.close(); }
            }
            return records;
        } finally { context.close(); }
    }
}

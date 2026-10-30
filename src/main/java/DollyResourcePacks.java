import dolly.security.ResourcePackPolicy;

public final class DollyResourcePacks {
    public static boolean reject(bcy client, hd packet) {
        ave mc = ave.A();
        if (ResourcePackPolicy.validate(packet.a(), mc.v.toPath(), mc.E()) != null) return false;
        client.a(new iu(packet.b(), iu.a.c));
        org.apache.logging.log4j.LogManager.getLogger().warn("[Dolly] Rejected an invalid or remote-local resource-pack request.");
        return true;
    }
    public static String url(hd packet) {
        String value = packet.a();
        if (!value.startsWith("level://")) return value;
        try { return "level://" + java.net.URLDecoder.decode(value.substring(8), "UTF-8"); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
}

import dolly.render.Occlusion;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import java.nio.*;
import java.util.*;

/** Protocol ownership/equivalence, exact occluder coverage, model sharing and draw order. No GPU. */
public final class DollyPerformanceTest {
    private static final class MutableQuad extends bgg {
        MutableQuad(int[] data) { super(data, -1, cq.b); }
    }
    public static void main(String[] args) {
        varInts(); frames(); terrain(); occlusion(); occlusionCache();
        System.out.println("Performance regressions passed: VarInts, fragmented frames/reference counts, terrain order and conservative occlusion.");
    }

    private static void referenceWrite(ByteBuf out, int value) {
        do {
            int b = value & 127; value >>>= 7;
            out.writeByte(value == 0 ? b : b | 128);
        } while (value != 0);
    }

    private static void varInts() {
        Random random = new Random(712);
        int[] edges = {0, 1, 127, 128, 16383, 16384, 2097151, 2097152, 268435455, 268435456, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int i = 0; i < 10000 + edges.length; i++) {
            int value = i < edges.length ? edges[i] : random.nextInt();
            ByteBuf expected = Unpooled.buffer(), actual = Unpooled.buffer();
            try {
                referenceWrite(expected, value);
                em packet = new em(actual); packet.b(value);
                assert em.a(value) == expected.readableBytes();
                assert expected.equals(actual) : "VarInt byte mismatch: " + value;
                assert packet.e() == value;
            } finally { actual.release(); expected.release(); }
        }
        for (int value : edges) {
            ByteBuf expected = Unpooled.buffer(), actual = Unpooled.buffer().order(ByteOrder.LITTLE_ENDIAN);
            try {
                referenceWrite(expected, value); new em(actual).b(value);
                assert expected.equals(actual) : "Byte order affected VarInt";
            } finally { actual.release(); expected.release(); }
        }
    }

    private static void frames() {
        for (int size : new int[] {0, 1, 127, 128, 256, 16384}) {
            byte[] payload = new byte[size]; new Random(size).nextBytes(payload);
            ByteBuf encoded = Unpooled.buffer(); referenceWrite(encoded, size); encoded.writeBytes(payload);
            byte[] wire = new byte[encoded.readableBytes()]; encoded.readBytes(wire); encoded.release();
            // Every header split, plus representative body splits, including the last byte.
            Set<Integer> splits = new HashSet<Integer>(Arrays.asList(0, 1, 2, 3, wire.length / 2, wire.length - 1, wire.length));
            for (int split : splits) {
                if (split > wire.length) continue;
                EmbeddedChannel channel = new EmbeddedChannel(new eq());
                ByteBuf result = null;
                try {
                    if (split > 0) channel.writeInbound(Unpooled.wrappedBuffer(Arrays.copyOfRange(wire, 0, split)));
                    if (split < wire.length) {
                        assert channel.readInbound() == null;
                        channel.writeInbound(Unpooled.wrappedBuffer(Arrays.copyOfRange(wire, split, wire.length)));
                    }
                    result = (ByteBuf) channel.readInbound();
                    assert result != null && result.readableBytes() == size;
                    assert channel.readInbound() == null;
                    channel.finish();
                    assert result.refCnt() > 0 : "Frame released with cumulation";
                    byte[] data = new byte[size]; result.readBytes(data); assert Arrays.equals(payload, data);
                } finally {
                    if (result != null) result.release();
                    channel.finish();
                    ByteBuf remaining;
                    while ((remaining = (ByteBuf) channel.readInbound()) != null) remaining.release();
                }
            }
        }
        EmbeddedChannel channel = new EmbeddedChannel(new eq());
        channel.writeInbound(Unpooled.wrappedBuffer(new byte[] {1, 42, 0, 2, 43, 44}));
        ByteBuf a = (ByteBuf) channel.readInbound(), b = (ByteBuf) channel.readInbound(), c = (ByteBuf) channel.readInbound();
        assert a.readByte() == 42 && b.readableBytes() == 0 && c.readShort() == 0x2b2c;
        a.release(); b.release(); c.release(); channel.finish();
        channel = new EmbeddedChannel(new eq());
        try { channel.writeInbound(Unpooled.wrappedBuffer(new byte[] {(byte) 128, (byte) 128, (byte) 128})); throw new AssertionError("Oversized header accepted"); }
        catch (DecoderException expected) { }
        finally { try { channel.finish(); } catch (DecoderException expected) { } }
        ByteBuf in = Unpooled.buffer(); in.writeByte(99); in.writeByte(128); in.readerIndex(1);
        List<Object> output = new ArrayList<Object>(); DollyNetwork.decode(in, output);
        assert in.readerIndex() == 1 && output.isEmpty(); in.release();
    }

    private static List<Integer> expand(IntBuffer first, IntBuffer counts, int n) {
        List<Integer> vertices = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) for (int j = 0; j < counts.get(counts.position() + i); j++) vertices.add(first.get(first.position() + i) + j);
        return vertices;
    }

    private static void terrain() {
        Random random = new Random(87); int joined = 0;
        for (int run = 0; run < 1000; run++) {
            int n = random.nextInt(30), offset = 2, start = 0;
            IntBuffer first = ByteBuffer.allocateDirect((n + offset) * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
            IntBuffer counts = IntBuffer.allocate(n + offset); first.position(offset); counts.position(offset);
            for (int i = 0; i < n; i++) {
                int count = random.nextInt(7) * 4;
                first.put(offset + i, start); counts.put(offset + i, count);
                start += count + (random.nextBoolean() ? 0 : 4);
            }
            List<Integer> before = expand(first, counts, n);
            int merged = DollyTerrain.merge(first, counts, 7); joined += n - merged;
            assert before.equals(expand(first, counts, merged)) : "Terrain contents/order changed";
            assert first.position() == offset && counts.position() == offset;
        }
        assert joined > 0;
        IntBuffer first = IntBuffer.wrap(new int[] {0, 4}), counts = IntBuffer.wrap(new int[] {4, 4});
        assert DollyTerrain.merge(first, counts, 1) == 2;
        first = IntBuffer.wrap(new int[] {0, 3}); counts = IntBuffer.wrap(new int[] {3, 4});
        assert DollyTerrain.merge(first, counts, 7) == 2;
        first = IntBuffer.wrap(new int[] {8, 0}); counts = IntBuffer.wrap(new int[] {4, 8});
        assert DollyTerrain.merge(first, counts, 7) == 2 : "Draws reordered";
    }

    private static final class Wall implements Occlusion.Blocks {
        int axis, plane, hole = Integer.MIN_VALUE, reads, budget = 100000;
        public boolean exhausted() { return reads >= budget; }
        public boolean opaque(int x, int y, int z) {
            reads++;
            int u = axis == 0 ? x : axis == 1 ? y : z;
            int v = axis == 0 ? y : x;
            return u == plane && v != hole;
        }
    }

    private static void occlusionCache() {
        Wall wall = new Wall(); wall.plane = 4;
        Occlusion.Cache cache = new Occlusion.Cache(wall); cache.reset();
        assert cache.opaque(4, 0, 0) && cache.opaque(4, 0, 0) && wall.reads == 1;
        assert !cache.opaque(3, 0, 0) && !cache.opaque(3, 0, 0) && wall.reads == 2;
        // x + 1024 collides in the fixed table, but must never inherit the old answer.
        assert !cache.opaque(1028, 0, 0) && cache.opaque(4, 0, 0) && wall.reads == 4;
        wall.plane = 3; cache.reset();
        assert !cache.opaque(4, 0, 0) && cache.opaque(3, 0, 0) && wall.reads == 6;
        wall.budget = 6; assert cache.exhausted();
    }

    private static void occlusion() {
        Wall wall = new Wall(); wall.plane = 4;
        assert Occlusion.hidden(0, 0, 0, 10, -1, -1, 12, 1, 1, wall);
        wall.hole = 0;
        assert !Occlusion.hidden(0, 0, 0, 10, -1, -1, 12, 1, 1, wall) : "Opening concealed";
        wall.hole = Integer.MIN_VALUE; wall.plane = 15;
        assert !Occlusion.hidden(0, 0, 0, 10, -1, -1, 12, 1, 1, wall) : "Wall behind object";
        wall.plane = 4;
        assert !Occlusion.hidden(11, 0, 0, 10, -1, -1, 12, 1, 1, wall) : "Camera inside box";
        wall.plane = -5;
        assert Occlusion.hidden(0, 0, 0, -12, -1, -1, -10, 1, 1, wall);
        wall.axis = 1; wall.plane = 4;
        assert Occlusion.hidden(0, 0, 0, -1, 10, -1, 1, 12, 1, wall);
        wall.axis = 2;
        assert Occlusion.hidden(0, 0, 0, -1, -1, 10, 1, 1, 12, wall);
        wall.reads = 0; wall.budget = 1;
        assert !Occlusion.hidden(0, 0, 0, -1, -1, 10, 1, 1, 12, wall) : "Incomplete query hid object";
        assert !Occlusion.hidden(Double.NaN, 0, 0, 10, -1, -1, 12, 1, 1, wall);
    }

    static void models() {
        int[] vertices = new int[28]; vertices[3] = 7;
        bgg a = new bgg(vertices, -1, cq.b), b = new bgg(vertices.clone(), -1, cq.b);
        int[] different = vertices.clone(); different[3] = 6; different[4] = 31;
        assert Arrays.hashCode(vertices) == Arrays.hashCode(different) : "Missing collision fixture";
        bgg c = new bgg(different, -1, cq.b);
        MutableQuad mutable = new MutableQuad(vertices.clone());
        List<List<bgg>> faces = new ArrayList<List<bgg>>();
        for (cq face : cq.values()) faces.add(Collections.<bgg>emptyList());
        final List<boq> models = Arrays.<boq>asList(new bow(Arrays.asList(a, b, c, mutable), faces, false, false, null, null));
        db<bov, boq> registry = new db<bov, boq>() {
            public boq a(bov key) { return models.get(0); }
            public void a(bov key, boq value) { throw new UnsupportedOperationException(); }
            public Iterator<boq> iterator() { return models.iterator(); }
        };
        DollyModels.deduplicate(registry);
        assert a.a == b.a && a.a != c.a;
        assert mutable.a != a.a : "Mutable subclass was pooled";
        assert DollyModels.savedBytes == 16 + a.a.length * 4;
        int[] before = a.a.clone();
        bgn breaking = new bgn(a, new bmi("dolly-test"));
        assert breaking.a != a.a && Arrays.equals(a.a, before) : "Breaking overlay mutated shared model";
        DollyModels.deduplicate(registry);
        assert DollyModels.savedBytes == 0 : "Reload pool retained old references";
    }
}

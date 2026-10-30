import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;

/** Allocation-free frame header and VarInt paths for the existing 1.8.9 protocol/Netty 4.0. */
public final class DollyNetwork {
    public static int varIntSize(int value) {
        return Math.max(1, (32 - Integer.numberOfLeadingZeros(value) + 6) / 7);
    }

    public static void writeVarInt(ByteBuf out, int value) {
        if ((value & ~127) == 0) { out.writeByte(value); return; }
        if ((value & ~16383) == 0 && out.order() == java.nio.ByteOrder.BIG_ENDIAN) {
            out.writeShort(((value & 127) | 128) << 8 | value >>> 7);
            return;
        }
        while ((value & ~127) != 0) {
            out.writeByte((value & 127) | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public static void decode(ByteBuf in, List<Object> out) {
        int start = in.readerIndex(), length = 0;
        for (int i = 0; i < 3; i++) {
            if (!in.isReadable()) { in.readerIndex(start); return; }
            int b = in.readUnsignedByte();
            length |= (b & 127) << (7 * i);
            if ((b & 128) == 0) {
                if (in.readableBytes() < length) { in.readerIndex(start); return; }
                // Netty's downstream decoder owns this reference, including for a fragmented TCP stream.
                ByteBuf frame = in.readSlice(length).retain();
                try { out.add(frame); }
                catch (RuntimeException | Error e) { frame.release(); throw e; }
                return;
            }
        }
        throw new CorruptedFrameException("length wider than 21-bit");
    }
}

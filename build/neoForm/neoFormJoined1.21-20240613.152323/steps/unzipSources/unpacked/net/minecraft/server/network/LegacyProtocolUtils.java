package net.minecraft.server.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class LegacyProtocolUtils {
    public static final int CUSTOM_PAYLOAD_PACKET_ID = 250;
    public static final String CUSTOM_PAYLOAD_PACKET_PING_CHANNEL = "MC|PingHost";
    public static final int GET_INFO_PACKET_ID = 254;
    public static final int GET_INFO_PACKET_VERSION_1 = 1;
    public static final int DISCONNECT_PACKET_ID = 255;
    public static final int FAKE_PROTOCOL_VERSION = 127;

    public static void writeLegacyString(ByteBuf pBuffer, String pString) {
        pBuffer.writeShort(pString.length());
        pBuffer.writeCharSequence(pString, StandardCharsets.UTF_16BE);
    }

    public static String readLegacyString(ByteBuf pBuffer) {
        int i = pBuffer.readShort();
        int j = i * 2;
        String s = pBuffer.toString(pBuffer.readerIndex(), j, StandardCharsets.UTF_16BE);
        pBuffer.skipBytes(j);
        return s;
    }
}

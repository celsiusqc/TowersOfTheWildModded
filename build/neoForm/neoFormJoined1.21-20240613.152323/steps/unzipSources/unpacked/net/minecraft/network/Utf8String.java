package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.nio.charset.StandardCharsets;

public class Utf8String {
    public static String read(ByteBuf pBuffer, int pMaxLength) {
        int i = ByteBufUtil.utf8MaxBytes(pMaxLength);
        int j = VarInt.read(pBuffer);
        if (j > i) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i + ")");
        } else if (j < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            int k = pBuffer.readableBytes();
            if (j > k) {
                throw new DecoderException("Not enough bytes in buffer, expected " + j + ", but got " + k);
            } else {
                String s = pBuffer.toString(pBuffer.readerIndex(), j, StandardCharsets.UTF_8);
                pBuffer.readerIndex(pBuffer.readerIndex() + j);
                if (s.length() > pMaxLength) {
                    throw new DecoderException("The received string length is longer than maximum allowed (" + s.length() + " > " + pMaxLength + ")");
                } else {
                    return s;
                }
            }
        }
    }

    public static void write(ByteBuf pBuffer, CharSequence pString, int pMaxLength) {
        if (pString.length() > pMaxLength) {
            throw new EncoderException("String too big (was " + pString.length() + " characters, max " + pMaxLength + ")");
        } else {
            int i = ByteBufUtil.utf8MaxBytes(pString);
            ByteBuf bytebuf = pBuffer.alloc().buffer(i);

            try {
                int j = ByteBufUtil.writeUtf8(bytebuf, pString);
                int k = ByteBufUtil.utf8MaxBytes(pMaxLength);
                if (j > k) {
                    throw new EncoderException("String too big (was " + j + " bytes encoded, max " + k + ")");
                }

                VarInt.write(pBuffer, j);
                pBuffer.writeBytes(bytebuf);
            } finally {
                bytebuf.release();
            }
        }
    }
}

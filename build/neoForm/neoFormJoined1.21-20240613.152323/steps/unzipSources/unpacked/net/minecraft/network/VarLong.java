package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarLong {
    private static final int MAX_VARLONG_SIZE = 10;
    private static final int DATA_BITS_MASK = 127;
    private static final int CONTINUATION_BIT_MASK = 128;
    private static final int DATA_BITS_PER_BYTE = 7;

    public static int getByteSize(long pData) {
        for (int i = 1; i < 10; i++) {
            if ((pData & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    public static boolean hasContinuationBit(byte pData) {
        return (pData & 128) == 128;
    }

    public static long read(ByteBuf pBuffer) {
        long i = 0L;
        int j = 0;

        byte b0;
        do {
            b0 = pBuffer.readByte();
            i |= (long)(b0 & 127) << j++ * 7;
            if (j > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while (hasContinuationBit(b0));

        return i;
    }

    public static ByteBuf write(ByteBuf pBuffer, long pValue) {
        while ((pValue & -128L) != 0L) {
            pBuffer.writeByte((int)(pValue & 127L) | 128);
            pValue >>>= 7;
        }

        pBuffer.writeByte((int)pValue);
        return pBuffer;
    }
}

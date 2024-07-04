package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Handles decompression of network traffic.
 *
 * @see Connection#setupCompression
 */
public class CompressionDecoder extends ByteToMessageDecoder {
    public static final int MAXIMUM_COMPRESSED_LENGTH = 2097152;
    public static final int MAXIMUM_UNCOMPRESSED_LENGTH = 8388608;
    private final Inflater inflater;
    private int threshold;
    private boolean validateDecompressed;

    public CompressionDecoder(int pThreshold, boolean pValidateDecompressed) {
        this.threshold = pThreshold;
        this.validateDecompressed = pValidateDecompressed;
        this.inflater = new Inflater();
    }

    @Override
    protected void decode(ChannelHandlerContext pContext, ByteBuf pIn, List<Object> pOut) throws Exception {
        if (pIn.readableBytes() != 0) {
            int i = VarInt.read(pIn);
            if (i == 0) {
                pOut.add(pIn.readBytes(pIn.readableBytes()));
            } else {
                if (this.validateDecompressed) {
                    if (i < this.threshold) {
                        throw new DecoderException("Badly compressed packet - size of " + i + " is below server threshold of " + this.threshold);
                    }

                    if (i > 8388608) {
                        throw new DecoderException("Badly compressed packet - size of " + i + " is larger than protocol maximum of 8388608");
                    }
                }

                this.setupInflaterInput(pIn);
                ByteBuf bytebuf = this.inflate(pContext, i);
                this.inflater.reset();
                pOut.add(bytebuf);
            }
        }
    }

    private void setupInflaterInput(ByteBuf pBuffer) {
        ByteBuffer bytebuffer;
        if (pBuffer.nioBufferCount() > 0) {
            bytebuffer = pBuffer.nioBuffer();
            pBuffer.skipBytes(pBuffer.readableBytes());
        } else {
            bytebuffer = ByteBuffer.allocateDirect(pBuffer.readableBytes());
            pBuffer.readBytes(bytebuffer);
            bytebuffer.flip();
        }

        this.inflater.setInput(bytebuffer);
    }

    private ByteBuf inflate(ChannelHandlerContext pContext, int pSize) throws DataFormatException {
        ByteBuf bytebuf = pContext.alloc().directBuffer(pSize);

        try {
            ByteBuffer bytebuffer = bytebuf.internalNioBuffer(0, pSize);
            int i = bytebuffer.position();
            this.inflater.inflate(bytebuffer);
            int j = bytebuffer.position() - i;
            if (j != pSize) {
                throw new DecoderException(
                    "Badly compressed packet - actual length of uncompressed payload " + j + " is does not match declared size " + pSize
                );
            } else {
                bytebuf.writerIndex(bytebuf.writerIndex() + j);
                return bytebuf;
            }
        } catch (Exception exception) {
            bytebuf.release();
            throw exception;
        }
    }

    public void setThreshold(int pThreshold, boolean pValidateDecompressed) {
        this.threshold = pThreshold;
        this.validateDecompressed = pValidateDecompressed;
    }
}

package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Prepends each frame ("packet") with its length encoded as a VarInt. Every frame's length must fit within a 3-byte VarInt.
 *
 * @see Varint21FrameDecoder
 */
@Sharable
public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {
    public static final int MAX_VARINT21_BYTES = 3;

    protected void encode(ChannelHandlerContext pContext, ByteBuf pEncoder, ByteBuf pDecoder) {
        int i = pEncoder.readableBytes();
        int j = VarInt.getByteSize(i);
        if (j > 3) {
            throw new EncoderException("Packet too large: size " + i + " is over 8");
        } else {
            pDecoder.ensureWritable(j + i);
            VarInt.write(pDecoder, i);
            pDecoder.writeBytes(pEncoder, pEncoder.readerIndex(), i);
        }
    }
}

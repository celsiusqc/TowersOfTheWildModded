package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;

public class PacketBundleUnpacker extends MessageToMessageEncoder<Packet<?>> {
    private final BundlerInfo bundlerInfo;

    public PacketBundleUnpacker(BundlerInfo pBundlerInfo) {
        this.bundlerInfo = pBundlerInfo;
    }

    protected void encode(ChannelHandlerContext pContext, Packet<?> pPacket, List<Object> p_265735_) throws Exception {
        this.bundlerInfo.unbundlePacket(pPacket, p_265735_::add, pContext);
        if (pPacket.isTerminal()) {
            pContext.pipeline().remove(pContext.name());
        }
    }
}

package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;

public interface ProtocolSwapHandler {
    static void handleInboundTerminalPacket(ChannelHandlerContext pContext, Packet<?> pPacket) {
        if (pPacket.isTerminal()) {
            pContext.channel().config().setAutoRead(false);
            pContext.pipeline().addBefore(pContext.name(), "inbound_config", new UnconfiguredPipelineHandler.Inbound());
            pContext.pipeline().remove(pContext.name());
        }
    }

    static void handleOutboundTerminalPacket(ChannelHandlerContext pContext, Packet<?> pPacket) {
        if (pPacket.isTerminal()) {
            pContext.pipeline().addAfter(pContext.name(), "outbound_config", new UnconfiguredPipelineHandler.Outbound());
            pContext.pipeline().remove(pContext.name());
        }
    }
}

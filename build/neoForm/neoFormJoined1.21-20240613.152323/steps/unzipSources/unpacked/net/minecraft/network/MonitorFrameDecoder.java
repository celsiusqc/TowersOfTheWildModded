package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MonitorFrameDecoder extends ChannelInboundHandlerAdapter {
    private final BandwidthDebugMonitor monitor;

    public MonitorFrameDecoder(BandwidthDebugMonitor pMonitor) {
        this.monitor = pMonitor;
    }

    @Override
    public void channelRead(ChannelHandlerContext pContext, Object pMessage) {
        if (pMessage instanceof ByteBuf bytebuf) {
            this.monitor.onReceive(bytebuf.readableBytes());
        }

        pContext.fireChannelRead(pMessage);
    }
}

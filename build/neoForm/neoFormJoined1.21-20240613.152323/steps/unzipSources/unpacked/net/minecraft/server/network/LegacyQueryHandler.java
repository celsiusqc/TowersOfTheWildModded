package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.server.ServerInfo;
import org.slf4j.Logger;

public class LegacyQueryHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServerInfo server;

    public LegacyQueryHandler(ServerInfo pServer) {
        this.server = pServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext pContext, Object pMessage) {
        ByteBuf bytebuf = (ByteBuf)pMessage;
        bytebuf.markReaderIndex();
        boolean flag = true;

        try {
            try {
                if (bytebuf.readUnsignedByte() != 254) {
                    return;
                }

                SocketAddress socketaddress = pContext.channel().remoteAddress();
                int i = bytebuf.readableBytes();
                if (i == 0) {
                    LOGGER.debug("Ping: (<1.3.x) from {}", socketaddress);
                    String s = createVersion0Response(this.server);
                    sendFlushAndClose(pContext, createLegacyDisconnectPacket(pContext.alloc(), s));
                } else {
                    if (bytebuf.readUnsignedByte() != 1) {
                        return;
                    }

                    if (bytebuf.isReadable()) {
                        if (!readCustomPayloadPacket(bytebuf)) {
                            return;
                        }

                        LOGGER.debug("Ping: (1.6) from {}", socketaddress);
                    } else {
                        LOGGER.debug("Ping: (1.4-1.5.x) from {}", socketaddress);
                    }

                    String s1 = createVersion1Response(this.server);
                    sendFlushAndClose(pContext, createLegacyDisconnectPacket(pContext.alloc(), s1));
                }

                bytebuf.release();
                flag = false;
            } catch (RuntimeException runtimeexception) {
            }
        } finally {
            if (flag) {
                bytebuf.resetReaderIndex();
                pContext.channel().pipeline().remove(this);
                pContext.fireChannelRead(pMessage);
            }
        }
    }

    private static boolean readCustomPayloadPacket(ByteBuf pBuffer) {
        short short1 = pBuffer.readUnsignedByte();
        if (short1 != 250) {
            return false;
        } else {
            String s = LegacyProtocolUtils.readLegacyString(pBuffer);
            if (!"MC|PingHost".equals(s)) {
                return false;
            } else {
                int i = pBuffer.readUnsignedShort();
                if (pBuffer.readableBytes() != i) {
                    return false;
                } else {
                    short short2 = pBuffer.readUnsignedByte();
                    if (short2 < 73) {
                        return false;
                    } else {
                        String s1 = LegacyProtocolUtils.readLegacyString(pBuffer);
                        int j = pBuffer.readInt();
                        return j <= 65535;
                    }
                }
            }
        }
    }

    private static String createVersion0Response(ServerInfo pServer) {
        return String.format(Locale.ROOT, "%s\u00a7%d\u00a7%d", pServer.getMotd(), pServer.getPlayerCount(), pServer.getMaxPlayers());
    }

    private static String createVersion1Response(ServerInfo pServer) {
        return String.format(
            Locale.ROOT,
            "\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
            127,
            pServer.getServerVersion(),
            pServer.getMotd(),
            pServer.getPlayerCount(),
            pServer.getMaxPlayers()
        );
    }

    private static void sendFlushAndClose(ChannelHandlerContext pContext, ByteBuf pBuffer) {
        pContext.pipeline().firstContext().writeAndFlush(pBuffer).addListener(ChannelFutureListener.CLOSE);
    }

    private static ByteBuf createLegacyDisconnectPacket(ByteBufAllocator pBufferAllocator, String pReason) {
        ByteBuf bytebuf = pBufferAllocator.buffer();
        bytebuf.writeByte(255);
        LegacyProtocolUtils.writeLegacyString(bytebuf, pReason);
        return bytebuf;
    }
}

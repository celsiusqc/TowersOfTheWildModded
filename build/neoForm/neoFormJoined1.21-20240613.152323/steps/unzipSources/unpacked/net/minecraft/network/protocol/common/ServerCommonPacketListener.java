package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;
import net.minecraft.network.protocol.game.ServerPacketListener;

public interface ServerCommonPacketListener extends ServerCookiePacketListener, ServerPacketListener, net.neoforged.neoforge.common.extensions.IServerCommonPacketListenerExtension {
    void handleKeepAlive(ServerboundKeepAlivePacket pPacket);

    void handlePong(ServerboundPongPacket pPacket);

    void handleCustomPayload(ServerboundCustomPayloadPacket pPacket);

    void handleResourcePackResponse(ServerboundResourcePackPacket pPacket);

    void handleClientInformation(ServerboundClientInformationPacket pPacket);
}

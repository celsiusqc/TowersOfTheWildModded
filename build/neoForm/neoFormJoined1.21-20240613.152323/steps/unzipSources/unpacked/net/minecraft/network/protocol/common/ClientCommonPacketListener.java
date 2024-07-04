package net.minecraft.network.protocol.common;

import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;

public interface ClientCommonPacketListener extends ClientCookiePacketListener, ClientboundPacketListener, net.neoforged.neoforge.common.extensions.IClientCommonPacketListenerExtension {
    void handleKeepAlive(ClientboundKeepAlivePacket pPacket);

    void handlePing(ClientboundPingPacket pPacket);

    void handleCustomPayload(ClientboundCustomPayloadPacket pPacket);

    void handleDisconnect(ClientboundDisconnectPacket pPacket);

    void handleResourcePackPush(ClientboundResourcePackPushPacket pPacket);

    void handleResourcePackPop(ClientboundResourcePackPopPacket pPacket);

    void handleUpdateTags(ClientboundUpdateTagsPacket pPacket);

    void handleStoreCookie(ClientboundStoreCookiePacket pPacket);

    void handleTransfer(ClientboundTransferPacket pPacket);

    void handleCustomReportDetails(ClientboundCustomReportDetailsPacket p_350481_);

    void handleServerLinks(ClientboundServerLinksPacket p_350776_);
}

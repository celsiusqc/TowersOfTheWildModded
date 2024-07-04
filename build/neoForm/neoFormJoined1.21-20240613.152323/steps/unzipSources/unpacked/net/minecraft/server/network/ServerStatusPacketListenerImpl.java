package net.minecraft.server.network;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerStatusPacketListener;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;

public class ServerStatusPacketListenerImpl implements ServerStatusPacketListener {
    private static final Component DISCONNECT_REASON = Component.translatable("multiplayer.status.request_handled");
    private final ServerStatus status;
    private final @org.jetbrains.annotations.Nullable String statusCache; // FORGE: cache status JSON
    private final Connection connection;
    private boolean hasRequestedStatus;

    public ServerStatusPacketListenerImpl(ServerStatus pStatus, Connection pConnection) {
        this(pStatus, pConnection, null);
    }
    public ServerStatusPacketListenerImpl(ServerStatus pStatus, Connection pConnection, @org.jetbrains.annotations.Nullable String statusCache) {
        this.status = pStatus;
        this.connection = pConnection;
        this.statusCache = statusCache;
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_350895_) {
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    @Override
    public void handleStatusRequest(ServerboundStatusRequestPacket pPacket) {
        if (this.hasRequestedStatus) {
            this.connection.disconnect(DISCONNECT_REASON);
        } else {
            this.hasRequestedStatus = true;
            this.connection.send(new ClientboundStatusResponsePacket(this.status, this.statusCache));
        }
    }

    @Override
    public void handlePingRequest(ServerboundPingRequestPacket pPacket) {
        this.connection.send(new ClientboundPongResponsePacket(pPacket.getTime()));
        this.connection.disconnect(DISCONNECT_REASON);
    }
}

package net.minecraft.server.network;

import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.server.MinecraftServer;

public class ServerHandshakePacketListenerImpl implements ServerHandshakePacketListener {
    private static final Component IGNORE_STATUS_REASON = Component.translatable("disconnect.ignoring_status_request");
    private final MinecraftServer server;
    private final Connection connection;

    public ServerHandshakePacketListenerImpl(MinecraftServer pServer, Connection pConnection) {
        this.server = pServer;
        this.connection = pConnection;
    }

    /**
     * There are two recognized intentions for initiating a handshake: logging in and acquiring server status. The NetworkManager's protocol will be reconfigured according to the specified intention, although a login-intention must pass a versioncheck or receive a disconnect otherwise
     */
    @Override
    public void handleIntention(ClientIntentionPacket pPacket) {
        switch (pPacket.intention()) {
            case LOGIN:
                this.beginLogin(pPacket, false);
                break;
            case STATUS:
                ServerStatus serverstatus = this.server.getStatus();
                this.connection.setupOutboundProtocol(StatusProtocols.CLIENTBOUND);
                if (this.server.repliesToStatus() && serverstatus != null) {
                    this.connection.setupInboundProtocol(StatusProtocols.SERVERBOUND, new ServerStatusPacketListenerImpl(serverstatus, this.connection));
                } else {
                    this.connection.disconnect(IGNORE_STATUS_REASON);
                }
                break;
            case TRANSFER:
                if (!this.server.acceptsTransfers()) {
                    this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
                    Component component = Component.translatable("multiplayer.disconnect.transfers_disabled");
                    this.connection.send(new ClientboundLoginDisconnectPacket(component));
                    this.connection.disconnect(component);
                } else {
                    this.beginLogin(pPacket, true);
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid intention " + pPacket.intention());
        }
    }

    private void beginLogin(ClientIntentionPacket pPacket, boolean pTransferred) {
        this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
        if (pPacket.protocolVersion() != SharedConstants.getCurrentVersion().getProtocolVersion()) {
            Component component;
            if (pPacket.protocolVersion() < 754) {
                component = Component.translatable("multiplayer.disconnect.outdated_client", SharedConstants.getCurrentVersion().getName());
            } else {
                component = Component.translatable("multiplayer.disconnect.incompatible", SharedConstants.getCurrentVersion().getName());
            }

            this.connection.send(new ClientboundLoginDisconnectPacket(component));
            this.connection.disconnect(component);
        } else {
            this.connection.setupInboundProtocol(LoginProtocols.SERVERBOUND, new ServerLoginPacketListenerImpl(this.server, this.connection, pTransferred));
        }
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_350912_) {
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }
}

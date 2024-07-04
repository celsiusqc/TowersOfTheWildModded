package net.minecraft.server.network;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.server.MinecraftServer;

public class MemoryServerHandshakePacketListenerImpl implements ServerHandshakePacketListener {
    private final MinecraftServer server;
    private final Connection connection;

    public MemoryServerHandshakePacketListenerImpl(MinecraftServer pServer, Connection pConnection) {
        this.server = pServer;
        this.connection = pConnection;
    }

    /**
     * There are two recognized intentions for initiating a handshake: logging in and acquiring server status. The NetworkManager's protocol will be reconfigured according to the specified intention, although a login-intention must pass a versioncheck or receive a disconnect otherwise
     */
    @Override
    public void handleIntention(ClientIntentionPacket pPacket) {
        if (pPacket.intention() != ClientIntent.LOGIN) {
            throw new UnsupportedOperationException("Invalid intention " + pPacket.intention());
        } else {
            this.connection.setupInboundProtocol(LoginProtocols.SERVERBOUND, new ServerLoginPacketListenerImpl(this.server, this.connection, false));
            this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
        }
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_350630_) {
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }
}

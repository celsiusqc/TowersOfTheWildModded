package net.minecraft.network.protocol;

/**
 * The direction of packets.
 */
public enum PacketFlow implements net.neoforged.neoforge.common.extensions.IPacketFlowExtension {
    SERVERBOUND("serverbound"),
    CLIENTBOUND("clientbound");

    private final String id;

    private PacketFlow(String pId) {
        this.id = pId;
    }

    public PacketFlow getOpposite() {
        return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
    }

    public String id() {
        return this.id;
    }
}

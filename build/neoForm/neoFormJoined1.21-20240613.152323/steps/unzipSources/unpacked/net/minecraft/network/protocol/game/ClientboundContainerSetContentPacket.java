package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.ItemStack;

public class ClientboundContainerSetContentPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundContainerSetContentPacket> STREAM_CODEC = Packet.codec(
        ClientboundContainerSetContentPacket::write, ClientboundContainerSetContentPacket::new
    );
    private final int containerId;
    private final int stateId;
    private final List<ItemStack> items;
    private final ItemStack carriedItem;

    public ClientboundContainerSetContentPacket(int pContainerId, int pStateId, NonNullList<ItemStack> pItems, ItemStack pCarriedItem) {
        this.containerId = pContainerId;
        this.stateId = pStateId;
        this.items = NonNullList.withSize(pItems.size(), ItemStack.EMPTY);

        for (int i = 0; i < pItems.size(); i++) {
            this.items.set(i, pItems.get(i).copy());
        }

        this.carriedItem = pCarriedItem.copy();
    }

    private ClientboundContainerSetContentPacket(RegistryFriendlyByteBuf p_319794_) {
        this.containerId = p_319794_.readUnsignedByte();
        this.stateId = p_319794_.readVarInt();
        this.items = ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(p_319794_);
        this.carriedItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(p_319794_);
    }

    private void write(RegistryFriendlyByteBuf p_320758_) {
        p_320758_.writeByte(this.containerId);
        p_320758_.writeVarInt(this.stateId);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(p_320758_, this.items);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(p_320758_, this.carriedItem);
    }

    @Override
    public PacketType<ClientboundContainerSetContentPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_CONTENT;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleContainerContent(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public ItemStack getCarriedItem() {
        return this.carriedItem;
    }

    public int getStateId() {
        return this.stateId;
    }
}

package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class ServerboundContainerClickPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundContainerClickPacket> STREAM_CODEC = Packet.codec(
        ServerboundContainerClickPacket::write, ServerboundContainerClickPacket::new
    );
    private static final int MAX_SLOT_COUNT = 128;
    private static final StreamCodec<RegistryFriendlyByteBuf, Int2ObjectMap<ItemStack>> SLOTS_STREAM_CODEC = ByteBufCodecs.map(
        Int2ObjectOpenHashMap::new, ByteBufCodecs.SHORT.map(Short::intValue, Integer::shortValue), ItemStack.OPTIONAL_STREAM_CODEC, 128
    );
    /**
     * The id of the window which was clicked. 0 for player inventory.
     */
    private final int containerId;
    private final int stateId;
    /**
     * Id of the clicked slot
     */
    private final int slotNum;
    /**
     * Button used
     */
    private final int buttonNum;
    /**
     * Inventory operation mode
     */
    private final ClickType clickType;
    private final ItemStack carriedItem;
    private final Int2ObjectMap<ItemStack> changedSlots;

    public ServerboundContainerClickPacket(
        int pContainerId, int pStateId, int pSlotNum, int pButtonNum, ClickType pClickType, ItemStack pCarriedItem, Int2ObjectMap<ItemStack> pChangedSlots
    ) {
        this.containerId = pContainerId;
        this.stateId = pStateId;
        this.slotNum = pSlotNum;
        this.buttonNum = pButtonNum;
        this.clickType = pClickType;
        this.carriedItem = pCarriedItem;
        this.changedSlots = Int2ObjectMaps.unmodifiable(pChangedSlots);
    }

    private ServerboundContainerClickPacket(RegistryFriendlyByteBuf p_319940_) {
        this.containerId = p_319940_.readByte();
        this.stateId = p_319940_.readVarInt();
        this.slotNum = p_319940_.readShort();
        this.buttonNum = p_319940_.readByte();
        this.clickType = p_319940_.readEnum(ClickType.class);
        this.changedSlots = Int2ObjectMaps.unmodifiable(SLOTS_STREAM_CODEC.decode(p_319940_));
        this.carriedItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(p_319940_);
    }

    private void write(RegistryFriendlyByteBuf p_319872_) {
        p_319872_.writeByte(this.containerId);
        p_319872_.writeVarInt(this.stateId);
        p_319872_.writeShort(this.slotNum);
        p_319872_.writeByte(this.buttonNum);
        p_319872_.writeEnum(this.clickType);
        SLOTS_STREAM_CODEC.encode(p_319872_, this.changedSlots);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(p_319872_, this.carriedItem);
    }

    @Override
    public PacketType<ServerboundContainerClickPacket> type() {
        return GamePacketTypes.SERVERBOUND_CONTAINER_CLICK;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleContainerClick(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSlotNum() {
        return this.slotNum;
    }

    public int getButtonNum() {
        return this.buttonNum;
    }

    public ItemStack getCarriedItem() {
        return this.carriedItem;
    }

    public Int2ObjectMap<ItemStack> getChangedSlots() {
        return this.changedSlots;
    }

    public ClickType getClickType() {
        return this.clickType;
    }

    public int getStateId() {
        return this.stateId;
    }
}

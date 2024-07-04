package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.Block;

/**
 * Triggers a block event on the client.
 *
 * @see Block#triggerEvent
 * @see Level#blockEvent
 */
public class ClientboundBlockEventPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBlockEventPacket> STREAM_CODEC = Packet.codec(
        ClientboundBlockEventPacket::write, ClientboundBlockEventPacket::new
    );
    private final BlockPos pos;
    private final int b0;
    private final int b1;
    private final Block block;

    /**
     * @param pB0 first parameter of the block event. The meaning of this value
     *            depends on the block.
     * @param pB1 second parameter of the block event. The meaning of this value
     *            depends on the block.
     */
    public ClientboundBlockEventPacket(BlockPos pPos, Block pBlock, int pB0, int pB1) {
        this.pos = pPos;
        this.block = pBlock;
        this.b0 = pB0;
        this.b1 = pB1;
    }

    private ClientboundBlockEventPacket(RegistryFriendlyByteBuf p_320705_) {
        this.pos = p_320705_.readBlockPos();
        this.b0 = p_320705_.readUnsignedByte();
        this.b1 = p_320705_.readUnsignedByte();
        this.block = ByteBufCodecs.registry(Registries.BLOCK).decode(p_320705_);
    }

    private void write(RegistryFriendlyByteBuf p_320189_) {
        p_320189_.writeBlockPos(this.pos);
        p_320189_.writeByte(this.b0);
        p_320189_.writeByte(this.b1);
        ByteBufCodecs.registry(Registries.BLOCK).encode(p_320189_, this.block);
    }

    @Override
    public PacketType<ClientboundBlockEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_BLOCK_EVENT;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleBlockEvent(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getB0() {
        return this.b0;
    }

    public int getB1() {
        return this.b1;
    }

    public Block getBlock() {
        return this.block;
    }
}

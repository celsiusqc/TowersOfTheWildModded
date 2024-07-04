package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;

public class ServerboundSetCommandMinecartPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSetCommandMinecartPacket> STREAM_CODEC = Packet.codec(
        ServerboundSetCommandMinecartPacket::write, ServerboundSetCommandMinecartPacket::new
    );
    private final int entity;
    private final String command;
    private final boolean trackOutput;

    public ServerboundSetCommandMinecartPacket(int pEntity, String pCommand, boolean pTrackOutput) {
        this.entity = pEntity;
        this.command = pCommand;
        this.trackOutput = pTrackOutput;
    }

    private ServerboundSetCommandMinecartPacket(FriendlyByteBuf p_179758_) {
        this.entity = p_179758_.readVarInt();
        this.command = p_179758_.readUtf();
        this.trackOutput = p_179758_.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134547_) {
        p_134547_.writeVarInt(this.entity);
        p_134547_.writeUtf(this.command);
        p_134547_.writeBoolean(this.trackOutput);
    }

    @Override
    public PacketType<ServerboundSetCommandMinecartPacket> type() {
        return GamePacketTypes.SERVERBOUND_SET_COMMAND_MINECART;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleSetCommandMinecart(this);
    }

    @Nullable
    public BaseCommandBlock getCommandBlock(Level pLevel) {
        Entity entity = pLevel.getEntity(this.entity);
        return entity instanceof MinecartCommandBlock ? ((MinecartCommandBlock)entity).getCommandBlock() : null;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }
}

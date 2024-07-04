package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

public class ServerboundSetJigsawBlockPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSetJigsawBlockPacket> STREAM_CODEC = Packet.codec(
        ServerboundSetJigsawBlockPacket::write, ServerboundSetJigsawBlockPacket::new
    );
    private final BlockPos pos;
    private final ResourceLocation name;
    private final ResourceLocation target;
    private final ResourceLocation pool;
    private final String finalState;
    private final JigsawBlockEntity.JointType joint;
    private final int selectionPriority;
    private final int placementPriority;

    public ServerboundSetJigsawBlockPacket(
        BlockPos pPos,
        ResourceLocation pName,
        ResourceLocation pTarget,
        ResourceLocation pPool,
        String pFinalState,
        JigsawBlockEntity.JointType pJoint,
        int pSelectionPriority,
        int pPlacementPriority
    ) {
        this.pos = pPos;
        this.name = pName;
        this.target = pTarget;
        this.pool = pPool;
        this.finalState = pFinalState;
        this.joint = pJoint;
        this.selectionPriority = pSelectionPriority;
        this.placementPriority = pPlacementPriority;
    }

    private ServerboundSetJigsawBlockPacket(FriendlyByteBuf p_179766_) {
        this.pos = p_179766_.readBlockPos();
        this.name = p_179766_.readResourceLocation();
        this.target = p_179766_.readResourceLocation();
        this.pool = p_179766_.readResourceLocation();
        this.finalState = p_179766_.readUtf();
        this.joint = JigsawBlockEntity.JointType.byName(p_179766_.readUtf()).orElse(JigsawBlockEntity.JointType.ALIGNED);
        this.selectionPriority = p_179766_.readVarInt();
        this.placementPriority = p_179766_.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134587_) {
        p_134587_.writeBlockPos(this.pos);
        p_134587_.writeResourceLocation(this.name);
        p_134587_.writeResourceLocation(this.target);
        p_134587_.writeResourceLocation(this.pool);
        p_134587_.writeUtf(this.finalState);
        p_134587_.writeUtf(this.joint.getSerializedName());
        p_134587_.writeVarInt(this.selectionPriority);
        p_134587_.writeVarInt(this.placementPriority);
    }

    @Override
    public PacketType<ServerboundSetJigsawBlockPacket> type() {
        return GamePacketTypes.SERVERBOUND_SET_JIGSAW_BLOCK;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleSetJigsawBlock(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public ResourceLocation getTarget() {
        return this.target;
    }

    public ResourceLocation getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public JigsawBlockEntity.JointType getJoint() {
        return this.joint;
    }

    public int getSelectionPriority() {
        return this.selectionPriority;
    }

    public int getPlacementPriority() {
        return this.placementPriority;
    }
}

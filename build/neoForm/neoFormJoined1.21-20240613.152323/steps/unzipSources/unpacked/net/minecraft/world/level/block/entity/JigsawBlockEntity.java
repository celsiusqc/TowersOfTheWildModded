package net.minecraft.world.level.block.entity;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class JigsawBlockEntity extends BlockEntity {
    public static final String TARGET = "target";
    public static final String POOL = "pool";
    public static final String JOINT = "joint";
    public static final String PLACEMENT_PRIORITY = "placement_priority";
    public static final String SELECTION_PRIORITY = "selection_priority";
    public static final String NAME = "name";
    public static final String FINAL_STATE = "final_state";
    private ResourceLocation name = ResourceLocation.withDefaultNamespace("empty");
    private ResourceLocation target = ResourceLocation.withDefaultNamespace("empty");
    private ResourceKey<StructureTemplatePool> pool = ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace("empty"));
    private JigsawBlockEntity.JointType joint = JigsawBlockEntity.JointType.ROLLABLE;
    private String finalState = "minecraft:air";
    private int placementPriority;
    private int selectionPriority;

    public JigsawBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.JIGSAW, pPos, pBlockState);
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public ResourceLocation getTarget() {
        return this.target;
    }

    public ResourceKey<StructureTemplatePool> getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public JigsawBlockEntity.JointType getJoint() {
        return this.joint;
    }

    public int getPlacementPriority() {
        return this.placementPriority;
    }

    public int getSelectionPriority() {
        return this.selectionPriority;
    }

    public void setName(ResourceLocation pName) {
        this.name = pName;
    }

    public void setTarget(ResourceLocation pTarget) {
        this.target = pTarget;
    }

    public void setPool(ResourceKey<StructureTemplatePool> pPool) {
        this.pool = pPool;
    }

    public void setFinalState(String pFinalState) {
        this.finalState = pFinalState;
    }

    public void setJoint(JigsawBlockEntity.JointType pJoint) {
        this.joint = pJoint;
    }

    public void setPlacementPriority(int pPlacementPriority) {
        this.placementPriority = pPlacementPriority;
    }

    public void setSelectionPriority(int pSelectionPriority) {
        this.selectionPriority = pSelectionPriority;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putString("name", this.name.toString());
        pTag.putString("target", this.target.toString());
        pTag.putString("pool", this.pool.location().toString());
        pTag.putString("final_state", this.finalState);
        pTag.putString("joint", this.joint.getSerializedName());
        pTag.putInt("placement_priority", this.placementPriority);
        pTag.putInt("selection_priority", this.selectionPriority);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.name = ResourceLocation.parse(pTag.getString("name"));
        this.target = ResourceLocation.parse(pTag.getString("target"));
        this.pool = ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.parse(pTag.getString("pool")));
        this.finalState = pTag.getString("final_state");
        this.joint = JigsawBlockEntity.JointType.byName(pTag.getString("joint"))
            .orElseGet(
                () -> JigsawBlock.getFrontFacing(this.getBlockState()).getAxis().isHorizontal()
                        ? JigsawBlockEntity.JointType.ALIGNED
                        : JigsawBlockEntity.JointType.ROLLABLE
            );
        this.placementPriority = pTag.getInt("placement_priority");
        this.selectionPriority = pTag.getInt("selection_priority");
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveCustomOnly(pRegistries);
    }

    public void generate(ServerLevel pLevel, int pMaxDepth, boolean pKeepJigsaws) {
        BlockPos blockpos = this.getBlockPos().relative(this.getBlockState().getValue(JigsawBlock.ORIENTATION).front());
        Registry<StructureTemplatePool> registry = pLevel.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> holder = registry.getHolderOrThrow(this.pool);
        JigsawPlacement.generateJigsaw(pLevel, holder, this.target, pMaxDepth, blockpos, pKeepJigsaws);
    }

    public static enum JointType implements StringRepresentable {
        ROLLABLE("rollable"),
        ALIGNED("aligned");

        private final String name;

        private JointType(String pName) {
            this.name = pName;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static Optional<JigsawBlockEntity.JointType> byName(String pName) {
            return Arrays.stream(values()).filter(p_59461_ -> p_59461_.getSerializedName().equals(pName)).findFirst();
        }

        public Component getTranslatedName() {
            return Component.translatable("jigsaw_block.joint." + this.name);
        }
    }
}

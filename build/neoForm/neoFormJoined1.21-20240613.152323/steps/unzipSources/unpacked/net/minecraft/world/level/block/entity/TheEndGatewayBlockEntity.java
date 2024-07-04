package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SPAWN_TIME = 200;
    private static final int COOLDOWN_TIME = 40;
    private static final int ATTENTION_INTERVAL = 2400;
    private static final int EVENT_COOLDOWN = 1;
    private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
    private long age;
    private int teleportCooldown;
    @Nullable
    private BlockPos exitPortal;
    private boolean exactTeleport;

    public TheEndGatewayBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.END_GATEWAY, pPos, pBlockState);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putLong("Age", this.age);
        if (this.exitPortal != null) {
            pTag.put("exit_portal", NbtUtils.writeBlockPos(this.exitPortal));
        }

        if (this.exactTeleport) {
            pTag.putBoolean("ExactTeleport", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.age = pTag.getLong("Age");
        NbtUtils.readBlockPos(pTag, "exit_portal").filter(Level::isInSpawnableBounds).ifPresent(p_325870_ -> this.exitPortal = p_325870_);
        this.exactTeleport = pTag.getBoolean("ExactTeleport");
    }

    public static void beamAnimationTick(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
        pBlockEntity.age++;
        if (pBlockEntity.isCoolingDown()) {
            pBlockEntity.teleportCooldown--;
        }
    }

    public static void portalTick(Level p_350841_, BlockPos p_350745_, BlockState p_350476_, TheEndGatewayBlockEntity p_350562_) {
        boolean flag = p_350562_.isSpawning();
        boolean flag1 = p_350562_.isCoolingDown();
        p_350562_.age++;
        if (flag1) {
            p_350562_.teleportCooldown--;
        } else if (p_350562_.age % 2400L == 0L) {
            triggerCooldown(p_350841_, p_350745_, p_350476_, p_350562_);
        }

        if (flag != p_350562_.isSpawning() || flag1 != p_350562_.isCoolingDown()) {
            setChanged(p_350841_, p_350745_, p_350476_);
        }
    }

    public boolean isSpawning() {
        return this.age < 200L;
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public float getSpawnPercent(float pPartialTicks) {
        return Mth.clamp(((float)this.age + pPartialTicks) / 200.0F, 0.0F, 1.0F);
    }

    public float getCooldownPercent(float pPartialTicks) {
        return 1.0F - Mth.clamp(((float)this.teleportCooldown - pPartialTicks) / 40.0F, 0.0F, 1.0F);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveCustomOnly(pRegistries);
    }

    public static void triggerCooldown(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
        if (!pLevel.isClientSide) {
            pBlockEntity.teleportCooldown = 40;
            pLevel.blockEvent(pPos, pState.getBlock(), 1, 0);
            setChanged(pLevel, pPos, pState);
        }
    }

    @Override
    public boolean triggerEvent(int pId, int pType) {
        if (pId == 1) {
            this.teleportCooldown = 40;
            return true;
        } else {
            return super.triggerEvent(pId, pType);
        }
    }

    @Nullable
    public Vec3 getPortalPosition(ServerLevel p_350561_, BlockPos p_350271_) {
        if (this.exitPortal == null && p_350561_.dimension() == Level.END) {
            BlockPos blockpos = findOrCreateValidTeleportPos(p_350561_, p_350271_);
            blockpos = blockpos.above(10);
            LOGGER.debug("Creating portal at {}", blockpos);
            spawnGatewayPortal(p_350561_, blockpos, EndGatewayConfiguration.knownExit(p_350271_, false));
            this.setExitPosition(blockpos, this.exactTeleport);
        }

        if (this.exitPortal != null) {
            BlockPos blockpos1 = this.exactTeleport ? this.exitPortal : findExitPosition(p_350561_, this.exitPortal);
            return blockpos1.getBottomCenter();
        } else {
            return null;
        }
    }

    private static BlockPos findExitPosition(Level pLevel, BlockPos pPos) {
        BlockPos blockpos = findTallestBlock(pLevel, pPos.offset(0, 2, 0), 5, false);
        LOGGER.debug("Best exit position for portal at {} is {}", pPos, blockpos);
        return blockpos.above();
    }

    private static BlockPos findOrCreateValidTeleportPos(ServerLevel pLevel, BlockPos pPos) {
        Vec3 vec3 = findExitPortalXZPosTentative(pLevel, pPos);
        LevelChunk levelchunk = getChunk(pLevel, vec3);
        BlockPos blockpos = findValidSpawnInChunk(levelchunk);
        if (blockpos == null) {
            BlockPos blockpos1 = BlockPos.containing(vec3.x + 0.5, 75.0, vec3.z + 0.5);
            LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", blockpos1);
            pLevel.registryAccess()
                .registry(Registries.CONFIGURED_FEATURE)
                .flatMap(p_258975_ -> p_258975_.getHolder(EndFeatures.END_ISLAND))
                .ifPresent(
                    p_256040_ -> p_256040_.value()
                            .place(pLevel, pLevel.getChunkSource().getGenerator(), RandomSource.create(blockpos1.asLong()), blockpos1)
                );
            blockpos = blockpos1;
        } else {
            LOGGER.debug("Found suitable block to teleport to: {}", blockpos);
        }

        return findTallestBlock(pLevel, blockpos, 16, true);
    }

    private static Vec3 findExitPortalXZPosTentative(ServerLevel pLevel, BlockPos pPos) {
        Vec3 vec3 = new Vec3((double)pPos.getX(), 0.0, (double)pPos.getZ()).normalize();
        int i = 1024;
        Vec3 vec31 = vec3.scale(1024.0);

        for (int j = 16; !isChunkEmpty(pLevel, vec31) && j-- > 0; vec31 = vec31.add(vec3.scale(-16.0))) {
            LOGGER.debug("Skipping backwards past nonempty chunk at {}", vec31);
        }

        for (int k = 16; isChunkEmpty(pLevel, vec31) && k-- > 0; vec31 = vec31.add(vec3.scale(16.0))) {
            LOGGER.debug("Skipping forward past empty chunk at {}", vec31);
        }

        LOGGER.debug("Found chunk at {}", vec31);
        return vec31;
    }

    private static boolean isChunkEmpty(ServerLevel pLevel, Vec3 pPos) {
        return getChunk(pLevel, pPos).getHighestFilledSectionIndex() == -1;
    }

    private static BlockPos findTallestBlock(BlockGetter pLevel, BlockPos pPos, int pRadius, boolean pAllowBedrock) {
        BlockPos blockpos = null;

        for (int i = -pRadius; i <= pRadius; i++) {
            for (int j = -pRadius; j <= pRadius; j++) {
                if (i != 0 || j != 0 || pAllowBedrock) {
                    for (int k = pLevel.getMaxBuildHeight() - 1; k > (blockpos == null ? pLevel.getMinBuildHeight() : blockpos.getY()); k--) {
                        BlockPos blockpos1 = new BlockPos(pPos.getX() + i, k, pPos.getZ() + j);
                        BlockState blockstate = pLevel.getBlockState(blockpos1);
                        if (blockstate.isCollisionShapeFullBlock(pLevel, blockpos1) && (pAllowBedrock || !blockstate.is(Blocks.BEDROCK))) {
                            blockpos = blockpos1;
                            break;
                        }
                    }
                }
            }
        }

        return blockpos == null ? pPos : blockpos;
    }

    private static LevelChunk getChunk(Level pLevel, Vec3 pPos) {
        return pLevel.getChunk(Mth.floor(pPos.x / 16.0), Mth.floor(pPos.z / 16.0));
    }

    @Nullable
    private static BlockPos findValidSpawnInChunk(LevelChunk pChunk) {
        ChunkPos chunkpos = pChunk.getPos();
        BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), 30, chunkpos.getMinBlockZ());
        int i = pChunk.getHighestSectionPosition() + 16 - 1;
        BlockPos blockpos1 = new BlockPos(chunkpos.getMaxBlockX(), i, chunkpos.getMaxBlockZ());
        BlockPos blockpos2 = null;
        double d0 = 0.0;

        for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos, blockpos1)) {
            BlockState blockstate = pChunk.getBlockState(blockpos3);
            BlockPos blockpos4 = blockpos3.above();
            BlockPos blockpos5 = blockpos3.above(2);
            if (blockstate.is(Blocks.END_STONE)
                && !pChunk.getBlockState(blockpos4).isCollisionShapeFullBlock(pChunk, blockpos4)
                && !pChunk.getBlockState(blockpos5).isCollisionShapeFullBlock(pChunk, blockpos5)) {
                double d1 = blockpos3.distToCenterSqr(0.0, 0.0, 0.0);
                if (blockpos2 == null || d1 < d0) {
                    blockpos2 = blockpos3;
                    d0 = d1;
                }
            }
        }

        return blockpos2;
    }

    private static void spawnGatewayPortal(ServerLevel pLevel, BlockPos pPos, EndGatewayConfiguration pConfig) {
        Feature.END_GATEWAY.place(pConfig, pLevel, pLevel.getChunkSource().getGenerator(), RandomSource.create(), pPos);
    }

    @Override
    public boolean shouldRenderFace(Direction pFace) {
        return Block.shouldRenderFace(this.getBlockState(), this.level, this.getBlockPos(), pFace, this.getBlockPos().relative(pFace));
    }

    public int getParticleAmount() {
        int i = 0;

        for (Direction direction : Direction.values()) {
            i += this.shouldRenderFace(direction) ? 1 : 0;
        }

        return i;
    }

    public void setExitPosition(BlockPos pExitPortal, boolean pExactTeleport) {
        this.exactTeleport = pExactTeleport;
        this.exitPortal = pExitPortal;
        this.setChanged();
    }
}

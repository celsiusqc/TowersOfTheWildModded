package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.WorldGenTickAccess;
import org.slf4j.Logger;

public class WorldGenRegion implements WorldGenLevel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final StaticCache2D<GenerationChunkHolder> cache;
    private final ChunkAccess center;
    private final ServerLevel level;
    private final long seed;
    private final LevelData levelData;
    private final RandomSource random;
    private final DimensionType dimensionType;
    private final WorldGenTickAccess<Block> blockTicks = new WorldGenTickAccess<>(p_313592_ -> this.getChunk(p_313592_).getBlockTicks());
    private final WorldGenTickAccess<Fluid> fluidTicks = new WorldGenTickAccess<>(p_313593_ -> this.getChunk(p_313593_).getFluidTicks());
    private final BiomeManager biomeManager;
    private final ChunkStep generatingStep;
    @Nullable
    private Supplier<String> currentlyGenerating;
    private final AtomicLong subTickCount = new AtomicLong();
    private static final ResourceLocation WORLDGEN_REGION_RANDOM = ResourceLocation.withDefaultNamespace("worldgen_region_random");

    public WorldGenRegion(ServerLevel p_143484_, StaticCache2D<GenerationChunkHolder> p_347548_, ChunkStep p_347626_, ChunkAccess p_347663_) {
        this.generatingStep = p_347626_;
        this.cache = p_347548_;
        this.center = p_347663_;
        this.level = p_143484_;
        this.seed = p_143484_.getSeed();
        this.levelData = p_143484_.getLevelData();
        this.random = p_143484_.getChunkSource().randomState().getOrCreateRandomFactory(WORLDGEN_REGION_RANDOM).at(this.center.getPos().getWorldPosition());
        this.dimensionType = p_143484_.dimensionType();
        this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed));
    }

    public boolean isOldChunkAround(ChunkPos pPos, int pRadius) {
        return this.level.getChunkSource().chunkMap.isOldChunkAround(pPos, pRadius);
    }

    public ChunkPos getCenter() {
        return this.center.getPos();
    }

    @Override
    public void setCurrentlyGenerating(@Nullable Supplier<String> pCurrentlyGenerating) {
        this.currentlyGenerating = pCurrentlyGenerating;
    }

    @Override
    public ChunkAccess getChunk(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, ChunkStatus.EMPTY);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int pX, int pZ, ChunkStatus pChunkStatus, boolean pRequireChunk) {
        int i = this.center.getPos().getChessboardDistance(pX, pZ);
        ChunkStatus chunkstatus = i >= this.generatingStep.directDependencies().size() ? null : this.generatingStep.directDependencies().get(i);
        GenerationChunkHolder generationchunkholder;
        if (chunkstatus != null) {
            generationchunkholder = this.cache.get(pX, pZ);
            if (pChunkStatus.isOrBefore(chunkstatus)) {
                ChunkAccess chunkaccess = generationchunkholder.getChunkIfPresentUnchecked(chunkstatus);
                if (chunkaccess != null) {
                    return chunkaccess;
                }
            }
        } else {
            generationchunkholder = null;
        }

        CrashReport crashreport = CrashReport.forThrowable(
            new IllegalStateException("Requested chunk unavailable during world generation"), "Exception generating new chunk"
        );
        CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk request details");
        crashreportcategory.setDetail("Requested chunk", String.format(Locale.ROOT, "%d, %d", pX, pZ));
        crashreportcategory.setDetail("Generating status", () -> this.generatingStep.targetStatus().getName());
        crashreportcategory.setDetail("Requested status", pChunkStatus::getName);
        crashreportcategory.setDetail(
            "Actual status", () -> generationchunkholder == null ? "[out of cache bounds]" : generationchunkholder.getPersistedStatus().getName()
        );
        crashreportcategory.setDetail("Maximum allowed status", () -> chunkstatus == null ? "null" : chunkstatus.getName());
        crashreportcategory.setDetail("Dependencies", this.generatingStep.directDependencies()::toString);
        crashreportcategory.setDetail("Requested distance", i);
        crashreportcategory.setDetail("Generating chunk", this.center.getPos()::toString);
        throw new ReportedException(crashreport);
    }

    @Override
    public boolean hasChunk(int pChunkX, int pChunkZ) {
        int i = this.center.getPos().getChessboardDistance(pChunkX, pChunkZ);
        return i < this.generatingStep.directDependencies().size();
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ())).getBlockState(pPos);
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return this.getChunk(pPos).getFluidState(pPos);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(double pX, double pY, double pZ, double pDistance, Predicate<Entity> pPredicate) {
        return null;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
        return this.level.getUncachedNoiseBiome(pX, pY, pZ);
    }

    @Override
    public float getShade(Direction pDirection, boolean pShade) {
        return 1.0F;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public boolean destroyBlock(BlockPos pPos, boolean pDropBlock, @Nullable Entity pEntity, int pRecursionLeft) {
        BlockState blockstate = this.getBlockState(pPos);
        if (blockstate.isAir()) {
            return false;
        } else {
            if (pDropBlock) {
                BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(pPos) : null;
                Block.dropResources(blockstate, this.level, pPos, blockentity, pEntity, ItemStack.EMPTY);
            }

            return this.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3, pRecursionLeft);
        }
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        ChunkAccess chunkaccess = this.getChunk(pPos);
        BlockEntity blockentity = chunkaccess.getBlockEntity(pPos);
        if (blockentity != null) {
            return blockentity;
        } else {
            CompoundTag compoundtag = chunkaccess.getBlockEntityNbt(pPos);
            BlockState blockstate = chunkaccess.getBlockState(pPos);
            if (compoundtag != null) {
                if ("DUMMY".equals(compoundtag.getString("id"))) {
                    if (!blockstate.hasBlockEntity()) {
                        return null;
                    }

                    blockentity = ((EntityBlock)blockstate.getBlock()).newBlockEntity(pPos, blockstate);
                } else {
                    blockentity = BlockEntity.loadStatic(pPos, blockstate, compoundtag, this.level.registryAccess());
                }

                if (blockentity != null) {
                    chunkaccess.setBlockEntity(blockentity);
                    return blockentity;
                }
            }

            if (blockstate.hasBlockEntity()) {
                LOGGER.warn("Tried to access a block entity before it was created. {}", pPos);
            }

            return null;
        }
    }

    @Override
    public boolean ensureCanWrite(BlockPos pPos) {
        int i = SectionPos.blockToSectionCoord(pPos.getX());
        int j = SectionPos.blockToSectionCoord(pPos.getZ());
        ChunkPos chunkpos = this.getCenter();
        int k = Math.abs(chunkpos.x - i);
        int l = Math.abs(chunkpos.z - j);
        if (k <= this.generatingStep.blockStateWriteRadius() && l <= this.generatingStep.blockStateWriteRadius()) {
            if (this.center.isUpgrading()) {
                LevelHeightAccessor levelheightaccessor = this.center.getHeightAccessorForGeneration();
                if (pPos.getY() < levelheightaccessor.getMinBuildHeight() || pPos.getY() >= levelheightaccessor.getMaxBuildHeight()) {
                    return false;
                }
            }

            return true;
        } else {
            Util.logAndPauseIfInIde(
                "Detected setBlock in a far chunk ["
                    + i
                    + ", "
                    + j
                    + "], pos: "
                    + pPos
                    + ", status: "
                    + this.generatingStep.targetStatus()
                    + (this.currentlyGenerating == null ? "" : ", currently generating: " + this.currentlyGenerating.get())
            );
            return false;
        }
    }

    @Override
    public boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft) {
        if (!this.ensureCanWrite(pPos)) {
            return false;
        } else {
            ChunkAccess chunkaccess = this.getChunk(pPos);
            BlockState blockstate = chunkaccess.setBlockState(pPos, pState, false);
            if (blockstate != null) {
                this.level.onBlockStateChange(pPos, blockstate, pState);
            }

            if (pState.hasBlockEntity()) {
                if (chunkaccess.getPersistedStatus().getChunkType() == ChunkType.LEVELCHUNK) {
                    BlockEntity blockentity = ((EntityBlock)pState.getBlock()).newBlockEntity(pPos, pState);
                    if (blockentity != null) {
                        chunkaccess.setBlockEntity(blockentity);
                    } else {
                        chunkaccess.removeBlockEntity(pPos);
                    }
                } else {
                    CompoundTag compoundtag = new CompoundTag();
                    compoundtag.putInt("x", pPos.getX());
                    compoundtag.putInt("y", pPos.getY());
                    compoundtag.putInt("z", pPos.getZ());
                    compoundtag.putString("id", "DUMMY");
                    chunkaccess.setBlockEntityNbt(compoundtag);
                }
            } else if (blockstate != null && blockstate.hasBlockEntity()) {
                chunkaccess.removeBlockEntity(pPos);
            }

            if (pState.hasPostProcess(this, pPos)) {
                this.markPosForPostprocessing(pPos);
            }

            return true;
        }
    }

    private void markPosForPostprocessing(BlockPos pPos) {
        this.getChunk(pPos).markPosForPostprocessing(pPos);
    }

    @Override
    public boolean addFreshEntity(Entity pEntity) {
        if (pEntity instanceof net.minecraft.world.entity.Mob mob && mob.isSpawnCancelled()) return false;
        int i = SectionPos.blockToSectionCoord(pEntity.getBlockX());
        int j = SectionPos.blockToSectionCoord(pEntity.getBlockZ());
        this.getChunk(i, j).addEntity(pEntity);
        return true;
    }

    @Override
    public boolean removeBlock(BlockPos pPos, boolean pIsMoving) {
        return this.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Deprecated
    @Override
    public ServerLevel getLevel() {
        return this.level;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.level.enabledFeatures();
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pPos) {
        if (!this.hasChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        } else {
            return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(), 0L, this.level.getMoonBrightness());
        }
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return this.level.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.level.getChunkSource();
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ) {
        return this.getChunk(SectionPos.blockToSectionCoord(pX), SectionPos.blockToSectionCoord(pZ)).getHeight(pHeightmapType, pX & 15, pZ & 15)
            + 1;
    }

    /**
     * Plays a sound. On the server, the sound is broadcast to all nearby <em>except</em> the given player. On the client, the sound only plays if the given player is the client player. Thus, this method is intended to be called from code running on both sides. The client plays it locally and the server plays it for everyone else.
     */
    @Override
    public void playSound(@Nullable Player pPlayer, BlockPos pPos, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch) {
    }

    @Override
    public void addParticle(ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
    }

    @Override
    public void levelEvent(@Nullable Player pPlayer, int pType, BlockPos pPos, int pData) {
    }

    @Override
    public void gameEvent(Holder<GameEvent> pGameEvent, Vec3 pPos, GameEvent.Context pContext) {
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionType;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pPos, Predicate<BlockState> pState) {
        return pState.test(this.getBlockState(pPos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pPos, Predicate<FluidState> pPredicate) {
        return pPredicate.test(this.getFluidState(pPos));
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB pBounds, Predicate<? super T> pPredicate) {
        return Collections.emptyList();
    }

    /**
     * Gets all entities within the specified AABB excluding the one passed into it.
     */
    @Override
    public List<Entity> getEntities(@Nullable Entity pEntity, AABB pBoundingBox, @Nullable Predicate<? super Entity> pPredicate) {
        return Collections.emptyList();
    }

    @Override
    public List<Player> players() {
        return Collections.emptyList();
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public long nextSubTickCount() {
        return this.subTickCount.getAndIncrement();
    }
}

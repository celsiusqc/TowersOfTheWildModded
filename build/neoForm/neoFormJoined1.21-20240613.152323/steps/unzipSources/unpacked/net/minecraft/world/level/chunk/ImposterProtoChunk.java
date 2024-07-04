package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;

/**
 * During world generation, adjacent chunks may be fully generated (and thus be level chunks), but are often needed in proto chunk form. This wraps a completely generated chunk as a proto chunk.
 */
public class ImposterProtoChunk extends ProtoChunk {
    private final LevelChunk wrapped;
    private final boolean allowWrites;

    public ImposterProtoChunk(LevelChunk pWrapped, boolean pAllowWrites) {
        super(
            pWrapped.getPos(),
            UpgradeData.EMPTY,
            pWrapped.levelHeightAccessor,
            pWrapped.getLevel().registryAccess().registryOrThrow(Registries.BIOME),
            pWrapped.getBlendingData()
        );
        this.wrapped = pWrapped;
        this.allowWrites = pAllowWrites;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        return this.wrapped.getBlockEntity(pPos);
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        return this.wrapped.getBlockState(pPos);
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return this.wrapped.getFluidState(pPos);
    }

    @Override
    public int getMaxLightLevel() {
        return this.wrapped.getMaxLightLevel();
    }

    @Override
    public LevelChunkSection getSection(int pIndex) {
        return this.allowWrites ? this.wrapped.getSection(pIndex) : super.getSection(pIndex);
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving) {
        return this.allowWrites ? this.wrapped.setBlockState(pPos, pState, pIsMoving) : null;
    }

    @Override
    public void setBlockEntity(BlockEntity pBlockEntity) {
        if (this.allowWrites) {
            this.wrapped.setBlockEntity(pBlockEntity);
        }
    }

    @Override
    public void addEntity(Entity pEntity) {
        if (this.allowWrites) {
            this.wrapped.addEntity(pEntity);
        }
    }

    @Override
    public void setPersistedStatus(ChunkStatus p_347629_) {
        if (this.allowWrites) {
            super.setPersistedStatus(p_347629_);
        }
    }

    @Override
    public LevelChunkSection[] getSections() {
        return this.wrapped.getSections();
    }

    @Override
    public void setHeightmap(Heightmap.Types pType, long[] pData) {
    }

    private Heightmap.Types fixType(Heightmap.Types pType) {
        if (pType == Heightmap.Types.WORLD_SURFACE_WG) {
            return Heightmap.Types.WORLD_SURFACE;
        } else {
            return pType == Heightmap.Types.OCEAN_FLOOR_WG ? Heightmap.Types.OCEAN_FLOOR : pType;
        }
    }

    @Override
    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types pType) {
        return this.wrapped.getOrCreateHeightmapUnprimed(pType);
    }

    @Override
    public int getHeight(Heightmap.Types pType, int pX, int pZ) {
        return this.wrapped.getHeight(this.fixType(pType), pX, pZ);
    }

    /**
     * Gets the biome at the given quart positions.
     * Note that the coordinates passed into this method are 1/4 the scale of block coordinates.
     */
    @Override
    public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
        return this.wrapped.getNoiseBiome(pX, pY, pZ);
    }

    @Override
    public ChunkPos getPos() {
        return this.wrapped.getPos();
    }

    @Nullable
    @Override
    public StructureStart getStartForStructure(Structure pStructure) {
        return this.wrapped.getStartForStructure(pStructure);
    }

    @Override
    public void setStartForStructure(Structure pStructure, StructureStart pStructureStart) {
    }

    @Override
    public Map<Structure, StructureStart> getAllStarts() {
        return this.wrapped.getAllStarts();
    }

    @Override
    public void setAllStarts(Map<Structure, StructureStart> pStructureStarts) {
    }

    @Override
    public LongSet getReferencesForStructure(Structure pStructure) {
        return this.wrapped.getReferencesForStructure(pStructure);
    }

    @Override
    public void addReferenceForStructure(Structure pStructure, long pReference) {
    }

    @Override
    public Map<Structure, LongSet> getAllReferences() {
        return this.wrapped.getAllReferences();
    }

    @Override
    public void setAllReferences(Map<Structure, LongSet> pStructureReferencesMap) {
    }

    @Override
    public void setUnsaved(boolean pUnsaved) {
        this.wrapped.setUnsaved(pUnsaved);
    }

    @Override
    public boolean isUnsaved() {
        return false;
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return this.wrapped.getPersistedStatus();
    }

    @Override
    public void removeBlockEntity(BlockPos pPos) {
    }

    @Override
    public void markPosForPostprocessing(BlockPos pPos) {
    }

    @Override
    public void setBlockEntityNbt(CompoundTag pTag) {
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbt(BlockPos pPos) {
        return this.wrapped.getBlockEntityNbt(pPos);
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pPos, HolderLookup.Provider pRegistries) {
        return this.wrapped.getBlockEntityNbtForSaving(pPos, pRegistries);
    }

    @Override
    public void findBlocks(Predicate<BlockState> pPredicate, BiConsumer<BlockPos, BlockState> pOutput) {
        this.wrapped.findBlocks(pPredicate, pOutput);
    }

    @Override
    public void findBlocks(java.util.function.BiPredicate<BlockState, BlockPos> p_285465_, BiConsumer<BlockPos, BlockState> p_285030_) {
        this.wrapped.findBlocks(p_285465_, p_285030_);
    }

    @Override
    public void findBlocks(Predicate<BlockState> p_285465_, java.util.function.BiPredicate<BlockState, BlockPos> fineFilter, BiConsumer<BlockPos, BlockState> p_285030_) {
        this.wrapped.findBlocks(p_285465_, fineFilter, p_285030_);
    }

    public TickContainerAccess<Block> getBlockTicks() {
        return this.allowWrites ? this.wrapped.getBlockTicks() : BlackholeTickAccess.emptyContainer();
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.allowWrites ? this.wrapped.getFluidTicks() : BlackholeTickAccess.emptyContainer();
    }

    @Override
    public ChunkAccess.TicksToSave getTicksForSerialization() {
        return this.wrapped.getTicksForSerialization();
    }

    @Nullable
    @Override
    public BlendingData getBlendingData() {
        return this.wrapped.getBlendingData();
    }

    @Override
    public void setBlendingData(BlendingData pBlendingData) {
        this.wrapped.setBlendingData(pBlendingData);
    }

    @Override
    public CarvingMask getCarvingMask(GenerationStep.Carving pStep) {
        if (this.allowWrites) {
            return super.getCarvingMask(pStep);
        } else {
            throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
        }
    }

    @Override
    public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving pStep) {
        if (this.allowWrites) {
            return super.getOrCreateCarvingMask(pStep);
        } else {
            throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
        }
    }

    public LevelChunk getWrapped() {
        return this.wrapped;
    }

    @Override
    public boolean isLightCorrect() {
        return this.wrapped.isLightCorrect();
    }

    @Override
    public void setLightCorrect(boolean pLightCorrect) {
        this.wrapped.setLightCorrect(pLightCorrect);
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver pResolver, Climate.Sampler pSampler) {
        if (this.allowWrites) {
            this.wrapped.fillBiomesFromNoise(pResolver, pSampler);
        }
    }

    @Override
    public void initializeLightSources() {
        this.wrapped.initializeLightSources();
    }

    @Override
    public ChunkSkyLightSources getSkyLightSources() {
        return this.wrapped.getSkyLightSources();
    }

    @Override
    public net.neoforged.neoforge.attachment.AttachmentHolder.AsField getAttachmentHolder() {
        return wrapped.getAttachmentHolder();
    }
}

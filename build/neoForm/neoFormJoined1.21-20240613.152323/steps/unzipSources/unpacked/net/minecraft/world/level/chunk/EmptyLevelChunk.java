package net.minecraft.world.level.chunk;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class EmptyLevelChunk extends LevelChunk {
    private final Holder<Biome> biome;

    public EmptyLevelChunk(Level pLevel, ChunkPos pPos, Holder<Biome> pBiome) {
        super(pLevel, pPos);
        this.biome = pBiome;
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        return Blocks.VOID_AIR.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving) {
        return null;
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getLightEmission(BlockPos pPos) {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos, LevelChunk.EntityCreationType pCreationType) {
        return null;
    }

    @Override
    public void addAndRegisterBlockEntity(BlockEntity pBlockEntity) {
    }

    @Override
    public void setBlockEntity(BlockEntity pBlockEntity) {
    }

    @Override
    public void removeBlockEntity(BlockPos pPos) {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isYSpaceEmpty(int pStartY, int pEndY) {
        return true;
    }

    @Override
    public boolean isSectionEmpty(int p_350288_) {
        return true;
    }

    @Override
    public FullChunkStatus getFullStatus() {
        return FullChunkStatus.FULL;
    }

    /**
     * Gets the biome at the given quart positions.
     * Note that the coordinates passed into this method are 1/4 the scale of block coordinates.
     */
    @Override
    public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
        return this.biome;
    }
}

package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ViewArea {
    protected final LevelRenderer levelRenderer;
    protected final Level level;
    protected int sectionGridSizeY;
    protected int sectionGridSizeX;
    protected int sectionGridSizeZ;
    private int viewDistance;
    public SectionRenderDispatcher.RenderSection[] sections;

    public ViewArea(SectionRenderDispatcher pSectionRenderDispatcher, Level pLevel, int pViewDistance, LevelRenderer pLevelRenderer) {
        this.levelRenderer = pLevelRenderer;
        this.level = pLevel;
        this.setViewDistance(pViewDistance);
        this.createSections(pSectionRenderDispatcher);
    }

    protected void createSections(SectionRenderDispatcher pSectionRenderDispatcher) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
        } else {
            int i = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
            this.sections = new SectionRenderDispatcher.RenderSection[i];

            for (int j = 0; j < this.sectionGridSizeX; j++) {
                for (int k = 0; k < this.sectionGridSizeY; k++) {
                    for (int l = 0; l < this.sectionGridSizeZ; l++) {
                        int i1 = this.getSectionIndex(j, k, l);
                        this.sections[i1] = pSectionRenderDispatcher.new RenderSection(i1, j * 16, this.level.getMinBuildHeight() + k * 16, l * 16);
                    }
                }
            }
        }
    }

    public void releaseAllBuffers() {
        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.sections) {
            sectionrenderdispatcher$rendersection.releaseBuffers();
        }
    }

    private int getSectionIndex(int pX, int pY, int pZ) {
        return (pZ * this.sectionGridSizeY + pY) * this.sectionGridSizeX + pX;
    }

    protected void setViewDistance(int pRenderDistanceChunks) {
        int i = pRenderDistanceChunks * 2 + 1;
        this.sectionGridSizeX = i;
        this.sectionGridSizeY = this.level.getSectionsCount();
        this.sectionGridSizeZ = i;
        this.viewDistance = pRenderDistanceChunks;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public LevelHeightAccessor getLevelHeightAccessor() {
        return this.level;
    }

    public void repositionCamera(double pViewEntityX, double pViewEntityZ) {
        int i = Mth.ceil(pViewEntityX);
        int j = Mth.ceil(pViewEntityZ);

        for (int k = 0; k < this.sectionGridSizeX; k++) {
            int l = this.sectionGridSizeX * 16;
            int i1 = i - 8 - l / 2;
            int j1 = i1 + Math.floorMod(k * 16 - i1, l);

            for (int k1 = 0; k1 < this.sectionGridSizeZ; k1++) {
                int l1 = this.sectionGridSizeZ * 16;
                int i2 = j - 8 - l1 / 2;
                int j2 = i2 + Math.floorMod(k1 * 16 - i2, l1);

                for (int k2 = 0; k2 < this.sectionGridSizeY; k2++) {
                    int l2 = this.level.getMinBuildHeight() + k2 * 16;
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(k, k2, k1)];
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    if (j1 != blockpos.getX() || l2 != blockpos.getY() || j2 != blockpos.getZ()) {
                        sectionrenderdispatcher$rendersection.setOrigin(j1, l2, j2);
                    }
                }
            }
        }
    }

    public void setDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
        int i = Math.floorMod(pSectionX, this.sectionGridSizeX);
        int j = Math.floorMod(pSectionY - this.level.getMinSection(), this.sectionGridSizeY);
        int k = Math.floorMod(pSectionZ, this.sectionGridSizeZ);
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(i, j, k)];
        sectionrenderdispatcher$rendersection.setDirty(pReRenderOnMainThread);
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pPos) {
        int i = Mth.floorDiv(pPos.getY() - this.level.getMinBuildHeight(), 16);
        if (i >= 0 && i < this.sectionGridSizeY) {
            int j = Mth.positiveModulo(Mth.floorDiv(pPos.getX(), 16), this.sectionGridSizeX);
            int k = Mth.positiveModulo(Mth.floorDiv(pPos.getZ(), 16), this.sectionGridSizeZ);
            return this.sections[this.getSectionIndex(j, i, k)];
        } else {
            return null;
        }
    }
}

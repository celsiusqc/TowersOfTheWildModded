package net.minecraft.gametest.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class StructureGridSpawner implements GameTestRunner.StructureSpawner {
    private static final int SPACE_BETWEEN_COLUMNS = 5;
    private static final int SPACE_BETWEEN_ROWS = 6;
    private final int testsPerRow;
    private int currentRowCount;
    private AABB rowBounds;
    private final BlockPos.MutableBlockPos nextTestNorthWestCorner;
    private final BlockPos firstTestNorthWestCorner;
    private final boolean clearOnBatch;
    private float maxX = -1.0F;
    private final Collection<GameTestInfo> testInLastBatch = new ArrayList<>();

    public StructureGridSpawner(BlockPos pFirstTestNorthWestCorner, int pTestsPerRow, boolean p_352391_) {
        this.testsPerRow = pTestsPerRow;
        this.nextTestNorthWestCorner = pFirstTestNorthWestCorner.mutable();
        this.rowBounds = new AABB(this.nextTestNorthWestCorner);
        this.firstTestNorthWestCorner = pFirstTestNorthWestCorner;
        this.clearOnBatch = p_352391_;
    }

    @Override
    public void onBatchStart(ServerLevel p_352155_) {
        if (this.clearOnBatch) {
            this.testInLastBatch.forEach(p_352408_ -> {
                BoundingBox boundingbox = StructureUtils.getStructureBoundingBox(p_352408_.getStructureBlockEntity());
                StructureUtils.clearSpaceForStructure(boundingbox, p_352155_);
            });
            this.testInLastBatch.clear();
            this.rowBounds = new AABB(this.firstTestNorthWestCorner);
            this.nextTestNorthWestCorner.set(this.firstTestNorthWestCorner);
        }
    }

    @Override
    public Optional<GameTestInfo> spawnStructure(GameTestInfo pGameTestInfo) {
        BlockPos blockpos = new BlockPos(this.nextTestNorthWestCorner);
        pGameTestInfo.setNorthWestCorner(blockpos);
        pGameTestInfo.prepareTestStructure();
        AABB aabb = StructureUtils.getStructureBounds(pGameTestInfo.getStructureBlockEntity());
        this.rowBounds = this.rowBounds.minmax(aabb);
        this.nextTestNorthWestCorner.move((int)aabb.getXsize() + 5, 0, 0);
        if ((float)this.nextTestNorthWestCorner.getX() > this.maxX) {
            this.maxX = (float)this.nextTestNorthWestCorner.getX();
        }

        if (++this.currentRowCount >= this.testsPerRow) {
            this.currentRowCount = 0;
            this.nextTestNorthWestCorner.move(0, 0, (int)this.rowBounds.getZsize() + 6);
            this.nextTestNorthWestCorner.setX(this.firstTestNorthWestCorner.getX());
            this.rowBounds = new AABB(this.nextTestNorthWestCorner);
        }

        this.testInLastBatch.add(pGameTestInfo);
        return Optional.of(pGameTestInfo);
    }
}
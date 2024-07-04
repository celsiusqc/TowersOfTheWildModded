package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;

public class StructurePiecesBuilder implements StructurePieceAccessor {
    private final List<StructurePiece> pieces = Lists.newArrayList();

    @Override
    public void addPiece(StructurePiece pPiece) {
        this.pieces.add(pPiece);
    }

    @Nullable
    @Override
    public StructurePiece findCollisionPiece(BoundingBox pBox) {
        return StructurePiece.findCollisionPiece(this.pieces, pBox);
    }

    @Deprecated
    public void offsetPiecesVertically(int pOffset) {
        for (StructurePiece structurepiece : this.pieces) {
            structurepiece.move(0, pOffset, 0);
        }
    }

    @Deprecated
    public int moveBelowSeaLevel(int pSeaLevel, int pMinY, RandomSource pRandom, int pAmount) {
        int i = pSeaLevel - pAmount;
        BoundingBox boundingbox = this.getBoundingBox();
        int j = boundingbox.getYSpan() + pMinY + 1;
        if (j < i) {
            j += pRandom.nextInt(i - j);
        }

        int k = j - boundingbox.maxY();
        this.offsetPiecesVertically(k);
        return k;
    }

    /**
 * @deprecated
 */
    public void moveInsideHeights(RandomSource pRandom, int pMinY, int pMaxY) {
        BoundingBox boundingbox = this.getBoundingBox();
        int i = pMaxY - pMinY + 1 - boundingbox.getYSpan();
        int j;
        if (i > 1) {
            j = pMinY + pRandom.nextInt(i);
        } else {
            j = pMinY;
        }

        int k = j - boundingbox.minY();
        this.offsetPiecesVertically(k);
    }

    public PiecesContainer build() {
        return new PiecesContainer(this.pieces);
    }

    public void clear() {
        this.pieces.clear();
    }

    public boolean isEmpty() {
        return this.pieces.isEmpty();
    }

    public BoundingBox getBoundingBox() {
        return StructurePiece.createBoundingBox(this.pieces.stream());
    }
}

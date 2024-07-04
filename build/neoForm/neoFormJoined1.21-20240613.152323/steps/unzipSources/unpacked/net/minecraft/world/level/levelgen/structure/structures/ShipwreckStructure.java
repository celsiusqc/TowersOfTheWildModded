package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class ShipwreckStructure extends Structure {
    public static final MapCodec<ShipwreckStructure> CODEC = RecordCodecBuilder.mapCodec(
        p_229401_ -> p_229401_.group(settingsCodec(p_229401_), Codec.BOOL.fieldOf("is_beached").forGetter(p_229399_ -> p_229399_.isBeached))
                .apply(p_229401_, ShipwreckStructure::new)
    );
    public final boolean isBeached;

    public ShipwreckStructure(Structure.StructureSettings p_229388_, boolean p_229389_) {
        super(p_229388_);
        this.isBeached = p_229389_;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
        Heightmap.Types heightmap$types = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
        return onTopOfChunkCenter(pContext, heightmap$types, p_229394_ -> this.generatePieces(p_229394_, pContext));
    }

    private void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
        Rotation rotation = Rotation.getRandom(pContext.random());
        BlockPos blockpos = new BlockPos(pContext.chunkPos().getMinBlockX(), 90, pContext.chunkPos().getMinBlockZ());
        ShipwreckPieces.ShipwreckPiece shipwreckpieces$shipwreckpiece = ShipwreckPieces.addRandomPiece(
            pContext.structureTemplateManager(), blockpos, rotation, pBuilder, pContext.random(), this.isBeached
        );
        if (shipwreckpieces$shipwreckpiece.isTooBigToFitInWorldGenRegion()) {
            BoundingBox boundingbox = shipwreckpieces$shipwreckpiece.getBoundingBox();
            int i;
            if (this.isBeached) {
                int j = Structure.getLowestY(pContext, boundingbox.minX(), boundingbox.getXSpan(), boundingbox.minZ(), boundingbox.getZSpan());
                i = shipwreckpieces$shipwreckpiece.calculateBeachedPosition(j, pContext.random());
            } else {
                i = Structure.getMeanFirstOccupiedHeight(pContext, boundingbox.minX(), boundingbox.getXSpan(), boundingbox.minZ(), boundingbox.getZSpan());
            }

            shipwreckpieces$shipwreckpiece.adjustPositionHeight(i);
        }
    }

    @Override
    public StructureType<?> type() {
        return StructureType.SHIPWRECK;
    }
}

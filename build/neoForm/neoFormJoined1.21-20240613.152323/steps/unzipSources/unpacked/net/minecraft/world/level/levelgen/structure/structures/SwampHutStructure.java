package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class SwampHutStructure extends Structure {
    public static final MapCodec<SwampHutStructure> CODEC = simpleCodec(SwampHutStructure::new);

    public SwampHutStructure(Structure.StructureSettings p_229974_) {
        super(p_229974_);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
        return onTopOfChunkCenter(pContext, Heightmap.Types.WORLD_SURFACE_WG, p_229979_ -> generatePieces(p_229979_, pContext));
    }

    private static void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
        pBuilder.addPiece(new SwampHutPiece(pContext.random(), pContext.chunkPos().getMinBlockX(), pContext.chunkPos().getMinBlockZ()));
    }

    @Override
    public StructureType<?> type() {
        return StructureType.SWAMP_HUT;
    }
}

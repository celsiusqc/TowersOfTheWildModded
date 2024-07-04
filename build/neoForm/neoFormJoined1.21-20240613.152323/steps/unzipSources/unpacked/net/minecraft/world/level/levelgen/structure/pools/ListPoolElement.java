package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class ListPoolElement extends StructurePoolElement {
    public static final MapCodec<ListPoolElement> CODEC = RecordCodecBuilder.mapCodec(
        p_352016_ -> p_352016_.group(StructurePoolElement.CODEC.listOf().fieldOf("elements").forGetter(p_210369_ -> p_210369_.elements), projectionCodec())
                .apply(p_352016_, ListPoolElement::new)
    );
    private final List<StructurePoolElement> elements;

    public ListPoolElement(List<StructurePoolElement> p_210363_, StructureTemplatePool.Projection p_210364_) {
        super(p_210364_);
        if (p_210363_.isEmpty()) {
            throw new IllegalArgumentException("Elements are empty");
        } else {
            this.elements = p_210363_;
            this.setProjectionOnEachElement(p_210364_);
        }
    }

    @Override
    public Vec3i getSize(StructureTemplateManager pStructureTemplateManager, Rotation pRotation) {
        int i = 0;
        int j = 0;
        int k = 0;

        for (StructurePoolElement structurepoolelement : this.elements) {
            Vec3i vec3i = structurepoolelement.getSize(pStructureTemplateManager, pRotation);
            i = Math.max(i, vec3i.getX());
            j = Math.max(j, vec3i.getY());
            k = Math.max(k, vec3i.getZ());
        }

        return new Vec3i(i, j, k);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
        StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation, RandomSource pRandom
    ) {
        return this.elements.get(0).getShuffledJigsawBlocks(pStructureTemplateManager, pPos, pRotation, pRandom);
    }

    @Override
    public BoundingBox getBoundingBox(StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation) {
        Stream<BoundingBox> stream = this.elements
            .stream()
            .filter(p_210371_ -> p_210371_ != EmptyPoolElement.INSTANCE)
            .map(p_227298_ -> p_227298_.getBoundingBox(pStructureTemplateManager, pPos, pRotation));
        return BoundingBox.encapsulatingBoxes(stream::iterator)
            .orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox for ListPoolElement"));
    }

    @Override
    public boolean place(
        StructureTemplateManager p_227272_,
        WorldGenLevel p_227273_,
        StructureManager p_227274_,
        ChunkGenerator p_227275_,
        BlockPos p_227276_,
        BlockPos p_227277_,
        Rotation p_227278_,
        BoundingBox p_227279_,
        RandomSource p_227280_,
        LiquidSettings p_352445_,
        boolean p_227281_
    ) {
        for (StructurePoolElement structurepoolelement : this.elements) {
            if (!structurepoolelement.place(
                p_227272_, p_227273_, p_227274_, p_227275_, p_227276_, p_227277_, p_227278_, p_227279_, p_227280_, p_352445_, p_227281_
            )) {
                return false;
            }
        }

        return true;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return StructurePoolElementType.LIST;
    }

    @Override
    public StructurePoolElement setProjection(StructureTemplatePool.Projection pProjection) {
        super.setProjection(pProjection);
        this.setProjectionOnEachElement(pProjection);
        return this;
    }

    @Override
    public String toString() {
        return "List[" + this.elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }

    private void setProjectionOnEachElement(StructureTemplatePool.Projection pProjection) {
        this.elements.forEach(p_210376_ -> p_210376_.setProjection(pProjection));
    }
}

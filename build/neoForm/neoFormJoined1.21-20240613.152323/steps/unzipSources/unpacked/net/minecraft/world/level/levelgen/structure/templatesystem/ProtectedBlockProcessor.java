package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;

public class ProtectedBlockProcessor extends StructureProcessor {
    public final TagKey<Block> cannotReplace;
    public static final MapCodec<ProtectedBlockProcessor> CODEC = TagKey.hashedCodec(Registries.BLOCK)
        .xmap(ProtectedBlockProcessor::new, p_205053_ -> p_205053_.cannotReplace)
        .fieldOf("value");

    public ProtectedBlockProcessor(TagKey<Block> p_205051_) {
        this.cannotReplace = p_205051_;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
        LevelReader pLevel,
        BlockPos pOffset,
        BlockPos pPos,
        StructureTemplate.StructureBlockInfo pBlockInfo,
        StructureTemplate.StructureBlockInfo pRelativeBlockInfo,
        StructurePlaceSettings pSettings
    ) {
        return Feature.isReplaceable(this.cannotReplace).test(pLevel.getBlockState(pRelativeBlockInfo.pos())) ? pRelativeBlockInfo : null;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.PROTECTED_BLOCKS;
    }
}

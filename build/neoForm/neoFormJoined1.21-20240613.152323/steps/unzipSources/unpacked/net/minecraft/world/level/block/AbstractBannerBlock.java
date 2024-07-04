package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractBannerBlock extends BaseEntityBlock {
    private final DyeColor color;

    protected AbstractBannerBlock(DyeColor pColor, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.color = pColor;
    }

    @Override
    protected abstract MapCodec<? extends AbstractBannerBlock> codec();

    @Override
    public boolean isPossibleToRespawnInThis(BlockState pState) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BannerBlockEntity(pPos, pState, this.color);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return pLevel.getBlockEntity(pPos) instanceof BannerBlockEntity bannerblockentity
            ? bannerblockentity.getItem()
            : super.getCloneItemStack(pLevel, pPos, pState);
    }

    public DyeColor getColor() {
        return this.color;
    }
}

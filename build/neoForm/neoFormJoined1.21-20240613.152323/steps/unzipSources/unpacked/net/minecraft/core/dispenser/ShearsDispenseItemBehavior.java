package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class ShearsDispenseItemBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
        ServerLevel serverlevel = pBlockSource.level();
        if (!serverlevel.isClientSide()) {
            BlockPos blockpos = pBlockSource.pos().relative(pBlockSource.state().getValue(DispenserBlock.FACING));
            this.setSuccess(tryShearBeehive(serverlevel, blockpos) || tryShearLivingEntity(serverlevel, blockpos));
            if (this.isSuccess()) {
                pItem.hurtAndBreak(1, serverlevel, null, p_348118_ -> {
                });
            }
        }

        return pItem;
    }

    private static boolean tryShearBeehive(ServerLevel pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        if (blockstate.is(BlockTags.BEEHIVES, p_202454_ -> p_202454_.hasProperty(BeehiveBlock.HONEY_LEVEL) && p_202454_.getBlock() instanceof BeehiveBlock)) {
            int i = blockstate.getValue(BeehiveBlock.HONEY_LEVEL);
            if (i >= 5) {
                pLevel.playSound(null, pPos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                BeehiveBlock.dropHoneycomb(pLevel, pPos);
                ((BeehiveBlock)blockstate.getBlock())
                    .releaseBeesAndResetHoneyLevel(pLevel, blockstate, pPos, null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                pLevel.gameEvent(null, GameEvent.SHEAR, pPos);
                return true;
            }
        }

        return false;
    }

    private static boolean tryShearLivingEntity(ServerLevel pLevel, BlockPos pPos) {
        for (LivingEntity livingentity : pLevel.getEntitiesOfClass(LivingEntity.class, new AABB(pPos), EntitySelector.NO_SPECTATORS)) {
            if (livingentity instanceof Shearable shearable && shearable.readyForShearing()) {
                shearable.shear(SoundSource.BLOCKS);
                pLevel.gameEvent(null, GameEvent.SHEAR, pPos);
                return true;
            }
        }

        return false;
    }
}

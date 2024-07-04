package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BucketItem extends Item implements DispensibleContainerItem {
    public final Fluid content;

    public BucketItem(Fluid pContent, Item.Properties pProperties) {
        super(pProperties);
        this.content = pContent;
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link #onItemUse}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(
            pLevel, pPlayer, this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE
        );
        if (blockhitresult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        } else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        } else {
            BlockPos blockpos = blockhitresult.getBlockPos();
            Direction direction = blockhitresult.getDirection();
            BlockPos blockpos1 = blockpos.relative(direction);
            if (!pLevel.mayInteract(pPlayer, blockpos) || !pPlayer.mayUseItemAt(blockpos1, direction, itemstack)) {
                return InteractionResultHolder.fail(itemstack);
            } else if (this.content == Fluids.EMPTY) {
                BlockState blockstate1 = pLevel.getBlockState(blockpos);
                if (blockstate1.getBlock() instanceof BucketPickup bucketpickup) {
                    ItemStack itemstack3 = bucketpickup.pickupBlock(pPlayer, pLevel, blockpos, blockstate1);
                    if (!itemstack3.isEmpty()) {
                        pPlayer.awardStat(Stats.ITEM_USED.get(this));
                        bucketpickup.getPickupSound(blockstate1).ifPresent(p_150709_ -> pPlayer.playSound(p_150709_, 1.0F, 1.0F));
                        pLevel.gameEvent(pPlayer, GameEvent.FLUID_PICKUP, blockpos);
                        ItemStack itemstack2 = ItemUtils.createFilledResult(itemstack, pPlayer, itemstack3);
                        if (!pLevel.isClientSide) {
                            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)pPlayer, itemstack3);
                        }

                        return InteractionResultHolder.sidedSuccess(itemstack2, pLevel.isClientSide());
                    }
                }

                return InteractionResultHolder.fail(itemstack);
            } else {
                BlockState blockstate = pLevel.getBlockState(blockpos);
                BlockPos blockpos2 = canBlockContainFluid(pPlayer, pLevel, blockpos, blockstate) ? blockpos : blockpos1;
                if (this.emptyContents(pPlayer, pLevel, blockpos2, blockhitresult, itemstack)) {
                    this.checkExtraContent(pPlayer, pLevel, itemstack, blockpos2);
                    if (pPlayer instanceof ServerPlayer) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)pPlayer, blockpos2, itemstack);
                    }

                    pPlayer.awardStat(Stats.ITEM_USED.get(this));
                    ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, pPlayer, getEmptySuccessItem(itemstack, pPlayer));
                    return InteractionResultHolder.sidedSuccess(itemstack1, pLevel.isClientSide());
                } else {
                    return InteractionResultHolder.fail(itemstack);
                }
            }
        }
    }

    public static ItemStack getEmptySuccessItem(ItemStack pBucketStack, Player pPlayer) {
        return !pPlayer.hasInfiniteMaterials() ? new ItemStack(Items.BUCKET) : pBucketStack;
    }

    @Override
    public void checkExtraContent(@Nullable Player pPlayer, Level pLevel, ItemStack pContainerStack, BlockPos pPos) {
    }

    @Override
    @Deprecated // Neo: use the ItemStack sensitive version
    public boolean emptyContents(@Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult) {
        return this.emptyContents(pPlayer, pLevel, pPos, pResult, null);
    }
    public boolean emptyContents(@Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult, @Nullable ItemStack container) {
        if (!(this.content instanceof FlowingFluid flowingfluid)) {
            return false;
        } else {
            Block $$7;
            boolean $$8;
            BlockState blockstate;
            boolean flag2;
            label82: {
                blockstate = pLevel.getBlockState(pPos);
                $$7 = blockstate.getBlock();
                $$8 = blockstate.canBeReplaced(this.content);
                label70:
                if (!blockstate.isAir() && !$$8) {
                    if ($$7 instanceof LiquidBlockContainer liquidblockcontainer
                        && liquidblockcontainer.canPlaceLiquid(pPlayer, pLevel, pPos, blockstate, this.content)) {
                        break label70;
                    }

                    flag2 = false;
                    break label82;
                }

                flag2 = true;
            }

            boolean flag1 = flag2;
            java.util.Optional<net.neoforged.neoforge.fluids.FluidStack> containedFluidStack = java.util.Optional.ofNullable(container).flatMap(net.neoforged.neoforge.fluids.FluidUtil::getFluidContained);
            if (!flag1) {
                return pResult != null && this.emptyContents(pPlayer, pLevel, pResult.getBlockPos().relative(pResult.getDirection()), null, container);
            } else if (containedFluidStack.isPresent() && this.content.getFluidType().isVaporizedOnPlacement(pLevel, pPos, containedFluidStack.get())) {
                this.content.getFluidType().onVaporize(pPlayer, pLevel, pPos, containedFluidStack.get());
                return true;
            } else if (pLevel.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
                int l = pPos.getX();
                int i = pPos.getY();
                int j = pPos.getZ();
                pLevel.playSound(
                    pPlayer,
                    pPos,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.5F,
                    2.6F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.8F
                );

                for (int k = 0; k < 8; k++) {
                    pLevel.addParticle(
                        ParticleTypes.LARGE_SMOKE, (double)l + Math.random(), (double)i + Math.random(), (double)j + Math.random(), 0.0, 0.0, 0.0
                    );
                }

                return true;
            } else {
                if ($$7 instanceof LiquidBlockContainer liquidblockcontainer1 && liquidblockcontainer1.canPlaceLiquid(pPlayer, pLevel, pPos, blockstate,content)) {
                    liquidblockcontainer1.placeLiquid(pLevel, pPos, blockstate, flowingfluid.getSource(false));
                    this.playEmptySound(pPlayer, pLevel, pPos);
                    return true;
                }

                if (!pLevel.isClientSide && $$8 && !blockstate.liquid()) {
                    pLevel.destroyBlock(pPos, true);
                }

                if (!pLevel.setBlock(pPos, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
                    return false;
                } else {
                    this.playEmptySound(pPlayer, pLevel, pPos);
                    return true;
                }
            }
        }
    }

    protected void playEmptySound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos) {
        SoundEvent soundevent = this.content.getFluidType().getSound(pPlayer, pLevel, pPos, net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY);
        if(soundevent == null) soundevent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        pLevel.playSound(pPlayer, pPos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
        pLevel.gameEvent(pPlayer, GameEvent.FLUID_PLACE, pPos);
    }

    protected boolean canBlockContainFluid(@Nullable Player player, Level worldIn, BlockPos posIn, BlockState blockstate)
    {
        return blockstate.getBlock() instanceof LiquidBlockContainer && ((LiquidBlockContainer)blockstate.getBlock()).canPlaceLiquid(player, worldIn, posIn, blockstate, this.content);
    }
}

package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BrushItem extends Item {
    public static final int ANIMATION_DURATION = 10;
    private static final int USE_DURATION = 200;

    public BrushItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();
        if (player != null && this.calculateHitResult(player).getType() == HitResult.Type.BLOCK) {
            player.startUsingItem(pContext.getHand());
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BRUSH;
    }

    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity p_344739_) {
        return 200;
    }

    /**
     * Called as the item is being used by an entity.
     */
    @Override
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
        if (pRemainingUseDuration >= 0 && pLivingEntity instanceof Player player) {
            HitResult hitresult = this.calculateHitResult(player);
            if (hitresult instanceof BlockHitResult blockhitresult && hitresult.getType() == HitResult.Type.BLOCK) {
                int i = this.getUseDuration(pStack, pLivingEntity) - pRemainingUseDuration + 1;
                boolean flag = i % 10 == 5;
                if (flag) {
                    BlockPos blockpos = blockhitresult.getBlockPos();
                    BlockState blockstate = pLevel.getBlockState(blockpos);
                    HumanoidArm humanoidarm = pLivingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND
                        ? player.getMainArm()
                        : player.getMainArm().getOpposite();
                    if (blockstate.shouldSpawnTerrainParticles() && blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                        this.spawnDustParticles(pLevel, blockhitresult, blockstate, pLivingEntity.getViewVector(0.0F), humanoidarm);
                    }

                    SoundEvent soundevent;
                    if (blockstate.getBlock() instanceof BrushableBlock brushableblock) {
                        soundevent = brushableblock.getBrushSound();
                    } else {
                        soundevent = SoundEvents.BRUSH_GENERIC;
                    }

                    pLevel.playSound(player, blockpos, soundevent, SoundSource.BLOCKS);
                    if (!pLevel.isClientSide() && pLevel.getBlockEntity(blockpos) instanceof BrushableBlockEntity brushableblockentity) {
                        boolean flag1 = brushableblockentity.brush(pLevel.getGameTime(), player, blockhitresult.getDirection());
                        if (flag1) {
                            EquipmentSlot equipmentslot = pStack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND))
                                ? EquipmentSlot.OFFHAND
                                : EquipmentSlot.MAINHAND;
                            pStack.hurtAndBreak(1, pLivingEntity, equipmentslot);
                        }
                    }
                }

                return;
            }

            pLivingEntity.releaseUsingItem();
        } else {
            pLivingEntity.releaseUsingItem();
        }
    }

    private HitResult calculateHitResult(Player pPlayer) {
        return ProjectileUtil.getHitResultOnViewVector(
            pPlayer, p_281111_ -> !p_281111_.isSpectator() && p_281111_.isPickable(), pPlayer.blockInteractionRange()
        );
    }

    private void spawnDustParticles(Level pLevel, BlockHitResult pHitResult, BlockState pState, Vec3 pPos, HumanoidArm pArm) {
        double d0 = 3.0;
        int i = pArm == HumanoidArm.RIGHT ? 1 : -1;
        int j = pLevel.getRandom().nextInt(7, 12);
        BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.BLOCK, pState);
        Direction direction = pHitResult.getDirection();
        BrushItem.DustParticlesDelta brushitem$dustparticlesdelta = BrushItem.DustParticlesDelta.fromDirection(pPos, direction);
        Vec3 vec3 = pHitResult.getLocation();

        for (int k = 0; k < j; k++) {
            pLevel.addParticle(
                blockparticleoption,
                vec3.x - (double)(direction == Direction.WEST ? 1.0E-6F : 0.0F),
                vec3.y,
                vec3.z - (double)(direction == Direction.NORTH ? 1.0E-6F : 0.0F),
                brushitem$dustparticlesdelta.xd() * (double)i * 3.0 * pLevel.getRandom().nextDouble(),
                0.0,
                brushitem$dustparticlesdelta.zd() * (double)i * 3.0 * pLevel.getRandom().nextDouble()
            );
        }
    }

    static record DustParticlesDelta(double xd, double yd, double zd) {
        private static final double ALONG_SIDE_DELTA = 1.0;
        private static final double OUT_FROM_SIDE_DELTA = 0.1;

        public static BrushItem.DustParticlesDelta fromDirection(Vec3 pPos, Direction pDirection) {
            double d0 = 0.0;

            return switch (pDirection) {
                case DOWN, UP -> new BrushItem.DustParticlesDelta(pPos.z(), 0.0, -pPos.x());
                case NORTH -> new BrushItem.DustParticlesDelta(1.0, 0.0, -0.1);
                case SOUTH -> new BrushItem.DustParticlesDelta(-1.0, 0.0, 0.1);
                case WEST -> new BrushItem.DustParticlesDelta(-0.1, 0.0, -1.0);
                case EAST -> new BrushItem.DustParticlesDelta(0.1, 0.0, 1.0);
            };
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_BRUSH_ACTIONS.contains(itemAbility);
    }
}

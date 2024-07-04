package net.minecraft.world.effect;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

class WeavingMobEffect extends MobEffect {
    private final ToIntFunction<RandomSource> maxCobwebs;

    protected WeavingMobEffect(MobEffectCategory pCategory, int pColor, ToIntFunction<RandomSource> pMaxCobwebs) {
        super(pCategory, pColor, ParticleTypes.ITEM_COBWEB);
        this.maxCobwebs = pMaxCobwebs;
    }

    @Override
    public void onMobRemoved(LivingEntity pLivingEntity, int pAmplifier, Entity.RemovalReason pReason) {
        if (pReason == Entity.RemovalReason.KILLED
            && (pLivingEntity instanceof Player || pLivingEntity.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))) {
            this.spawnCobwebsRandomlyAround(pLivingEntity.level(), pLivingEntity.getRandom(), pLivingEntity.getOnPos());
        }
    }

    private void spawnCobwebsRandomlyAround(Level pLevel, RandomSource pRandom, BlockPos pPos) {
        Set<BlockPos> set = Sets.newHashSet();
        int i = this.maxCobwebs.applyAsInt(pRandom);

        for (BlockPos blockpos : BlockPos.randomInCube(pRandom, 15, pPos, 1)) {
            BlockPos blockpos1 = blockpos.below();
            if (!set.contains(blockpos)
                && pLevel.getBlockState(blockpos).canBeReplaced()
                && pLevel.getBlockState(blockpos1).isFaceSturdy(pLevel, blockpos1, Direction.UP)) {
                set.add(blockpos.immutable());
                if (set.size() >= i) {
                    break;
                }
            }
        }

        for (BlockPos blockpos2 : set) {
            pLevel.setBlock(blockpos2, Blocks.COBWEB.defaultBlockState(), 3);
            pLevel.levelEvent(3018, blockpos2, 0);
        }
    }
}

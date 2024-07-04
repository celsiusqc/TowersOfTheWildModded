package net.minecraft.world.effect;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

class OozingMobEffect extends MobEffect {
    private static final int RADIUS_TO_CHECK_SLIMES = 2;
    public static final int SLIME_SIZE = 2;
    private final ToIntFunction<RandomSource> spawnedCount;

    protected OozingMobEffect(MobEffectCategory pCategory, int pColor, ToIntFunction<RandomSource> pSpawnedCount) {
        super(pCategory, pColor, ParticleTypes.ITEM_SLIME);
        this.spawnedCount = pSpawnedCount;
    }

    @VisibleForTesting
    protected static int numberOfSlimesToSpawn(int p_341016_, OozingMobEffect.NearbySlimes p_348591_, int p_341398_) {
        return p_341016_ < 1 ? p_341398_ : Mth.clamp(0, p_341016_ - p_348591_.count(p_341016_), p_341398_);
    }

    @Override
    public void onMobRemoved(LivingEntity pLivingEntity, int pAmplifier, Entity.RemovalReason pReason) {
        if (pReason == Entity.RemovalReason.KILLED) {
            int i = this.spawnedCount.applyAsInt(pLivingEntity.getRandom());
            Level level = pLivingEntity.level();
            int j = level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            int k = numberOfSlimesToSpawn(j, OozingMobEffect.NearbySlimes.closeTo(pLivingEntity), i);

            for (int l = 0; l < k; l++) {
                this.spawnSlimeOffspring(pLivingEntity.level(), pLivingEntity.getX(), pLivingEntity.getY() + 0.5, pLivingEntity.getZ());
            }
        }
    }

    private void spawnSlimeOffspring(Level pLevel, double pX, double pY, double pZ) {
        Slime slime = EntityType.SLIME.create(pLevel);
        if (slime != null) {
            slime.setSize(2, true);
            slime.moveTo(pX, pY, pZ, pLevel.getRandom().nextFloat() * 360.0F, 0.0F);
            pLevel.addFreshEntity(slime);
        }
    }

    @FunctionalInterface
    protected interface NearbySlimes {
        int count(int p_348627_);

        static OozingMobEffect.NearbySlimes closeTo(LivingEntity p_348471_) {
            return p_352702_ -> {
                List<Slime> list = new ArrayList<>();
                p_348471_.level().getEntities(EntityType.SLIME, p_348471_.getBoundingBox().inflate(2.0), p_348647_ -> p_348647_ != p_348471_, list, p_352702_);
                return list.size();
            };
        }
    }
}

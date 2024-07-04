package net.minecraft.world.effect;

import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

class InfestedMobEffect extends MobEffect {
    private final float chanceToSpawn;
    private final ToIntFunction<RandomSource> spawnedCount;

    protected InfestedMobEffect(MobEffectCategory pCategory, int pColor, float pChanceToSpawn, ToIntFunction<RandomSource> pSpawnedCount) {
        super(pCategory, pColor, ParticleTypes.INFESTED);
        this.chanceToSpawn = pChanceToSpawn;
        this.spawnedCount = pSpawnedCount;
    }

    @Override
    public void onMobHurt(LivingEntity pLivingEntity, int pAmplifier, DamageSource pDamageSource, float pAmount) {
        if (pLivingEntity.getRandom().nextFloat() <= this.chanceToSpawn) {
            int i = this.spawnedCount.applyAsInt(pLivingEntity.getRandom());

            for (int j = 0; j < i; j++) {
                this.spawnSilverfish(pLivingEntity.level(), pLivingEntity, pLivingEntity.getX(), pLivingEntity.getY() + (double)pLivingEntity.getBbHeight() / 2.0, pLivingEntity.getZ());
            }
        }
    }

    private void spawnSilverfish(Level pLevel, LivingEntity pEntity, double pX, double pY, double pZ) {
        Silverfish silverfish = EntityType.SILVERFISH.create(pLevel);
        if (silverfish != null) {
            RandomSource randomsource = pEntity.getRandom();
            float f = (float) (Math.PI / 2);
            float f1 = Mth.randomBetween(randomsource, (float) (-Math.PI / 2), (float) (Math.PI / 2));
            Vector3f vector3f = pEntity.getLookAngle().toVector3f().mul(0.3F).mul(1.0F, 1.5F, 1.0F).rotateY(f1);
            silverfish.moveTo(pX, pY, pZ, pLevel.getRandom().nextFloat() * 360.0F, 0.0F);
            silverfish.setDeltaMovement(new Vec3(vector3f));
            pLevel.addFreshEntity(silverfish);
            silverfish.playSound(SoundEvents.SILVERFISH_HURT);
        }
    }
}

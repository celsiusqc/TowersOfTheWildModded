package net.minecraft.world.effect;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

class HealOrHarmMobEffect extends InstantenousMobEffect {
    private final boolean isHarm;

    public HealOrHarmMobEffect(MobEffectCategory pCategory, int pColor, boolean pIsHarm) {
        super(pCategory, pColor);
        this.isHarm = pIsHarm;
    }

    @Override
    public boolean applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (this.isHarm == pLivingEntity.isInvertedHealAndHarm()) {
            pLivingEntity.heal((float)Math.max(4 << pAmplifier, 0));
        } else {
            pLivingEntity.hurt(pLivingEntity.damageSources().magic(), (float)(6 << pAmplifier));
        }

        return true;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity pSource, @Nullable Entity pIndirectSource, LivingEntity pLivingEntity, int pAmplifier, double pHealth) {
        if (this.isHarm == pLivingEntity.isInvertedHealAndHarm()) {
            int i = (int)(pHealth * (double)(4 << pAmplifier) + 0.5);
            pLivingEntity.heal((float)i);
        } else {
            int j = (int)(pHealth * (double)(6 << pAmplifier) + 0.5);
            if (pSource == null) {
                pLivingEntity.hurt(pLivingEntity.damageSources().magic(), (float)j);
            } else {
                pLivingEntity.hurt(pLivingEntity.damageSources().indirectMagic(pSource, pIndirectSource), (float)j);
            }
        }
    }
}

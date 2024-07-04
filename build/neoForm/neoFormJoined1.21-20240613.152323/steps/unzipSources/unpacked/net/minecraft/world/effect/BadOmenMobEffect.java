package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

class BadOmenMobEffect extends MobEffect {
    protected BadOmenMobEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (pLivingEntity instanceof ServerPlayer serverplayer && !serverplayer.isSpectator()) {
            ServerLevel serverlevel = serverplayer.serverLevel();
            if (serverlevel.getDifficulty() != Difficulty.PEACEFUL && serverlevel.isVillage(serverplayer.blockPosition())) {
                Raid raid = serverlevel.getRaidAt(serverplayer.blockPosition());
                if (raid == null || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel()) {
                    serverplayer.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, pAmplifier));
                    serverplayer.setRaidOmenPosition(serverplayer.blockPosition());
                    return false;
                }
            }
        }

        return true;
    }
}

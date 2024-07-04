package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;

public class Swim extends Behavior<Mob> {
    private final float chance;

    public Swim(float pChance) {
        super(ImmutableMap.of());
        this.chance = pChance;
    }

    public static boolean shouldSwim(Mob pMob) {
        return pMob.isInWater() && pMob.getFluidHeight(FluidTags.WATER) > pMob.getFluidJumpThreshold() || pMob.isInLava() || pMob.isInFluidType((fluidType, height) -> pMob.canSwimInFluidType(fluidType) && height > pMob.getFluidJumpThreshold());
    }

    protected boolean checkExtraStartConditions(ServerLevel pLevel, Mob pOwner) {
        return shouldSwim(pOwner);
    }

    protected boolean canStillUse(ServerLevel pLevel, Mob pEntity, long pGameTime) {
        return this.checkExtraStartConditions(pLevel, pEntity);
    }

    protected void tick(ServerLevel pLevel, Mob pOwner, long pGameTime) {
        if (pOwner.getRandom().nextFloat() < this.chance) {
            pOwner.getJumpControl().jump();
        }
    }
}

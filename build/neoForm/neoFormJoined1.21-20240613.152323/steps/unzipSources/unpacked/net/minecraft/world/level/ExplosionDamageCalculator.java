package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class ExplosionDamageCalculator {
    public Optional<Float> getBlockExplosionResistance(Explosion pExplosion, BlockGetter pReader, BlockPos pPos, BlockState pState, FluidState pFluid) {
        return pState.isAir() && pFluid.isEmpty()
            ? Optional.empty()
            : Optional.of(Math.max(pState.getExplosionResistance(pReader, pPos, pExplosion), pFluid.getExplosionResistance(pReader, pPos, pExplosion)));
    }

    public boolean shouldBlockExplode(Explosion pExplosion, BlockGetter pReader, BlockPos pPos, BlockState pState, float pPower) {
        return true;
    }

    public boolean shouldDamageEntity(Explosion pExplosion, Entity pEntity) {
        return true;
    }

    public float getKnockbackMultiplier(Entity pEntity) {
        return 1.0F;
    }

    public float getEntityDamageAmount(Explosion pExplosion, Entity pEntity) {
        float f = pExplosion.radius() * 2.0F;
        Vec3 vec3 = pExplosion.center();
        double d0 = Math.sqrt(pEntity.distanceToSqr(vec3)) / (double)f;
        double d1 = (1.0 - d0) * (double)Explosion.getSeenPercent(vec3, pEntity);
        return (float)((d1 * d1 + d1) / 2.0 * 7.0 * (double)f + 1.0);
    }
}

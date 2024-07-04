package net.minecraft.client.renderer.item;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompassItemPropertyFunction implements ClampedItemPropertyFunction {
    public static final int DEFAULT_ROTATION = 0;
    private final CompassItemPropertyFunction.CompassWobble wobble = new CompassItemPropertyFunction.CompassWobble();
    private final CompassItemPropertyFunction.CompassWobble wobbleRandom = new CompassItemPropertyFunction.CompassWobble();
    public final CompassItemPropertyFunction.CompassTarget compassTarget;

    public CompassItemPropertyFunction(CompassItemPropertyFunction.CompassTarget pCompassTarget) {
        this.compassTarget = pCompassTarget;
    }

    @Override
    public float unclampedCall(ItemStack pStack, @Nullable ClientLevel pLevel, @Nullable LivingEntity pEntity, int pSeed) {
        Entity entity = (Entity)(pEntity != null ? pEntity : pStack.getEntityRepresentation());
        if (entity == null) {
            return 0.0F;
        } else {
            pLevel = this.tryFetchLevelIfMissing(entity, pLevel);
            return pLevel == null ? 0.0F : this.getCompassRotation(pStack, pLevel, pSeed, entity);
        }
    }

    private float getCompassRotation(ItemStack pStack, ClientLevel pLevel, int pSeed, Entity pEntity) {
        GlobalPos globalpos = this.compassTarget.getPos(pLevel, pStack, pEntity);
        long i = pLevel.getGameTime();
        return !this.isValidCompassTargetPos(pEntity, globalpos)
            ? this.getRandomlySpinningRotation(pSeed, i)
            : this.getRotationTowardsCompassTarget(pEntity, i, globalpos.pos());
    }

    private float getRandomlySpinningRotation(int pSeed, long pTicks) {
        if (this.wobbleRandom.shouldUpdate(pTicks)) {
            this.wobbleRandom.update(pTicks, Math.random());
        }

        double d0 = this.wobbleRandom.rotation + (double)((float)this.hash(pSeed) / 2.1474836E9F);
        return Mth.positiveModulo((float)d0, 1.0F);
    }

    private float getRotationTowardsCompassTarget(Entity pEntity, long pTicks, BlockPos pPos) {
        double d0 = this.getAngleFromEntityToPos(pEntity, pPos);
        double d1 = this.getWrappedVisualRotationY(pEntity);
        if (pEntity instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
            if (this.wobble.shouldUpdate(pTicks)) {
                this.wobble.update(pTicks, 0.5 - (d1 - 0.25));
            }

            double d3 = d0 + this.wobble.rotation;
            return Mth.positiveModulo((float)d3, 1.0F);
        }

        double d2 = 0.5 - (d1 - 0.25 - d0);
        return Mth.positiveModulo((float)d2, 1.0F);
    }

    @Nullable
    private ClientLevel tryFetchLevelIfMissing(Entity pEntity, @Nullable ClientLevel pLevel) {
        return pLevel == null && pEntity.level() instanceof ClientLevel ? (ClientLevel)pEntity.level() : pLevel;
    }

    private boolean isValidCompassTargetPos(Entity pEntity, @Nullable GlobalPos pPos) {
        return pPos != null
            && pPos.dimension() == pEntity.level().dimension()
            && !(pPos.pos().distToCenterSqr(pEntity.position()) < 1.0E-5F);
    }

    private double getAngleFromEntityToPos(Entity pEntity, BlockPos pPos) {
        Vec3 vec3 = Vec3.atCenterOf(pPos);
        return Math.atan2(vec3.z() - pEntity.getZ(), vec3.x() - pEntity.getX()) / (float) (Math.PI * 2);
    }

    private double getWrappedVisualRotationY(Entity pEntity) {
        return Mth.positiveModulo((double)(pEntity.getVisualRotationYInDegrees() / 360.0F), 1.0);
    }

    private int hash(int pValue) {
        return pValue * 1327217883;
    }

    @OnlyIn(Dist.CLIENT)
    public interface CompassTarget {
        @Nullable
        GlobalPos getPos(ClientLevel pLevel, ItemStack pStack, Entity pEntity);
    }

    @OnlyIn(Dist.CLIENT)
    static class CompassWobble {
        double rotation;
        private double deltaRotation;
        private long lastUpdateTick;

        boolean shouldUpdate(long pTicks) {
            return this.lastUpdateTick != pTicks;
        }

        void update(long pTicks, double pRotation) {
            this.lastUpdateTick = pTicks;
            double d0 = pRotation - this.rotation;
            d0 = Mth.positiveModulo(d0 + 0.5, 1.0) - 0.5;
            this.deltaRotation += d0 * 0.1;
            this.deltaRotation *= 0.8;
            this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0);
        }
    }
}

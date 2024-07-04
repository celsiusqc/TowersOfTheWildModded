package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge;
import net.minecraft.world.phys.Vec3;

public class Shoot extends Behavior<Breeze> {
    private static final int ATTACK_RANGE_MIN_SQRT = 4;
    private static final int ATTACK_RANGE_MAX_SQRT = 256;
    private static final int UNCERTAINTY_BASE = 5;
    private static final int UNCERTAINTY_MULTIPLIER = 4;
    private static final float PROJECTILE_MOVEMENT_SCALE = 0.7F;
    private static final int SHOOT_INITIAL_DELAY_TICKS = Math.round(15.0F);
    private static final int SHOOT_RECOVER_DELAY_TICKS = Math.round(4.0F);
    private static final int SHOOT_COOLDOWN_TICKS = Math.round(10.0F);

    @VisibleForTesting
    public Shoot() {
        super(
            ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_SHOOT_CHARGING,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_SHOOT_RECOVERING,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_SHOOT,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_JUMP_TARGET,
                MemoryStatus.VALUE_ABSENT
            ),
            SHOOT_INITIAL_DELAY_TICKS + 1 + SHOOT_RECOVER_DELAY_TICKS
        );
    }

    protected boolean checkExtraStartConditions(ServerLevel pLevel, Breeze pOwner) {
        return pOwner.getPose() != Pose.STANDING
            ? false
            : pOwner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(p_312632_ -> isTargetWithinRange(pOwner, p_312632_)).map(p_312737_ -> {
                if (!p_312737_) {
                    pOwner.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
                }

                return (Boolean)p_312737_;
            }).orElse(false);
    }

    protected boolean canStillUse(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
        return pEntity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && pEntity.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_SHOOT);
    }

    protected void start(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
        pEntity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(p_312833_ -> pEntity.setPose(Pose.SHOOTING));
        pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_CHARGING, Unit.INSTANCE, (long)SHOOT_INITIAL_DELAY_TICKS);
        pEntity.playSound(SoundEvents.BREEZE_INHALE, 1.0F, 1.0F);
    }

    protected void stop(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
        if (pEntity.getPose() == Pose.SHOOTING) {
            pEntity.setPose(Pose.STANDING);
        }

        pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, (long)SHOOT_COOLDOWN_TICKS);
        pEntity.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
    }

    protected void tick(ServerLevel pLevel, Breeze pOwner, long pGameTime) {
        Brain<Breeze> brain = pOwner.getBrain();
        LivingEntity livingentity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (livingentity != null) {
            pOwner.lookAt(EntityAnchorArgument.Anchor.EYES, livingentity.position());
            if (!brain.getMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING).isPresent() && !brain.getMemory(MemoryModuleType.BREEZE_SHOOT_RECOVERING).isPresent()) {
                brain.setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_RECOVERING, Unit.INSTANCE, (long)SHOOT_RECOVER_DELAY_TICKS);
                if (isFacingTarget(pOwner, livingentity)) {
                    double d0 = livingentity.getX() - pOwner.getX();
                    double d1 = livingentity.getY(livingentity.isPassenger() ? 0.8 : 0.3) - pOwner.getY(0.5);
                    double d2 = livingentity.getZ() - pOwner.getZ();
                    BreezeWindCharge breezewindcharge = new BreezeWindCharge(pOwner, pLevel);
                    pOwner.playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.0F);
                    breezewindcharge.shoot(d0, d1, d2, 0.7F, (float)(5 - pLevel.getDifficulty().getId() * 4));
                    pLevel.addFreshEntity(breezewindcharge);
                }
            }
        }
    }

    @VisibleForTesting
    public static boolean isFacingTarget(Breeze pBreeze, LivingEntity pTarget) {
        Vec3 vec3 = pBreeze.getViewVector(1.0F);
        Vec3 vec31 = pTarget.position().subtract(pBreeze.position()).normalize();
        return vec3.dot(vec31) > 0.5;
    }

    private static boolean isTargetWithinRange(Breeze pBreeze, LivingEntity pTarget) {
        double d0 = pBreeze.position().distanceToSqr(pTarget.position());
        return d0 > 4.0 && d0 < 256.0;
    }
}

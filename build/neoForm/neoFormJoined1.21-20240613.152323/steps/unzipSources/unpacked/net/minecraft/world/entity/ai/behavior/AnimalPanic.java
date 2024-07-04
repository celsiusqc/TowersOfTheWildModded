package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public class AnimalPanic<E extends PathfinderMob> extends Behavior<E> {
    private static final int PANIC_MIN_DURATION = 100;
    private static final int PANIC_MAX_DURATION = 120;
    private static final int PANIC_DISTANCE_HORIZONTAL = 5;
    private static final int PANIC_DISTANCE_VERTICAL = 4;
    private final float speedMultiplier;
    private final Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes;

    public AnimalPanic(float pSpeedMultiplier) {
        this(pSpeedMultiplier, p_349992_ -> DamageTypeTags.PANIC_CAUSES);
    }

    public AnimalPanic(float pSpeedMultiplier, Function<PathfinderMob, TagKey<DamageType>> p_350957_) {
        super(Map.of(MemoryModuleType.IS_PANICKING, MemoryStatus.REGISTERED, MemoryModuleType.HURT_BY, MemoryStatus.REGISTERED), 100, 120);
        this.speedMultiplier = pSpeedMultiplier;
        this.panicCausingDamageTypes = p_350957_;
    }

    protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
        return pOwner.getBrain()
                .getMemory(MemoryModuleType.HURT_BY)
                .map(p_349994_ -> p_349994_.is(this.panicCausingDamageTypes.apply(pOwner)))
                .orElse(false)
            || pOwner.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
    }

    protected boolean canStillUse(ServerLevel pLevel, E pEntity, long pGameTime) {
        return true;
    }

    protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
        pEntity.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);
        pEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    protected void stop(ServerLevel pLevel, E pEntity, long pGameTime) {
        Brain<?> brain = pEntity.getBrain();
        brain.eraseMemory(MemoryModuleType.IS_PANICKING);
    }

    protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
        if (pOwner.getNavigation().isDone()) {
            Vec3 vec3 = this.getPanicPos(pOwner, pLevel);
            if (vec3 != null) {
                pOwner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, this.speedMultiplier, 0));
            }
        }
    }

    @Nullable
    private Vec3 getPanicPos(E pPathfinder, ServerLevel pLevel) {
        if (pPathfinder.isOnFire()) {
            Optional<Vec3> optional = this.lookForWater(pLevel, pPathfinder).map(Vec3::atBottomCenterOf);
            if (optional.isPresent()) {
                return optional.get();
            }
        }

        return LandRandomPos.getPos(pPathfinder, 5, 4);
    }

    private Optional<BlockPos> lookForWater(BlockGetter pLevel, Entity pEntity) {
        BlockPos blockpos = pEntity.blockPosition();
        if (!pLevel.getBlockState(blockpos).getCollisionShape(pLevel, blockpos).isEmpty()) {
            return Optional.empty();
        } else {
            Predicate<BlockPos> predicate;
            if (Mth.ceil(pEntity.getBbWidth()) == 2) {
                predicate = p_284705_ -> BlockPos.squareOutSouthEast(p_284705_).allMatch(p_196646_ -> pLevel.getFluidState(p_196646_).is(FluidTags.WATER));
            } else {
                predicate = p_284707_ -> pLevel.getFluidState(p_284707_).is(FluidTags.WATER);
            }

            return BlockPos.findClosestMatch(blockpos, 5, 1, predicate);
        }
    }
}

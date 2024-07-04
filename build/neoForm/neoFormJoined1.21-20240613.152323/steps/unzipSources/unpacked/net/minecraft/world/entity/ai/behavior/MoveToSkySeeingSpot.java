package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MoveToSkySeeingSpot {
    public static OneShot<LivingEntity> create(float pSpeedModifier) {
        return BehaviorBuilder.create(
            p_258543_ -> p_258543_.group(p_258543_.absent(MemoryModuleType.WALK_TARGET)).apply(p_258543_, p_258545_ -> (p_352756_, p_352757_, p_352758_) -> {
                        if (p_352756_.canSeeSky(p_352757_.blockPosition())) {
                            return false;
                        } else {
                            Optional<Vec3> optional = Optional.ofNullable(getOutdoorPosition(p_352756_, p_352757_));
                            optional.ifPresent(p_258548_ -> p_258545_.set(new WalkTarget(p_258548_, pSpeedModifier, 0)));
                            return true;
                        }
                    })
        );
    }

    @Nullable
    private static Vec3 getOutdoorPosition(ServerLevel pLevel, LivingEntity pEntity) {
        RandomSource randomsource = pEntity.getRandom();
        BlockPos blockpos = pEntity.blockPosition();

        for (int i = 0; i < 10; i++) {
            BlockPos blockpos1 = blockpos.offset(randomsource.nextInt(20) - 10, randomsource.nextInt(6) - 3, randomsource.nextInt(20) - 10);
            if (hasNoBlocksAbove(pLevel, pEntity, blockpos1)) {
                return Vec3.atBottomCenterOf(blockpos1);
            }
        }

        return null;
    }

    public static boolean hasNoBlocksAbove(ServerLevel pLevel, LivingEntity pEntity, BlockPos pPos) {
        return pLevel.canSeeSky(pPos) && (double)pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pPos).getY() <= pEntity.getY();
    }
}

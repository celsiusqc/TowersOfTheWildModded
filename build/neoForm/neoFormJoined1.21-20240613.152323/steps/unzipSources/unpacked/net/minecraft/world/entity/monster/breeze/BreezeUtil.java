package net.minecraft.world.entity.monster.breeze;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BreezeUtil {
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 50.0;

    public static Vec3 randomPointBehindTarget(LivingEntity pTarget, RandomSource pRandom) {
        int i = 90;
        float f = pTarget.yHeadRot + 180.0F + (float)pRandom.nextGaussian() * 90.0F / 2.0F;
        float f1 = Mth.lerp(pRandom.nextFloat(), 4.0F, 8.0F);
        Vec3 vec3 = Vec3.directionFromRotation(0.0F, f).scale((double)f1);
        return pTarget.position().add(vec3);
    }

    public static boolean hasLineOfSight(Breeze pBreeze, Vec3 pPos) {
        Vec3 vec3 = new Vec3(pBreeze.getX(), pBreeze.getY(), pBreeze.getZ());
        return pPos.distanceTo(vec3) > 50.0
            ? false
            : pBreeze.level().clip(new ClipContext(vec3, pPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pBreeze)).getType()
                == HitResult.Type.MISS;
    }
}

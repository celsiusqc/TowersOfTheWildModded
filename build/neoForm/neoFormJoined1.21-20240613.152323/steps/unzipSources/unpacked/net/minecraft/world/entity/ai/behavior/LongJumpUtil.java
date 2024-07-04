package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class LongJumpUtil {
    public static Optional<Vec3> calculateJumpVectorForAngle(Mob pMob, Vec3 pTarget, float pMaxJumpVelocity, int pAngle, boolean pRequireClearTransition) {
        Vec3 vec3 = pMob.position();
        Vec3 vec31 = new Vec3(pTarget.x - vec3.x, 0.0, pTarget.z - vec3.z).normalize().scale(0.5);
        Vec3 vec32 = pTarget.subtract(vec31);
        Vec3 vec33 = vec32.subtract(vec3);
        float f = (float)pAngle * (float) Math.PI / 180.0F;
        double d0 = Math.atan2(vec33.z, vec33.x);
        double d1 = vec33.subtract(0.0, vec33.y, 0.0).lengthSqr();
        double d2 = Math.sqrt(d1);
        double d3 = vec33.y;
        double d4 = pMob.getGravity();
        double d5 = Math.sin((double)(2.0F * f));
        double d6 = Math.pow(Math.cos((double)f), 2.0);
        double d7 = Math.sin((double)f);
        double d8 = Math.cos((double)f);
        double d9 = Math.sin(d0);
        double d10 = Math.cos(d0);
        double d11 = d1 * d4 / (d2 * d5 - 2.0 * d3 * d6);
        if (d11 < 0.0) {
            return Optional.empty();
        } else {
            double d12 = Math.sqrt(d11);
            if (d12 > (double)pMaxJumpVelocity) {
                return Optional.empty();
            } else {
                double d13 = d12 * d8;
                double d14 = d12 * d7;
                if (pRequireClearTransition) {
                    int i = Mth.ceil(d2 / d13) * 2;
                    double d15 = 0.0;
                    Vec3 vec34 = null;
                    EntityDimensions entitydimensions = pMob.getDimensions(Pose.LONG_JUMPING);

                    for (int j = 0; j < i - 1; j++) {
                        d15 += d2 / (double)i;
                        double d16 = d7 / d8 * d15 - Math.pow(d15, 2.0) * d4 / (2.0 * d11 * Math.pow(d8, 2.0));
                        double d17 = d15 * d10;
                        double d18 = d15 * d9;
                        Vec3 vec35 = new Vec3(vec3.x + d17, vec3.y + d16, vec3.z + d18);
                        if (vec34 != null && !isClearTransition(pMob, entitydimensions, vec34, vec35)) {
                            return Optional.empty();
                        }

                        vec34 = vec35;
                    }
                }

                return Optional.of(new Vec3(d13 * d10, d14, d13 * d9).scale(0.95F));
            }
        }
    }

    private static boolean isClearTransition(Mob pMob, EntityDimensions pDimensions, Vec3 pStartPos, Vec3 pEndPos) {
        Vec3 vec3 = pEndPos.subtract(pStartPos);
        double d0 = (double)Math.min(pDimensions.width(), pDimensions.height());
        int i = Mth.ceil(vec3.length() / d0);
        Vec3 vec31 = vec3.normalize();
        Vec3 vec32 = pStartPos;

        for (int j = 0; j < i; j++) {
            vec32 = j == i - 1 ? pEndPos : vec32.add(vec31.scale(d0 * 0.9F));
            if (!pMob.level().noCollision(pMob, pDimensions.makeBoundingBox(vec32))) {
                return false;
            }
        }

        return true;
    }
}

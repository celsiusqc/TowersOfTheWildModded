package net.minecraft.world.item;

import java.util.OptionalInt;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public interface ProjectileItem {
    Projectile asProjectile(Level pLevel, Position pPos, ItemStack pStack, Direction pDirection);

    default ProjectileItem.DispenseConfig createDispenseConfig() {
        return ProjectileItem.DispenseConfig.DEFAULT;
    }

    default void shoot(Projectile pProjectile, double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        pProjectile.shoot(pX, pY, pZ, pVelocity, pInaccuracy);
    }

    public static record DispenseConfig(ProjectileItem.PositionFunction positionFunction, float uncertainty, float power, OptionalInt overrideDispenseEvent) {
        public static final ProjectileItem.DispenseConfig DEFAULT = builder().build();

        public static ProjectileItem.DispenseConfig.Builder builder() {
            return new ProjectileItem.DispenseConfig.Builder();
        }

        public static class Builder {
            private ProjectileItem.PositionFunction positionFunction = (p_338429_, p_338348_) -> DispenserBlock.getDispensePosition(
                    p_338429_, 0.7, new Vec3(0.0, 0.1, 0.0)
                );
            private float uncertainty = 6.0F;
            private float power = 1.1F;
            private OptionalInt overrideDispenseEvent = OptionalInt.empty();

            public ProjectileItem.DispenseConfig.Builder positionFunction(ProjectileItem.PositionFunction pPositionFunction) {
                this.positionFunction = pPositionFunction;
                return this;
            }

            public ProjectileItem.DispenseConfig.Builder uncertainty(float pUncertainty) {
                this.uncertainty = pUncertainty;
                return this;
            }

            public ProjectileItem.DispenseConfig.Builder power(float pPower) {
                this.power = pPower;
                return this;
            }

            public ProjectileItem.DispenseConfig.Builder overrideDispenseEvent(int pOverrideDispenseEvent) {
                this.overrideDispenseEvent = OptionalInt.of(pOverrideDispenseEvent);
                return this;
            }

            public ProjectileItem.DispenseConfig build() {
                return new ProjectileItem.DispenseConfig(this.positionFunction, this.uncertainty, this.power, this.overrideDispenseEvent);
            }
        }
    }

    @FunctionalInterface
    public interface PositionFunction {
        Position getDispensePosition(BlockSource pSource, Direction pDirection);
    }
}

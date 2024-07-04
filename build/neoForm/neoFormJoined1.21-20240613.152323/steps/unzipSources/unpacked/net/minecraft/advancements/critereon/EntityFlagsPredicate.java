package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record EntityFlagsPredicate(
    Optional<Boolean> isOnGround,
    Optional<Boolean> isOnFire,
    Optional<Boolean> isCrouching,
    Optional<Boolean> isSprinting,
    Optional<Boolean> isSwimming,
    Optional<Boolean> isFlying,
    Optional<Boolean> isBaby
) {
    public static final Codec<EntityFlagsPredicate> CODEC = RecordCodecBuilder.create(
        p_344147_ -> p_344147_.group(
                    Codec.BOOL.optionalFieldOf("is_on_ground").forGetter(EntityFlagsPredicate::isOnGround),
                    Codec.BOOL.optionalFieldOf("is_on_fire").forGetter(EntityFlagsPredicate::isOnFire),
                    Codec.BOOL.optionalFieldOf("is_sneaking").forGetter(EntityFlagsPredicate::isCrouching),
                    Codec.BOOL.optionalFieldOf("is_sprinting").forGetter(EntityFlagsPredicate::isSprinting),
                    Codec.BOOL.optionalFieldOf("is_swimming").forGetter(EntityFlagsPredicate::isSwimming),
                    Codec.BOOL.optionalFieldOf("is_flying").forGetter(EntityFlagsPredicate::isFlying),
                    Codec.BOOL.optionalFieldOf("is_baby").forGetter(EntityFlagsPredicate::isBaby)
                )
                .apply(p_344147_, EntityFlagsPredicate::new)
    );

    public boolean matches(Entity pEntity) {
        if (this.isOnGround.isPresent() && pEntity.onGround() != this.isOnGround.get()) {
            return false;
        } else if (this.isOnFire.isPresent() && pEntity.isOnFire() != this.isOnFire.get()) {
            return false;
        } else if (this.isCrouching.isPresent() && pEntity.isCrouching() != this.isCrouching.get()) {
            return false;
        } else if (this.isSprinting.isPresent() && pEntity.isSprinting() != this.isSprinting.get()) {
            return false;
        } else if (this.isSwimming.isPresent() && pEntity.isSwimming() != this.isSwimming.get()) {
            return false;
        } else {
            if (this.isFlying.isPresent()) {
                boolean flag1;
                label53: {
                    if (pEntity instanceof LivingEntity livingentity
                        && (livingentity.isFallFlying() || livingentity instanceof Player player && player.getAbilities().flying)) {
                        flag1 = true;
                        break label53;
                    }

                    flag1 = false;
                }

                boolean flag = flag1;
                if (flag != this.isFlying.get()) {
                    return false;
                }
            }

            if (this.isBaby.isPresent() && pEntity instanceof LivingEntity livingentity1 && livingentity1.isBaby() != this.isBaby.get()) {
                return false;
            }

            return true;
        }
    }

    public static class Builder {
        private Optional<Boolean> isOnGround = Optional.empty();
        private Optional<Boolean> isOnFire = Optional.empty();
        private Optional<Boolean> isCrouching = Optional.empty();
        private Optional<Boolean> isSprinting = Optional.empty();
        private Optional<Boolean> isSwimming = Optional.empty();
        private Optional<Boolean> isFlying = Optional.empty();
        private Optional<Boolean> isBaby = Optional.empty();

        public static EntityFlagsPredicate.Builder flags() {
            return new EntityFlagsPredicate.Builder();
        }

        public EntityFlagsPredicate.Builder setOnGround(Boolean p_344950_) {
            this.isOnGround = Optional.of(p_344950_);
            return this;
        }

        public EntityFlagsPredicate.Builder setOnFire(Boolean pOnFire) {
            this.isOnFire = Optional.of(pOnFire);
            return this;
        }

        public EntityFlagsPredicate.Builder setCrouching(Boolean pIsCrouching) {
            this.isCrouching = Optional.of(pIsCrouching);
            return this;
        }

        public EntityFlagsPredicate.Builder setSprinting(Boolean pIsSprinting) {
            this.isSprinting = Optional.of(pIsSprinting);
            return this;
        }

        public EntityFlagsPredicate.Builder setSwimming(Boolean pIsSwimming) {
            this.isSwimming = Optional.of(pIsSwimming);
            return this;
        }

        public EntityFlagsPredicate.Builder setIsFlying(Boolean p_346066_) {
            this.isFlying = Optional.of(p_346066_);
            return this;
        }

        public EntityFlagsPredicate.Builder setIsBaby(Boolean pIsBaby) {
            this.isBaby = Optional.of(pIsBaby);
            return this;
        }

        public EntityFlagsPredicate build() {
            return new EntityFlagsPredicate(this.isOnGround, this.isOnFire, this.isCrouching, this.isSprinting, this.isSwimming, this.isFlying, this.isBaby);
        }
    }
}

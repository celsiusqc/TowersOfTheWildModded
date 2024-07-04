package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class FallAfterExplosionTrigger extends SimpleCriterionTrigger<FallAfterExplosionTrigger.TriggerInstance> {
    @Override
    public Codec<FallAfterExplosionTrigger.TriggerInstance> codec() {
        return FallAfterExplosionTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Vec3 pPos, @Nullable Entity pEntity) {
        Vec3 vec3 = pPlayer.position();
        LootContext lootcontext = pEntity != null ? EntityPredicate.createContext(pPlayer, pEntity) : null;
        this.trigger(pPlayer, p_335739_ -> p_335739_.matches(pPlayer.serverLevel(), pPos, vec3, lootcontext));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<LocationPredicate> startPosition,
        Optional<DistancePredicate> distance,
        Optional<ContextAwarePredicate> cause
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<FallAfterExplosionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337362_ -> p_337362_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FallAfterExplosionTrigger.TriggerInstance::player),
                        LocationPredicate.CODEC.optionalFieldOf("start_position").forGetter(FallAfterExplosionTrigger.TriggerInstance::startPosition),
                        DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(FallAfterExplosionTrigger.TriggerInstance::distance),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("cause").forGetter(FallAfterExplosionTrigger.TriggerInstance::cause)
                    )
                    .apply(p_337362_, FallAfterExplosionTrigger.TriggerInstance::new)
        );

        public static Criterion<FallAfterExplosionTrigger.TriggerInstance> fallAfterExplosion(DistancePredicate pDistance, EntityPredicate.Builder pCause) {
            return CriteriaTriggers.FALL_AFTER_EXPLOSION
                .createCriterion(
                    new FallAfterExplosionTrigger.TriggerInstance(
                        Optional.empty(), Optional.empty(), Optional.of(pDistance), Optional.of(EntityPredicate.wrap(pCause))
                    )
                );
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.cause(), ".cause");
        }

        public boolean matches(ServerLevel pLevel, Vec3 pStartPosition, Vec3 pEndPosition, @Nullable LootContext pContext) {
            if (this.startPosition.isPresent() && !this.startPosition.get().matches(pLevel, pStartPosition.x, pStartPosition.y, pStartPosition.z)) {
                return false;
            } else {
                return this.distance.isPresent() && !this.distance.get().matches(pStartPosition.x, pStartPosition.y, pStartPosition.z, pEndPosition.x, pEndPosition.y, pEndPosition.z)
                    ? false
                    : !this.cause.isPresent() || pContext != null && this.cause.get().matches(pContext);
            }
        }
    }
}

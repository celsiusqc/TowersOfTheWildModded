package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
    @Override
    public Codec<TargetBlockTrigger.TriggerInstance> codec() {
        return TargetBlockTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Entity pProjectile, Vec3 pVector, int pSignalStrength) {
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, pProjectile);
        this.trigger(pPlayer, p_70224_ -> p_70224_.matches(lootcontext, pVector, pSignalStrength));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints signalStrength, Optional<ContextAwarePredicate> projectile)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TargetBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337400_ -> p_337400_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TargetBlockTrigger.TriggerInstance::player),
                        MinMaxBounds.Ints.CODEC
                            .optionalFieldOf("signal_strength", MinMaxBounds.Ints.ANY)
                            .forGetter(TargetBlockTrigger.TriggerInstance::signalStrength),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("projectile").forGetter(TargetBlockTrigger.TriggerInstance::projectile)
                    )
                    .apply(p_337400_, TargetBlockTrigger.TriggerInstance::new)
        );

        public static Criterion<TargetBlockTrigger.TriggerInstance> targetHit(MinMaxBounds.Ints pSignalStrength, Optional<ContextAwarePredicate> pProjectile) {
            return CriteriaTriggers.TARGET_BLOCK_HIT.createCriterion(new TargetBlockTrigger.TriggerInstance(Optional.empty(), pSignalStrength, pProjectile));
        }

        public boolean matches(LootContext pContext, Vec3 pVector, int pSignalStrength) {
            return !this.signalStrength.matches(pSignalStrength) ? false : !this.projectile.isPresent() || this.projectile.get().matches(pContext);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.projectile, ".projectile");
        }
    }
}

package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class EntityHurtPlayerTrigger extends SimpleCriterionTrigger<EntityHurtPlayerTrigger.TriggerInstance> {
    @Override
    public Codec<EntityHurtPlayerTrigger.TriggerInstance> codec() {
        return EntityHurtPlayerTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, DamageSource pSource, float pDealtDamage, float pTakenDamage, boolean pBlocked) {
        this.trigger(pPlayer, p_35186_ -> p_35186_.matches(pPlayer, pSource, pDealtDamage, pTakenDamage, pBlocked));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DamagePredicate> damage)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<EntityHurtPlayerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337361_ -> p_337361_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EntityHurtPlayerTrigger.TriggerInstance::player),
                        DamagePredicate.CODEC.optionalFieldOf("damage").forGetter(EntityHurtPlayerTrigger.TriggerInstance::damage)
                    )
                    .apply(p_337361_, EntityHurtPlayerTrigger.TriggerInstance::new)
        );

        public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer() {
            return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
        }

        public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate pDamage) {
            return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage)));
        }

        public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate.Builder pDamage) {
            return CriteriaTriggers.ENTITY_HURT_PLAYER
                .createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage.build())));
        }

        public boolean matches(ServerPlayer pPlayer, DamageSource pSource, float pDealtDamage, float pTakenDamage, boolean pBlocked) {
            return !this.damage.isPresent() || this.damage.get().matches(pPlayer, pSource, pDealtDamage, pTakenDamage, pBlocked);
        }
    }
}

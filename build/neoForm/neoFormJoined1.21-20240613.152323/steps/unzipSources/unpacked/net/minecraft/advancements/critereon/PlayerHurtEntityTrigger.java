package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerHurtEntityTrigger extends SimpleCriterionTrigger<PlayerHurtEntityTrigger.TriggerInstance> {
    @Override
    public Codec<PlayerHurtEntityTrigger.TriggerInstance> codec() {
        return PlayerHurtEntityTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Entity pEntity, DamageSource pSource, float pAmountDealt, float pAmountTaken, boolean pBlocked) {
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
        this.trigger(pPlayer, p_60126_ -> p_60126_.matches(pPlayer, lootcontext, pSource, pAmountDealt, pAmountTaken, pBlocked));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DamagePredicate> damage, Optional<ContextAwarePredicate> entity)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<PlayerHurtEntityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337386_ -> p_337386_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerHurtEntityTrigger.TriggerInstance::player),
                        DamagePredicate.CODEC.optionalFieldOf("damage").forGetter(PlayerHurtEntityTrigger.TriggerInstance::damage),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PlayerHurtEntityTrigger.TriggerInstance::entity)
                    )
                    .apply(p_337386_, PlayerHurtEntityTrigger.TriggerInstance::new)
        );

        public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity() {
            return CriteriaTriggers.PLAYER_HURT_ENTITY
                .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
        }

        public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(Optional<DamagePredicate> pDamage) {
            return CriteriaTriggers.PLAYER_HURT_ENTITY
                .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), pDamage, Optional.empty()));
        }

        public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(DamagePredicate.Builder pDamage) {
            return CriteriaTriggers.PLAYER_HURT_ENTITY
                .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage.build()), Optional.empty()));
        }

        public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(Optional<EntityPredicate> pEntity) {
            return CriteriaTriggers.PLAYER_HURT_ENTITY
                .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), EntityPredicate.wrap(pEntity)));
        }

        public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(
            Optional<DamagePredicate> pDamage, Optional<EntityPredicate> pEntity
        ) {
            return CriteriaTriggers.PLAYER_HURT_ENTITY
                .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), pDamage, EntityPredicate.wrap(pEntity)));
        }

        public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(
            DamagePredicate.Builder pDamage, Optional<EntityPredicate> pEntity
        ) {
            return CriteriaTriggers.PLAYER_HURT_ENTITY
                .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage.build()), EntityPredicate.wrap(pEntity)));
        }

        public boolean matches(ServerPlayer pPlayer, LootContext pContext, DamageSource pDamage, float pDealt, float pTaken, boolean pBlocked) {
            return this.damage.isPresent() && !this.damage.get().matches(pPlayer, pDamage, pDealt, pTaken, pBlocked)
                ? false
                : !this.entity.isPresent() || this.entity.get().matches(pContext);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.entity, ".entity");
        }
    }
}

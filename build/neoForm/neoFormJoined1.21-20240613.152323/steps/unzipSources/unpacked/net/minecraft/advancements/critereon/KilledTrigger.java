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

public class KilledTrigger extends SimpleCriterionTrigger<KilledTrigger.TriggerInstance> {
    @Override
    public Codec<KilledTrigger.TriggerInstance> codec() {
        return KilledTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Entity pEntity, DamageSource pSource) {
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
        this.trigger(pPlayer, p_48112_ -> p_48112_.matches(pPlayer, lootcontext, pSource));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entityPredicate, Optional<DamageSourcePredicate> killingBlow
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<KilledTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337374_ -> p_337374_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledTrigger.TriggerInstance::player),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(KilledTrigger.TriggerInstance::entityPredicate),
                        DamageSourcePredicate.CODEC.optionalFieldOf("killing_blow").forGetter(KilledTrigger.TriggerInstance::killingBlow)
                    )
                    .apply(p_337374_, KilledTrigger.TriggerInstance::new)
        );

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> pEntityPredicate) {
            return CriteriaTriggers.PLAYER_KILLED_ENTITY
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.empty()));
        }

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder pEntityPredicate) {
            return CriteriaTriggers.PLAYER_KILLED_ENTITY
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.empty()));
        }

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity() {
            return CriteriaTriggers.PLAYER_KILLED_ENTITY
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
        }

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(
            Optional<EntityPredicate> pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow
        ) {
            return CriteriaTriggers.PLAYER_KILLED_ENTITY
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), pKillingBlow));
        }

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow) {
            return CriteriaTriggers.PLAYER_KILLED_ENTITY
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), pKillingBlow));
        }

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
            return CriteriaTriggers.PLAYER_KILLED_ENTITY
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.of(pKillingBlow.build())));
        }

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
            return CriteriaTriggers.PLAYER_KILLED_ENTITY
                .createCriterion(
                    new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.of(pKillingBlow.build()))
                );
        }

        public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntityNearSculkCatalyst() {
            return CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
        }

        public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> pEntityPredicate) {
            return CriteriaTriggers.ENTITY_KILLED_PLAYER
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.empty()));
        }

        public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder pEntityPredicate) {
            return CriteriaTriggers.ENTITY_KILLED_PLAYER
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.empty()));
        }

        public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer() {
            return CriteriaTriggers.ENTITY_KILLED_PLAYER
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
        }

        public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(
            Optional<EntityPredicate> pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow
        ) {
            return CriteriaTriggers.ENTITY_KILLED_PLAYER
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), pKillingBlow));
        }

        public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow) {
            return CriteriaTriggers.ENTITY_KILLED_PLAYER
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), pKillingBlow));
        }

        public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
            return CriteriaTriggers.ENTITY_KILLED_PLAYER
                .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.of(pKillingBlow.build())));
        }

        public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
            return CriteriaTriggers.ENTITY_KILLED_PLAYER
                .createCriterion(
                    new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.of(pKillingBlow.build()))
                );
        }

        public boolean matches(ServerPlayer pPlayer, LootContext pContext, DamageSource pSource) {
            return this.killingBlow.isPresent() && !this.killingBlow.get().matches(pPlayer, pSource)
                ? false
                : this.entityPredicate.isEmpty() || this.entityPredicate.get().matches(pContext);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.entityPredicate, ".entity");
        }
    }
}

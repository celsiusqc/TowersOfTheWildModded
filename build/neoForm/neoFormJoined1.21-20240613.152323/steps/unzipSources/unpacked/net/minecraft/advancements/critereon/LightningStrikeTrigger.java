package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
    @Override
    public Codec<LightningStrikeTrigger.TriggerInstance> codec() {
        return LightningStrikeTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, LightningBolt pLightning, List<Entity> pNearbyEntities) {
        List<LootContext> list = pNearbyEntities.stream().map(p_153390_ -> EntityPredicate.createContext(pPlayer, p_153390_)).collect(Collectors.toList());
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, pLightning);
        this.trigger(pPlayer, p_153402_ -> p_153402_.matches(lootcontext, list));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> lightning, Optional<ContextAwarePredicate> bystander
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<LightningStrikeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337378_ -> p_337378_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(LightningStrikeTrigger.TriggerInstance::player),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("lightning").forGetter(LightningStrikeTrigger.TriggerInstance::lightning),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("bystander").forGetter(LightningStrikeTrigger.TriggerInstance::bystander)
                    )
                    .apply(p_337378_, LightningStrikeTrigger.TriggerInstance::new)
        );

        public static Criterion<LightningStrikeTrigger.TriggerInstance> lightningStrike(
            Optional<EntityPredicate> pLightning, Optional<EntityPredicate> pBystander
        ) {
            return CriteriaTriggers.LIGHTNING_STRIKE
                .createCriterion(new LightningStrikeTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pLightning), EntityPredicate.wrap(pBystander)));
        }

        public boolean matches(LootContext pPlayerContext, List<LootContext> pEntityContexts) {
            return this.lightning.isPresent() && !this.lightning.get().matches(pPlayerContext)
                ? false
                : !this.bystander.isPresent() || !pEntityContexts.stream().noneMatch(this.bystander.get()::matches);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.lightning, ".lightning");
            pValidator.validateEntity(this.bystander, ".bystander");
        }
    }
}

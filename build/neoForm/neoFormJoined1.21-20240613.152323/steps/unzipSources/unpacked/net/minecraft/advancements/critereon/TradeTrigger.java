package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
    @Override
    public Codec<TradeTrigger.TriggerInstance> codec() {
        return TradeTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, AbstractVillager pVillager, ItemStack pStack) {
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, pVillager);
        this.trigger(pPlayer, p_70970_ -> p_70970_.matches(lootcontext, pStack));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> villager, Optional<ItemPredicate> item)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TradeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337401_ -> p_337401_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TradeTrigger.TriggerInstance::player),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("villager").forGetter(TradeTrigger.TriggerInstance::villager),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TradeTrigger.TriggerInstance::item)
                    )
                    .apply(p_337401_, TradeTrigger.TriggerInstance::new)
        );

        public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager() {
            return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
        }

        public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager(EntityPredicate.Builder pVillager) {
            return CriteriaTriggers.TRADE
                .createCriterion(new TradeTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pVillager)), Optional.empty(), Optional.empty()));
        }

        public boolean matches(LootContext pContext, ItemStack pStack) {
            return this.villager.isPresent() && !this.villager.get().matches(pContext) ? false : !this.item.isPresent() || this.item.get().test(pStack);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.villager, ".villager");
        }
    }
}

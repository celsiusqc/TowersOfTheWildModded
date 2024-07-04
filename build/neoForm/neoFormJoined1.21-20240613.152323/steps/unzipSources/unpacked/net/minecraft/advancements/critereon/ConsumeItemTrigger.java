package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ConsumeItemTrigger extends SimpleCriterionTrigger<ConsumeItemTrigger.TriggerInstance> {
    @Override
    public Codec<ConsumeItemTrigger.TriggerInstance> codec() {
        return ConsumeItemTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, ItemStack pItem) {
        this.trigger(pPlayer, p_23687_ -> p_23687_.matches(pItem));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ConsumeItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337348_ -> p_337348_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ConsumeItemTrigger.TriggerInstance::player),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ConsumeItemTrigger.TriggerInstance::item)
                    )
                    .apply(p_337348_, ConsumeItemTrigger.TriggerInstance::new)
        );

        public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem() {
            return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
        }

        public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(ItemLike pItem) {
            return usedItem(ItemPredicate.Builder.item().of(pItem.asItem()));
        }

        public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(ItemPredicate.Builder pItem) {
            return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.of(pItem.build())));
        }

        public boolean matches(ItemStack pItem) {
            return this.item.isEmpty() || this.item.get().test(pItem);
        }
    }
}

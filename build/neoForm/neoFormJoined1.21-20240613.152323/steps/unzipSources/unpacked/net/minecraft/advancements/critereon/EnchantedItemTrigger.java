package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnchantedItemTrigger extends SimpleCriterionTrigger<EnchantedItemTrigger.TriggerInstance> {
    @Override
    public Codec<EnchantedItemTrigger.TriggerInstance> codec() {
        return EnchantedItemTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, ItemStack pItem, int pLevelsSpent) {
        this.trigger(pPlayer, p_27675_ -> p_27675_.matches(pItem, pLevelsSpent));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints levels)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<EnchantedItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337356_ -> p_337356_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EnchantedItemTrigger.TriggerInstance::player),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(EnchantedItemTrigger.TriggerInstance::item),
                        MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", MinMaxBounds.Ints.ANY).forGetter(EnchantedItemTrigger.TriggerInstance::levels)
                    )
                    .apply(p_337356_, EnchantedItemTrigger.TriggerInstance::new)
        );

        public static Criterion<EnchantedItemTrigger.TriggerInstance> enchantedItem() {
            return CriteriaTriggers.ENCHANTED_ITEM
                .createCriterion(new EnchantedItemTrigger.TriggerInstance(Optional.empty(), Optional.empty(), MinMaxBounds.Ints.ANY));
        }

        public boolean matches(ItemStack pItem, int pLevels) {
            return this.item.isPresent() && !this.item.get().test(pItem) ? false : this.levels.matches(pLevels);
        }
    }
}

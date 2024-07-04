package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
    @Override
    public Codec<ItemDurabilityTrigger.TriggerInstance> codec() {
        return ItemDurabilityTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, ItemStack pItem, int pNewDurability) {
        this.trigger(pPlayer, p_43676_ -> p_43676_.matches(pItem, pNewDurability));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints durability, MinMaxBounds.Ints delta
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ItemDurabilityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337370_ -> p_337370_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemDurabilityTrigger.TriggerInstance::player),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemDurabilityTrigger.TriggerInstance::item),
                        MinMaxBounds.Ints.CODEC
                            .optionalFieldOf("durability", MinMaxBounds.Ints.ANY)
                            .forGetter(ItemDurabilityTrigger.TriggerInstance::durability),
                        MinMaxBounds.Ints.CODEC.optionalFieldOf("delta", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::delta)
                    )
                    .apply(p_337370_, ItemDurabilityTrigger.TriggerInstance::new)
        );

        public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(Optional<ItemPredicate> pItem, MinMaxBounds.Ints pDurability) {
            return changedDurability(Optional.empty(), pItem, pDurability);
        }

        public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(
            Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem, MinMaxBounds.Ints pDurability
        ) {
            return CriteriaTriggers.ITEM_DURABILITY_CHANGED
                .createCriterion(new ItemDurabilityTrigger.TriggerInstance(pPlayer, pItem, pDurability, MinMaxBounds.Ints.ANY));
        }

        public boolean matches(ItemStack pItem, int pDurability) {
            if (this.item.isPresent() && !this.item.get().test(pItem)) {
                return false;
            } else {
                return !this.durability.matches(pItem.getMaxDamage() - pDurability) ? false : this.delta.matches(pItem.getDamageValue() - pDurability);
            }
        }
    }
}

package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingRodHookedTrigger extends SimpleCriterionTrigger<FishingRodHookedTrigger.TriggerInstance> {
    @Override
    public Codec<FishingRodHookedTrigger.TriggerInstance> codec() {
        return FishingRodHookedTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, ItemStack pRod, FishingHook pEntity, Collection<ItemStack> pStacks) {
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, (Entity)(pEntity.getHookedIn() != null ? pEntity.getHookedIn() : pEntity));
        this.trigger(pPlayer, p_40425_ -> p_40425_.matches(pRod, lootcontext, pStacks));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<ItemPredicate> rod, Optional<ContextAwarePredicate> entity, Optional<ItemPredicate> item
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<FishingRodHookedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337365_ -> p_337365_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FishingRodHookedTrigger.TriggerInstance::player),
                        ItemPredicate.CODEC.optionalFieldOf("rod").forGetter(FishingRodHookedTrigger.TriggerInstance::rod),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(FishingRodHookedTrigger.TriggerInstance::entity),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(FishingRodHookedTrigger.TriggerInstance::item)
                    )
                    .apply(p_337365_, FishingRodHookedTrigger.TriggerInstance::new)
        );

        public static Criterion<FishingRodHookedTrigger.TriggerInstance> fishedItem(
            Optional<ItemPredicate> pRod, Optional<EntityPredicate> pEntity, Optional<ItemPredicate> pItem
        ) {
            return CriteriaTriggers.FISHING_ROD_HOOKED
                .createCriterion(new FishingRodHookedTrigger.TriggerInstance(Optional.empty(), pRod, EntityPredicate.wrap(pEntity), pItem));
        }

        public boolean matches(ItemStack pRod, LootContext pContext, Collection<ItemStack> pStacks) {
            if (this.rod.isPresent() && !this.rod.get().test(pRod)) {
                return false;
            } else if (this.entity.isPresent() && !this.entity.get().matches(pContext)) {
                return false;
            } else {
                if (this.item.isPresent()) {
                    boolean flag = false;
                    Entity entity = pContext.getParamOrNull(LootContextParams.THIS_ENTITY);
                    if (entity instanceof ItemEntity itementity && this.item.get().test(itementity.getItem())) {
                        flag = true;
                    }

                    for (ItemStack itemstack : pStacks) {
                        if (this.item.get().test(itemstack)) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }

                return true;
            }
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.entity, ".entity");
        }
    }
}

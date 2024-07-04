package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
    @Override
    public Codec<ItemUsedOnLocationTrigger.TriggerInstance> codec() {
        return ItemUsedOnLocationTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, BlockPos pPos, ItemStack pStack) {
        ServerLevel serverlevel = pPlayer.serverLevel();
        BlockState blockstate = serverlevel.getBlockState(pPos);
        LootParams lootparams = new LootParams.Builder(serverlevel)
            .withParameter(LootContextParams.ORIGIN, pPos.getCenter())
            .withParameter(LootContextParams.THIS_ENTITY, pPlayer)
            .withParameter(LootContextParams.BLOCK_STATE, blockstate)
            .withParameter(LootContextParams.TOOL, pStack)
            .create(LootContextParamSets.ADVANCEMENT_LOCATION);
        LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
        this.trigger(pPlayer, p_286596_ -> p_286596_.matches(lootcontext));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ItemUsedOnLocationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337372_ -> p_337372_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::player),
                        ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::location)
                    )
                    .apply(p_337372_, ItemUsedOnLocationTrigger.TriggerInstance::new)
        );

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block pBlock) {
            ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock).build()
            );
            return CriteriaTriggers.PLACED_BLOCK
                .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(LootItemCondition.Builder... pConditions) {
            ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
                Arrays.stream(pConditions).map(LootItemCondition.Builder::build).toArray(LootItemCondition[]::new)
            );
            return CriteriaTriggers.PLACED_BLOCK
                .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
        }

        private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
            ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
                LocationCheck.checkLocation(pLocation).build(), MatchTool.toolMatches(pTool).build()
            );
            return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
            return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(
            LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool
        ) {
            return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
        }

        public boolean matches(LootContext pContext) {
            return this.location.isEmpty() || this.location.get().matches(pContext);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            this.location.ifPresent(p_311427_ -> pValidator.validate(p_311427_, LootContextParamSets.ADVANCEMENT_LOCATION, ".location"));
        }
    }
}

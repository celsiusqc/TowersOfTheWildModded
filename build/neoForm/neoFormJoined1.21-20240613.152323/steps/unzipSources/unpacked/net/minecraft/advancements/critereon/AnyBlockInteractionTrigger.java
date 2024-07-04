package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class AnyBlockInteractionTrigger extends SimpleCriterionTrigger<AnyBlockInteractionTrigger.TriggerInstance> {
    @Override
    public Codec<AnyBlockInteractionTrigger.TriggerInstance> codec() {
        return AnyBlockInteractionTrigger.TriggerInstance.CODEC;
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
        this.trigger(pPlayer, p_320901_ -> p_320901_.matches(lootcontext));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<AnyBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337340_ -> p_337340_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(AnyBlockInteractionTrigger.TriggerInstance::player),
                        ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(AnyBlockInteractionTrigger.TriggerInstance::location)
                    )
                    .apply(p_337340_, AnyBlockInteractionTrigger.TriggerInstance::new)
        );

        public boolean matches(LootContext pContext) {
            return this.location.isEmpty() || this.location.get().matches(pContext);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            this.location.ifPresent(p_320455_ -> pValidator.validate(p_320455_, LootContextParamSets.ADVANCEMENT_LOCATION, ".location"));
        }
    }
}

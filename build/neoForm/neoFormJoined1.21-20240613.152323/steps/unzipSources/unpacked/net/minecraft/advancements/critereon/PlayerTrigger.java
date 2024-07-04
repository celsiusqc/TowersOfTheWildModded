package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class PlayerTrigger extends SimpleCriterionTrigger<PlayerTrigger.TriggerInstance> {
    @Override
    public Codec<PlayerTrigger.TriggerInstance> codec() {
        return PlayerTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer) {
        this.trigger(pPlayer, p_222625_ -> true);
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<PlayerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_337390_ -> p_337390_.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerTrigger.TriggerInstance::player))
                    .apply(p_337390_, PlayerTrigger.TriggerInstance::new)
        );

        public static Criterion<PlayerTrigger.TriggerInstance> located(LocationPredicate.Builder pLocation) {
            return CriteriaTriggers.LOCATION
                .createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().located(pLocation)))));
        }

        public static Criterion<PlayerTrigger.TriggerInstance> located(EntityPredicate.Builder pEntity) {
            return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pEntity.build()))));
        }

        public static Criterion<PlayerTrigger.TriggerInstance> located(Optional<EntityPredicate> pEntity) {
            return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(EntityPredicate.wrap(pEntity)));
        }

        public static Criterion<PlayerTrigger.TriggerInstance> sleptInBed() {
            return CriteriaTriggers.SLEPT_IN_BED.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
        }

        public static Criterion<PlayerTrigger.TriggerInstance> raidWon() {
            return CriteriaTriggers.RAID_WIN.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
        }

        public static Criterion<PlayerTrigger.TriggerInstance> avoidVibration() {
            return CriteriaTriggers.AVOID_VIBRATION.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
        }

        public static Criterion<PlayerTrigger.TriggerInstance> tick() {
            return CriteriaTriggers.TICK.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
        }

        public static Criterion<PlayerTrigger.TriggerInstance> walkOnBlockWithEquipment(Block pBlock, Item pEquipment) {
            return located(
                EntityPredicate.Builder.entity()
                    .equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(pEquipment)))
                    .steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(pBlock)))
            );
        }
    }
}

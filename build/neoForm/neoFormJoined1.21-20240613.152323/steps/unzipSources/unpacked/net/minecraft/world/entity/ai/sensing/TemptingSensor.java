package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TemptingSensor extends Sensor<PathfinderMob> {
    public static final int TEMPTATION_RANGE = 10;
    private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().range(10.0).ignoreLineOfSight();
    private final Predicate<ItemStack> temptations;

    public TemptingSensor(Predicate<ItemStack> pTemptations) {
        this.temptations = pTemptations;
    }

    protected void doTick(ServerLevel pLevel, PathfinderMob pEntity) {
        Brain<?> brain = pEntity.getBrain();
        List<Player> list = pLevel.players()
            .stream()
            .filter(EntitySelector.NO_SPECTATORS)
            .filter(p_148342_ -> TEMPT_TARGETING.test(pEntity, p_148342_))
            .filter(p_148335_ -> pEntity.closerThan(p_148335_, 10.0))
            .filter(this::playerHoldingTemptation)
            .filter(p_325765_ -> !pEntity.hasPassenger(p_325765_))
            .sorted(Comparator.comparingDouble(pEntity::distanceToSqr))
            .collect(Collectors.toList());
        if (!list.isEmpty()) {
            Player player = list.get(0);
            brain.setMemory(MemoryModuleType.TEMPTING_PLAYER, player);
        } else {
            brain.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
        }
    }

    private boolean playerHoldingTemptation(Player p_148337_) {
        return this.isTemptation(p_148337_.getMainHandItem()) || this.isTemptation(p_148337_.getOffhandItem());
    }

    private boolean isTemptation(ItemStack pStack) {
        return this.temptations.test(pStack);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
    }
}

package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class SimpleCriterionTrigger<T extends SimpleCriterionTrigger.SimpleInstance> implements CriterionTrigger<T> {
    private final Map<PlayerAdvancements, Set<CriterionTrigger.Listener<T>>> players = Maps.newIdentityHashMap();

    @Override
    public final void addPlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<T> pListener) {
        this.players.computeIfAbsent(pPlayerAdvancements, p_66252_ -> Sets.newHashSet()).add(pListener);
    }

    @Override
    public final void removePlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<T> pListener) {
        Set<CriterionTrigger.Listener<T>> set = this.players.get(pPlayerAdvancements);
        if (set != null) {
            set.remove(pListener);
            if (set.isEmpty()) {
                this.players.remove(pPlayerAdvancements);
            }
        }
    }

    @Override
    public final void removePlayerListeners(PlayerAdvancements pPlayerAdvancements) {
        this.players.remove(pPlayerAdvancements);
    }

    protected void trigger(ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        PlayerAdvancements playeradvancements = pPlayer.getAdvancements();
        Set<CriterionTrigger.Listener<T>> set = this.players.get(playeradvancements);
        if (set != null && !set.isEmpty()) {
            LootContext lootcontext = EntityPredicate.createContext(pPlayer, pPlayer);
            List<CriterionTrigger.Listener<T>> list = null;

            for (CriterionTrigger.Listener<T> listener : set) {
                T t = listener.trigger();
                if (pTestTrigger.test(t)) {
                    Optional<ContextAwarePredicate> optional = t.player();
                    if (optional.isEmpty() || optional.get().matches(lootcontext)) {
                        if (list == null) {
                            list = Lists.newArrayList();
                        }

                        list.add(listener);
                    }
                }
            }

            if (list != null) {
                for (CriterionTrigger.Listener<T> listener1 : list) {
                    listener1.run(playeradvancements);
                }
            }
        }
    }

    public interface SimpleInstance extends CriterionTriggerInstance {
        @Override
        default void validate(CriterionValidator p_312329_) {
            p_312329_.validateEntity(this.player(), ".player");
        }

        Optional<ContextAwarePredicate> player();
    }
}

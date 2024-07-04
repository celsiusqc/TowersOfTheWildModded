package net.minecraft.advancements;

import com.mojang.serialization.Codec;
import net.minecraft.server.PlayerAdvancements;

public interface CriterionTrigger<T extends CriterionTriggerInstance> {
    void addPlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<T> pListener);

    void removePlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<T> pListener);

    void removePlayerListeners(PlayerAdvancements pPlayerAdvancements);

    Codec<T> codec();

    default Criterion<T> createCriterion(T pTriggerInstance) {
        return new Criterion<>(this, pTriggerInstance);
    }

    public static record Listener<T extends CriterionTriggerInstance>(T trigger, AdvancementHolder advancement, String criterion) {
        public void run(PlayerAdvancements pPlayerAdvancements) {
            pPlayerAdvancements.award(this.advancement, this.criterion);
        }
    }
}

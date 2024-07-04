package net.minecraft.world.scores;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;

class PlayerScores {
    private final Reference2ObjectOpenHashMap<Objective, Score> scores = new Reference2ObjectOpenHashMap<>(16, 0.5F);

    @Nullable
    public Score get(Objective pObjective) {
        return this.scores.get(pObjective);
    }

    public Score getOrCreate(Objective pObjective, Consumer<Score> pCreator) {
        return this.scores.computeIfAbsent(pObjective, p_314724_ -> {
            Score score = new Score();
            pCreator.accept(score);
            return score;
        });
    }

    public boolean remove(Objective pObjective) {
        return this.scores.remove(pObjective) != null;
    }

    public boolean hasScores() {
        return !this.scores.isEmpty();
    }

    public Object2IntMap<Objective> listScores() {
        Object2IntMap<Objective> object2intmap = new Object2IntOpenHashMap<>();
        this.scores.forEach((p_313743_, p_313919_) -> object2intmap.put(p_313743_, p_313919_.value()));
        return object2intmap;
    }

    void setScore(Objective pObjective, Score pScore) {
        this.scores.put(pObjective, pScore);
    }

    Map<Objective, Score> listRawScores() {
        return Collections.unmodifiableMap(this.scores);
    }
}

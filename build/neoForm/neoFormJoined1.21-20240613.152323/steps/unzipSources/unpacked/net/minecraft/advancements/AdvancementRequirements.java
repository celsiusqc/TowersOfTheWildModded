package net.minecraft.advancements;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;

public record AdvancementRequirements(List<List<String>> requirements) {
    public static final Codec<AdvancementRequirements> CODEC = Codec.STRING
        .listOf()
        .listOf()
        .xmap(AdvancementRequirements::new, AdvancementRequirements::requirements);
    public static final AdvancementRequirements EMPTY = new AdvancementRequirements(List.of());

    public AdvancementRequirements(FriendlyByteBuf p_301089_) {
        this(p_301089_.readList(p_319371_ -> p_319371_.readList(FriendlyByteBuf::readUtf)));
    }

    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeCollection(this.requirements, (p_319372_, p_319373_) -> p_319372_.writeCollection(p_319373_, FriendlyByteBuf::writeUtf));
    }

    public static AdvancementRequirements allOf(Collection<String> pRequirements) {
        return new AdvancementRequirements(pRequirements.stream().map(List::of).toList());
    }

    public static AdvancementRequirements anyOf(Collection<String> pCriteria) {
        return new AdvancementRequirements(List.of(List.copyOf(pCriteria)));
    }

    public int size() {
        return this.requirements.size();
    }

    public boolean test(Predicate<String> pPredicate) {
        if (this.requirements.isEmpty()) {
            return false;
        } else {
            for (List<String> list : this.requirements) {
                if (!anyMatch(list, pPredicate)) {
                    return false;
                }
            }

            return true;
        }
    }

    public int count(Predicate<String> pFilter) {
        int i = 0;

        for (List<String> list : this.requirements) {
            if (anyMatch(list, pFilter)) {
                i++;
            }
        }

        return i;
    }

    private static boolean anyMatch(List<String> pRequirements, Predicate<String> pPredicate) {
        for (String s : pRequirements) {
            if (pPredicate.test(s)) {
                return true;
            }
        }

        return false;
    }

    public DataResult<AdvancementRequirements> validate(Set<String> pRequirements) {
        Set<String> set = new ObjectOpenHashSet<>();

        for (List<String> list : this.requirements) {
            if (list.isEmpty() && pRequirements.isEmpty()) {
                return DataResult.error(() -> "Requirement entry cannot be empty");
            }

            set.addAll(list);
        }

        if (!pRequirements.equals(set)) {
            Set<String> set1 = Sets.difference(pRequirements, set);
            Set<String> set2 = Sets.difference(set, pRequirements);
            return DataResult.error(
                () -> "Advancement completion requirements did not exactly match specified criteria. Missing: " + set1 + ". Unknown: " + set2
            );
        } else {
            return DataResult.success(this);
        }
    }

    public boolean isEmpty() {
        return this.requirements.isEmpty();
    }

    @Override
    public String toString() {
        return this.requirements.toString();
    }

    public Set<String> names() {
        Set<String> set = new ObjectOpenHashSet<>();

        for (List<String> list : this.requirements) {
            set.addAll(list);
        }

        return set;
    }

    public interface Strategy {
        AdvancementRequirements.Strategy AND = AdvancementRequirements::allOf;
        AdvancementRequirements.Strategy OR = AdvancementRequirements::anyOf;

        AdvancementRequirements create(Collection<String> pCriteria);
    }
}

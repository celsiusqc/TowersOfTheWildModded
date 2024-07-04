package net.minecraft.advancements.critereon;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class CriterionValidator {
    private final ProblemReporter reporter;
    private final HolderGetter.Provider lootData;

    public CriterionValidator(ProblemReporter pReporter, HolderGetter.Provider pLootData) {
        this.reporter = pReporter;
        this.lootData = pLootData;
    }

    public void validateEntity(Optional<ContextAwarePredicate> pEntity, String pName) {
        pEntity.ifPresent(p_311858_ -> this.validateEntity(p_311858_, pName));
    }

    public void validateEntities(List<ContextAwarePredicate> pEntities, String pName) {
        this.validate(pEntities, LootContextParamSets.ADVANCEMENT_ENTITY, pName);
    }

    public void validateEntity(ContextAwarePredicate pEntity, String pName) {
        this.validate(pEntity, LootContextParamSets.ADVANCEMENT_ENTITY, pName);
    }

    public void validate(ContextAwarePredicate pEntity, LootContextParamSet pContextParams, String pName) {
        pEntity.validate(new ValidationContext(this.reporter.forChild(pName), pContextParams, this.lootData));
    }

    public void validate(List<ContextAwarePredicate> pEntities, LootContextParamSet pContextParams, String pName) {
        for (int i = 0; i < pEntities.size(); i++) {
            ContextAwarePredicate contextawarepredicate = pEntities.get(i);
            contextawarepredicate.validate(new ValidationContext(this.reporter.forChild(pName + "[" + i + "]"), pContextParams, this.lootData));
        }
    }
}

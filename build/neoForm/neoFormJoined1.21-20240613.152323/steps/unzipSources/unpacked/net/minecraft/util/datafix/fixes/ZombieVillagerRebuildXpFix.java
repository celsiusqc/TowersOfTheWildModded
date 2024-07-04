package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class ZombieVillagerRebuildXpFix extends NamedEntityFix {
    public ZombieVillagerRebuildXpFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "Zombie Villager XP rebuild", References.ENTITY, "minecraft:zombie_villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> pTyped) {
        return pTyped.update(DSL.remainderFinder(), p_337684_ -> {
            Optional<Number> optional = p_337684_.get("Xp").asNumber().result();
            if (optional.isEmpty()) {
                int i = p_337684_.get("VillagerData").get("level").asInt(1);
                return p_337684_.set("Xp", p_337684_.createInt(VillagerRebuildLevelAndXpFix.getMinXpPerLevel(i)));
            } else {
                return p_337684_;
            }
        });
    }
}

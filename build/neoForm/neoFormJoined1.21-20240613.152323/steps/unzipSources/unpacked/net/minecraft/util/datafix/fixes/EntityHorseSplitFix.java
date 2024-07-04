package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.Util;

public class EntityHorseSplitFix extends EntityRenameFix {
    public EntityHorseSplitFix(Schema pOutputSchema, boolean pChangesType) {
        super("EntityHorseSplitFix", pOutputSchema, pChangesType);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
        if (Objects.equals("EntityHorse", pEntityName)) {
            Dynamic<?> dynamic = pTyped.get(DSL.remainderFinder());
            int i = dynamic.get("Type").asInt(0);

            String s = switch (i) {
                case 1 -> "Donkey";
                case 2 -> "Mule";
                case 3 -> "ZombieHorse";
                case 4 -> "SkeletonHorse";
                default -> "Horse";
            };
            Type<?> type = this.getOutputSchema().findChoiceType(References.ENTITY).types().get(s);
            return Pair.of(s, Util.writeAndReadTypedOrThrow(pTyped, type, p_341965_ -> p_341965_.remove("Type")));
        } else {
            return Pair.of(pEntityName, pTyped);
        }
    }
}

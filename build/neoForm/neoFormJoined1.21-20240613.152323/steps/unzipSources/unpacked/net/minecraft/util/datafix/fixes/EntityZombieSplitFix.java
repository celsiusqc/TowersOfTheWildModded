package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.function.Supplier;
import net.minecraft.Util;

public class EntityZombieSplitFix extends EntityRenameFix {
    private final Supplier<Type<?>> zombieVillagerType = Suppliers.memoize(() -> this.getOutputSchema().getChoiceType(References.ENTITY, "ZombieVillager"));

    public EntityZombieSplitFix(Schema pOutputSchema) {
        super("EntityZombieSplitFix", pOutputSchema, true);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
        if (!pEntityName.equals("Zombie")) {
            return Pair.of(pEntityName, pTyped);
        } else {
            Dynamic<?> dynamic = pTyped.getOptional(DSL.remainderFinder()).orElseThrow();
            int i = dynamic.get("ZombieType").asInt(0);
            String s;
            Typed<?> typed;
            switch (i) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    s = "ZombieVillager";
                    typed = this.changeSchemaToZombieVillager(pTyped, i - 1);
                    break;
                case 6:
                    s = "Husk";
                    typed = pTyped;
                    break;
                default:
                    s = "Zombie";
                    typed = pTyped;
            }

            return Pair.of(s, typed.update(DSL.remainderFinder(), p_341600_ -> p_341600_.remove("ZombieType")));
        }
    }

    private Typed<?> changeSchemaToZombieVillager(Typed<?> pTyped, int pProfession) {
        return Util.writeAndReadTypedOrThrow(pTyped, this.zombieVillagerType.get(), p_341611_ -> p_341611_.set("Profession", p_341611_.createInt(pProfession)));
    }
}

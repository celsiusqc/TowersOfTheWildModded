package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.Util;

public class EntityMinecartIdentifiersFix extends EntityRenameFix {
    public EntityMinecartIdentifiersFix(Schema pOutputSchema) {
        super("EntityMinecartIdentifiersFix", pOutputSchema, true);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
        if (!pEntityName.equals("Minecart")) {
            return Pair.of(pEntityName, pTyped);
        } else {
            int i = pTyped.getOrCreate(DSL.remainderFinder()).get("Type").asInt(0);

            String s = switch (i) {
                case 1 -> "MinecartChest";
                case 2 -> "MinecartFurnace";
                default -> "MinecartRideable";
            };
            Type<?> type = this.getOutputSchema().findChoiceType(References.ENTITY).types().get(s);
            return Pair.of(s, Util.writeAndReadTypedOrThrow(pTyped, type, p_341966_ -> p_341966_.remove("Type")));
        }
    }
}

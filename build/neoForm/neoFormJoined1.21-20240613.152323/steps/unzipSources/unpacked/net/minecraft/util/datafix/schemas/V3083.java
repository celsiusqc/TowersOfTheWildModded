package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3083 extends NamespacedSchema {
    public V3083(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.register(
            map,
            "minecraft:allay",
            () -> DSL.optionalFields(
                    "Inventory",
                    DSL.list(References.ITEM_STACK.in(pSchema)),
                    "listener",
                    DSL.optionalFields("event", DSL.optionalFields("game_event", References.GAME_EVENT_NAME.in(pSchema))),
                    V100.equipment(pSchema)
                )
        );
        return map;
    }
}

package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3807 extends NamespacedSchema {
    public V3807(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        pSchema.register(
            map,
            "minecraft:vault",
            () -> DSL.optionalFields(
                    "config",
                    DSL.optionalFields("key_item", References.ITEM_STACK.in(pSchema)),
                    "server_data",
                    DSL.optionalFields("items_to_eject", DSL.list(References.ITEM_STACK.in(pSchema))),
                    "shared_data",
                    DSL.optionalFields("display_item", References.ITEM_STACK.in(pSchema))
                )
        );
        return map;
    }
}

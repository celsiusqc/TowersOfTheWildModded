package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_3 extends NamespacedSchema {
    public V3818_3(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            true,
            References.DATA_COMPONENTS,
            () -> DSL.optionalFields(
                    Pair.of("minecraft:bees", DSL.list(DSL.optionalFields("entity_data", References.ENTITY_TREE.in(pSchema)))),
                    Pair.of("minecraft:block_entity_data", References.BLOCK_ENTITY.in(pSchema)),
                    Pair.of("minecraft:bundle_contents", DSL.list(References.ITEM_STACK.in(pSchema))),
                    Pair.of(
                        "minecraft:can_break",
                        DSL.optionalFields(
                            "predicates",
                            DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(pSchema), DSL.list(References.BLOCK_NAME.in(pSchema)))))
                        )
                    ),
                    Pair.of(
                        "minecraft:can_place_on",
                        DSL.optionalFields(
                            "predicates",
                            DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(pSchema), DSL.list(References.BLOCK_NAME.in(pSchema)))))
                        )
                    ),
                    Pair.of("minecraft:charged_projectiles", DSL.list(References.ITEM_STACK.in(pSchema))),
                    Pair.of("minecraft:container", DSL.list(DSL.optionalFields("item", References.ITEM_STACK.in(pSchema)))),
                    Pair.of("minecraft:entity_data", References.ENTITY_TREE.in(pSchema)),
                    Pair.of("minecraft:pot_decorations", DSL.list(References.ITEM_NAME.in(pSchema))),
                    Pair.of("minecraft:food", DSL.optionalFields("using_converts_to", References.ITEM_STACK.in(pSchema)))
                )
        );
    }
}

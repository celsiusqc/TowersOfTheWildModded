package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V102 extends Schema {
    public V102(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            true,
            References.ITEM_STACK,
            () -> DSL.hook(
                    DSL.optionalFields(
                        "id",
                        References.ITEM_NAME.in(pSchema),
                        "tag",
                        DSL.optionalFields(
                            Pair.of("EntityTag", References.ENTITY_TREE.in(pSchema)),
                            Pair.of("BlockEntityTag", References.BLOCK_ENTITY.in(pSchema)),
                            Pair.of("CanDestroy", DSL.list(References.BLOCK_NAME.in(pSchema))),
                            Pair.of("CanPlaceOn", DSL.list(References.BLOCK_NAME.in(pSchema))),
                            Pair.of("Items", DSL.list(References.ITEM_STACK.in(pSchema))),
                            Pair.of("ChargedProjectiles", DSL.list(References.ITEM_STACK.in(pSchema)))
                        )
                    ),
                    V99.ADD_NAMES,
                    HookFunction.IDENTITY
                )
        );
    }
}

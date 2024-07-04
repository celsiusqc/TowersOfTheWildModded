package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1022 extends Schema {
    public V1022(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(false, References.RECIPE, () -> DSL.constType(NamespacedSchema.namespacedString()));
        pSchema.registerType(
            false,
            References.PLAYER,
            () -> DSL.optionalFields(
                    Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(pSchema))),
                    Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(pSchema))),
                    Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(pSchema))),
                    Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(pSchema)),
                    Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(pSchema)),
                    Pair.of(
                        "recipeBook",
                        DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(pSchema)), "toBeDisplayed", DSL.list(References.RECIPE.in(pSchema)))
                    )
                )
        );
        pSchema.registerType(false, References.HOTBAR, () -> DSL.compoundList(DSL.list(References.ITEM_STACK.in(pSchema))));
    }
}

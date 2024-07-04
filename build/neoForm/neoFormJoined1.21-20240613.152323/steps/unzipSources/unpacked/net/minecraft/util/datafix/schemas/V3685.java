package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3685 extends NamespacedSchema {
    public V3685(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    protected static TypeTemplate abstractArrow(Schema pSchema) {
        return DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema), "item", References.ITEM_STACK.in(pSchema));
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.register(map, "minecraft:trident", () -> abstractArrow(pSchema));
        pSchema.register(map, "minecraft:spectral_arrow", () -> abstractArrow(pSchema));
        pSchema.register(map, "minecraft:arrow", () -> abstractArrow(pSchema));
        return map;
    }
}

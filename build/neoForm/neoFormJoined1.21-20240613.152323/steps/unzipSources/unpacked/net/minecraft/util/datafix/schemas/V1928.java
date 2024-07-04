package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1928 extends NamespacedSchema {
    public V1928(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> V100.equipment(pSchema));
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        map.remove("minecraft:illager_beast");
        registerMob(pSchema, map, "minecraft:ravager");
        return map;
    }
}

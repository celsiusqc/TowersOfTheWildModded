package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1451_3 extends NamespacedSchema {
    public V1451_3(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.registerSimple(map, "minecraft:egg");
        pSchema.registerSimple(map, "minecraft:ender_pearl");
        pSchema.registerSimple(map, "minecraft:fireball");
        pSchema.register(map, "minecraft:potion", p_17450_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(pSchema)));
        pSchema.registerSimple(map, "minecraft:small_fireball");
        pSchema.registerSimple(map, "minecraft:snowball");
        pSchema.registerSimple(map, "minecraft:wither_skull");
        pSchema.registerSimple(map, "minecraft:xp_bottle");
        pSchema.register(map, "minecraft:arrow", () -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
        pSchema.register(
            map, "minecraft:enderman", () -> DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(pSchema), V100.equipment(pSchema))
        );
        pSchema.register(
            map,
            "minecraft:falling_block",
            () -> DSL.optionalFields("BlockState", References.BLOCK_STATE.in(pSchema), "TileEntityData", References.BLOCK_ENTITY.in(pSchema))
        );
        pSchema.register(map, "minecraft:spectral_arrow", () -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
        pSchema.register(
            map,
            "minecraft:chest_minecart",
            () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        pSchema.register(map, "minecraft:commandblock_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
        pSchema.register(map, "minecraft:furnace_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
        pSchema.register(
            map,
            "minecraft:hopper_minecart",
            () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        pSchema.register(map, "minecraft:minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
        pSchema.register(
            map,
            "minecraft:spawner_minecart",
            () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), References.UNTAGGED_SPAWNER.in(pSchema))
        );
        pSchema.register(map, "minecraft:tnt_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
        return map;
    }
}

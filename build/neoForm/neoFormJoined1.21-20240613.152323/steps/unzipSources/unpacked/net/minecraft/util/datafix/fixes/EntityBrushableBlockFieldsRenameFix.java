package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityBrushableBlockFieldsRenameFix extends NamedEntityFix {
    public EntityBrushableBlockFieldsRenameFix(Schema pOutputSchema) {
        super(pOutputSchema, false, "EntityBrushableBlockFieldsRenameFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
    }

    public Dynamic<?> fixTag(Dynamic<?> p_277830_) {
        return p_277830_.renameField("loot_table", "LootTable").renameField("loot_table_seed", "LootTableSeed");
    }

    @Override
    protected Typed<?> fix(Typed<?> pTyped) {
        return pTyped.update(DSL.remainderFinder(), this::fixTag);
    }
}

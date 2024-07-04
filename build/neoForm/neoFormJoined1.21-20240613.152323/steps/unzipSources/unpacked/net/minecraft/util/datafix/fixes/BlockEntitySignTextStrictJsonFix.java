package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class BlockEntitySignTextStrictJsonFix extends NamedEntityFix {
    public BlockEntitySignTextStrictJsonFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "BlockEntitySignTextStrictJsonFix", References.BLOCK_ENTITY, "Sign");
    }

    private Dynamic<?> updateLine(Dynamic<?> pDynamic, String pKey) {
        return pDynamic.update(pKey, ComponentDataFixUtils::rewriteFromLenient);
    }

    @Override
    protected Typed<?> fix(Typed<?> pTyped) {
        return pTyped.update(DSL.remainderFinder(), p_14869_ -> {
            p_14869_ = this.updateLine(p_14869_, "Text1");
            p_14869_ = this.updateLine(p_14869_, "Text2");
            p_14869_ = this.updateLine(p_14869_, "Text3");
            return this.updateLine(p_14869_, "Text4");
        });
    }
}

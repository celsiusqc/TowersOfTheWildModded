package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.function.UnaryOperator;

public class BlockEntityRenameFix extends DataFix {
    private final String name;
    private final UnaryOperator<String> nameChangeLookup;

    private BlockEntityRenameFix(Schema pOutputSchema, String pName, UnaryOperator<String> pNameChangeLookup) {
        super(pOutputSchema, true);
        this.name = pName;
        this.nameChangeLookup = pNameChangeLookup;
    }

    @Override
    public TypeRewriteRule makeRule() {
        TaggedChoiceType<String> taggedchoicetype = (TaggedChoiceType<String>)this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
        TaggedChoiceType<String> taggedchoicetype1 = (TaggedChoiceType<String>)this.getOutputSchema().findChoiceType(References.BLOCK_ENTITY);
        return this.fixTypeEverywhere(this.name, taggedchoicetype, taggedchoicetype1, p_277946_ -> p_277512_ -> p_277512_.mapFirst(this.nameChangeLookup));
    }

    public static DataFix create(Schema pOutputSchema, String pName, UnaryOperator<String> pNameChangeLookup) {
        return new BlockEntityRenameFix(pOutputSchema, pName, pNameChangeLookup);
    }
}

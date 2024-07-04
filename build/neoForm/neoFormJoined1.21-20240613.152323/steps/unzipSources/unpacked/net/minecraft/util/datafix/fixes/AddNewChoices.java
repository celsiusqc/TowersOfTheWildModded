package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.Locale;

public class AddNewChoices extends DataFix {
    private final String name;
    private final TypeReference type;

    public AddNewChoices(Schema pOutputSchema, String pName, TypeReference pType) {
        super(pOutputSchema, true);
        this.name = pName;
        this.type = pType;
    }

    @Override
    public TypeRewriteRule makeRule() {
        TaggedChoiceType<?> taggedchoicetype = this.getInputSchema().findChoiceType(this.type);
        TaggedChoiceType<?> taggedchoicetype1 = this.getOutputSchema().findChoiceType(this.type);
        return this.cap(taggedchoicetype, taggedchoicetype1);
    }

    private <K> TypeRewriteRule cap(TaggedChoiceType<K> pInputChoiceType, TaggedChoiceType<?> pOutputChoiceType) {
        if (pInputChoiceType.getKeyType() != pOutputChoiceType.getKeyType()) {
            throw new IllegalStateException("Could not inject: key type is not the same");
        } else {
            return this.fixTypeEverywhere(
                this.name,
                pInputChoiceType,
                (TaggedChoiceType<K>)pOutputChoiceType,
                p_14636_ -> p_339489_ -> {
                        if (!((TaggedChoiceType<K>)pOutputChoiceType).hasType(p_339489_.getFirst())) {
                            throw new IllegalArgumentException(
                                String.format(Locale.ROOT, "%s: Unknown type %s in '%s'", this.name, p_339489_.getFirst(), this.type.typeName())
                            );
                        } else {
                            return p_339489_;
                        }
                    }
            );
        }
    }
}

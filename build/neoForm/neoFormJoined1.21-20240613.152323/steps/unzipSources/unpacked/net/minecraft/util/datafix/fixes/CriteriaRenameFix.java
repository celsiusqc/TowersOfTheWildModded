package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class CriteriaRenameFix extends DataFix {
    private final String name;
    private final String advancementId;
    private final UnaryOperator<String> conversions;

    public CriteriaRenameFix(Schema pOutputSchema, String pName, String pAdvancementId, UnaryOperator<String> pConversions) {
        super(pOutputSchema, false);
        this.name = pName;
        this.advancementId = pAdvancementId;
        this.conversions = pConversions;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            this.name, this.getInputSchema().getType(References.ADVANCEMENTS), p_216590_ -> p_216590_.update(DSL.remainderFinder(), this::fixAdvancements)
        );
    }

    private Dynamic<?> fixAdvancements(Dynamic<?> p_216594_) {
        return p_216594_.update(
            this.advancementId,
            p_216599_ -> p_216599_.update(
                    "criteria",
                    p_216601_ -> p_216601_.updateMapValues(
                            p_216592_ -> p_216592_.mapFirst(
                                    p_337615_ -> DataFixUtils.orElse(
                                            p_337615_.asString().map(p_216597_ -> p_337615_.createString(this.conversions.apply(p_216597_))).result(),
                                            p_337615_
                                        )
                                )
                        )
                )
        );
    }
}

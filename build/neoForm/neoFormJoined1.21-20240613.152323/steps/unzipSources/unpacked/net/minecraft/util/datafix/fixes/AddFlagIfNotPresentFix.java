package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class AddFlagIfNotPresentFix extends DataFix {
    private final String name;
    private final boolean flagValue;
    private final String flagKey;
    private final TypeReference typeReference;

    public AddFlagIfNotPresentFix(Schema pOutputSchema, TypeReference pTypeReference, String pFlagKey, boolean pFlagValue) {
        super(pOutputSchema, true);
        this.flagValue = pFlagValue;
        this.flagKey = pFlagKey;
        this.name = "AddFlagIfNotPresentFix_" + this.flagKey + "=" + this.flagValue + " for " + pOutputSchema.getVersionKey();
        this.typeReference = pTypeReference;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(this.typeReference);
        return this.fixTypeEverywhereTyped(
            this.name,
            type,
            p_184815_ -> p_184815_.update(
                    DSL.remainderFinder(),
                    p_184817_ -> p_184817_.set(
                            this.flagKey, DataFixUtils.orElseGet(p_184817_.get(this.flagKey).result(), () -> p_184817_.createBoolean(this.flagValue))
                        )
                )
        );
    }
}

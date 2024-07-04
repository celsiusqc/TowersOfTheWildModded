package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;
import java.util.function.IntFunction;

public class EntityVariantFix extends NamedEntityFix {
    private final String fieldName;
    private final IntFunction<String> idConversions;

    public EntityVariantFix(Schema pOutputSchema, String pName, TypeReference pType, String pEntityName, String pFieldName, IntFunction<String> pIdConversions) {
        super(pOutputSchema, false, pName, pType, pEntityName);
        this.fieldName = pFieldName;
        this.idConversions = pIdConversions;
    }

    private static <T> Dynamic<T> updateAndRename(Dynamic<T> pDynamic, String pFieldName, String p_216639_, Function<Dynamic<T>, Dynamic<T>> pFixer) {
        return pDynamic.map(
            p_337624_ -> {
                DynamicOps<T> dynamicops = pDynamic.getOps();
                Function<T, T> function = p_216656_ -> pFixer.apply(new Dynamic<>(dynamicops, p_216656_)).getValue();
                return dynamicops.get((T)p_337624_, pFieldName)
                    .map(p_216652_ -> dynamicops.set((T)p_337624_, p_216639_, function.apply((T)p_216652_)))
                    .result()
                    .orElse((T)p_337624_);
            }
        );
    }

    @Override
    protected Typed<?> fix(Typed<?> pTyped) {
        return pTyped.update(
            DSL.remainderFinder(),
            p_216632_ -> updateAndRename(
                    p_216632_,
                    this.fieldName,
                    "variant",
                    p_337619_ -> DataFixUtils.orElse(
                            p_337619_.asNumber().map(p_216635_ -> p_337619_.createString(this.idConversions.apply(p_216635_.intValue()))).result(), p_337619_
                        )
                )
        );
    }
}

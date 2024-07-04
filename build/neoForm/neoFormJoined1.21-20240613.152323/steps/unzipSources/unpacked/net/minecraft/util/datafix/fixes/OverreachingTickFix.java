package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;

public class OverreachingTickFix extends DataFix {
    public OverreachingTickFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.CHUNK);
        OpticFinder<?> opticfinder = type.findField("block_ticks");
        return this.fixTypeEverywhereTyped("Handle ticks saved in the wrong chunk", type, p_337660_ -> {
            Optional<? extends Typed<?>> optional = p_337660_.getOptionalTyped(opticfinder);
            Optional<? extends Dynamic<?>> optional1 = optional.isPresent() ? optional.get().write().result() : Optional.empty();
            return p_337660_.update(DSL.remainderFinder(), p_337658_ -> {
                int i = p_337658_.get("xPos").asInt(0);
                int j = p_337658_.get("zPos").asInt(0);
                Optional<? extends Dynamic<?>> optional2 = p_337658_.get("fluid_ticks").get().result();
                p_337658_ = extractOverreachingTicks(p_337658_, i, j, optional1, "neighbor_block_ticks");
                return extractOverreachingTicks(p_337658_, i, j, optional2, "neighbor_fluid_ticks");
            });
        });
    }

    private static Dynamic<?> extractOverreachingTicks(
        Dynamic<?> pTag, int pX, int pZ, Optional<? extends Dynamic<?>> p_207666_, String pId
    ) {
        if (p_207666_.isPresent()) {
            List<? extends Dynamic<?>> list = p_207666_.get().asStream().filter(p_207658_ -> {
                int i = p_207658_.get("x").asInt(0);
                int j = p_207658_.get("z").asInt(0);
                int k = Math.abs(pX - (i >> 4));
                int l = Math.abs(pZ - (j >> 4));
                return (k != 0 || l != 0) && k <= 1 && l <= 1;
            }).toList();
            if (!list.isEmpty()) {
                pTag = pTag.set("UpgradeData", pTag.get("UpgradeData").orElseEmptyMap().set(pId, pTag.createList(list.stream())));
            }
        }

        return pTag;
    }
}

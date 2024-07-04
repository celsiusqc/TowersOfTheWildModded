package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;

public class PrimedTntBlockStateFixer extends NamedEntityWriteReadFix {
    public PrimedTntBlockStateFixer(Schema pOutputSchema) {
        super(pOutputSchema, true, "PrimedTnt BlockState fixer", References.ENTITY, "minecraft:tnt");
    }

    private static <T> Dynamic<T> renameFuse(Dynamic<T> pTag) {
        Optional<Dynamic<T>> optional = pTag.get("Fuse").get().result();
        return optional.isPresent() ? pTag.set("fuse", optional.get()) : pTag;
    }

    private static <T> Dynamic<T> insertBlockState(Dynamic<T> pTag) {
        return pTag.set("block_state", pTag.createMap(Map.of(pTag.createString("Name"), pTag.createString("minecraft:tnt"))));
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> pTag) {
        return renameFuse(insertBlockState(pTag));
    }
}

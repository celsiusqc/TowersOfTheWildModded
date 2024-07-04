package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class LodestoneCompassComponentFix extends ItemStackComponentRemainderFix {
    public LodestoneCompassComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, "LodestoneCompassComponentFix", "minecraft:lodestone_target", "minecraft:lodestone_tracker");
    }

    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> pTag) {
        Optional<Dynamic<T>> optional = pTag.get("pos").result();
        Optional<Dynamic<T>> optional1 = pTag.get("dimension").result();
        pTag = pTag.remove("pos").remove("dimension");
        if (optional.isPresent() && optional1.isPresent()) {
            pTag = pTag.set("target", pTag.emptyMap().set("pos", optional.get()).set("dimension", optional1.get()));
        }

        return pTag;
    }
}

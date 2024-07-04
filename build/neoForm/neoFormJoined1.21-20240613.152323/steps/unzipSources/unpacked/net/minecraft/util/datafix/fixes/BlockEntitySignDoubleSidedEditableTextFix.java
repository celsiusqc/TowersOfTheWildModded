package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class BlockEntitySignDoubleSidedEditableTextFix extends NamedEntityFix {
    public static final String FILTERED_CORRECT = "_filtered_correct";
    private static final String DEFAULT_COLOR = "black";

    public BlockEntitySignDoubleSidedEditableTextFix(Schema pOutputSchema, String pName, String pEntityName) {
        super(pOutputSchema, false, pName, References.BLOCK_ENTITY, pEntityName);
    }

    private static <T> Dynamic<T> fixTag(Dynamic<T> p_278110_) {
        return p_278110_.set("front_text", fixFrontTextTag(p_278110_))
            .set("back_text", createDefaultText(p_278110_))
            .set("is_waxed", p_278110_.createBoolean(false));
    }

    private static <T> Dynamic<T> fixFrontTextTag(Dynamic<T> pTag) {
        Dynamic<T> dynamic = ComponentDataFixUtils.createEmptyComponent(pTag.getOps());
        List<Dynamic<T>> list = getLines(pTag, "Text").map(p_294721_ -> p_294721_.orElse(dynamic)).toList();
        Dynamic<T> dynamic1 = pTag.emptyMap()
            .set("messages", pTag.createList(list.stream()))
            .set("color", pTag.get("Color").result().orElse(pTag.createString("black")))
            .set("has_glowing_text", pTag.get("GlowingText").result().orElse(pTag.createBoolean(false)))
            .set("_filtered_correct", pTag.createBoolean(true));
        List<Optional<Dynamic<T>>> list1 = getLines(pTag, "FilteredText").toList();
        if (list1.stream().anyMatch(Optional::isPresent)) {
            dynamic1 = dynamic1.set("filtered_messages", pTag.createList(Streams.mapWithIndex(list1.stream(), (p_295046_, p_294135_) -> {
                Dynamic<T> dynamic2 = list.get((int)p_294135_);
                return p_295046_.orElse(dynamic2);
            })));
        }

        return dynamic1;
    }

    private static <T> Stream<Optional<Dynamic<T>>> getLines(Dynamic<T> pDynamic, String pPrefix) {
        return Stream.of(
            pDynamic.get(pPrefix + "1").result(),
            pDynamic.get(pPrefix + "2").result(),
            pDynamic.get(pPrefix + "3").result(),
            pDynamic.get(pPrefix + "4").result()
        );
    }

    private static <T> Dynamic<T> createDefaultText(Dynamic<T> pDynamic) {
        return pDynamic.emptyMap()
            .set("messages", createEmptyLines(pDynamic))
            .set("color", pDynamic.createString("black"))
            .set("has_glowing_text", pDynamic.createBoolean(false));
    }

    private static <T> Dynamic<T> createEmptyLines(Dynamic<T> pDynamic) {
        Dynamic<T> dynamic = ComponentDataFixUtils.createEmptyComponent(pDynamic.getOps());
        return pDynamic.createList(Stream.of(dynamic, dynamic, dynamic, dynamic));
    }

    @Override
    protected Typed<?> fix(Typed<?> pTyped) {
        return pTyped.update(DSL.remainderFinder(), BlockEntitySignDoubleSidedEditableTextFix::fixTag);
    }
}

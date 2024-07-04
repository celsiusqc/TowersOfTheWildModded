package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.slf4j.Logger;

public class ParticleUnflatteningFix extends DataFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public ParticleUnflatteningFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.PARTICLE);
        Type<?> type1 = this.getOutputSchema().getType(References.PARTICLE);
        return this.writeFixAndRead("ParticleUnflatteningFix", type, type1, this::fix);
    }

    private <T> Dynamic<T> fix(Dynamic<T> p_341243_) {
        Optional<String> optional = p_341243_.asString().result();
        if (optional.isEmpty()) {
            return p_341243_;
        } else {
            String s = optional.get();
            String[] astring = s.split(" ", 2);
            String s1 = NamespacedSchema.ensureNamespaced(astring[0]);
            Dynamic<T> dynamic = p_341243_.createMap(Map.of(p_341243_.createString("type"), p_341243_.createString(s1)));

            return switch (s1) {
                case "minecraft:item" -> astring.length > 1 ? this.updateItem(dynamic, astring[1]) : dynamic;
                case "minecraft:block", "minecraft:block_marker", "minecraft:falling_dust", "minecraft:dust_pillar" -> astring.length > 1
                ? this.updateBlock(dynamic, astring[1])
                : dynamic;
                case "minecraft:dust" -> astring.length > 1 ? this.updateDust(dynamic, astring[1]) : dynamic;
                case "minecraft:dust_color_transition" -> astring.length > 1 ? this.updateDustTransition(dynamic, astring[1]) : dynamic;
                case "minecraft:sculk_charge" -> astring.length > 1 ? this.updateSculkCharge(dynamic, astring[1]) : dynamic;
                case "minecraft:vibration" -> astring.length > 1 ? this.updateVibration(dynamic, astring[1]) : dynamic;
                case "minecraft:shriek" -> astring.length > 1 ? this.updateShriek(dynamic, astring[1]) : dynamic;
                default -> dynamic;
            };
        }
    }

    private <T> Dynamic<T> updateItem(Dynamic<T> pTag, String pItem) {
        int i = pItem.indexOf("{");
        Dynamic<T> dynamic = pTag.createMap(Map.of(pTag.createString("Count"), pTag.createInt(1)));
        if (i == -1) {
            dynamic = dynamic.set("id", pTag.createString(pItem));
        } else {
            dynamic = dynamic.set("id", pTag.createString(pItem.substring(0, i)));
            CompoundTag compoundtag = parseTag(pItem.substring(i));
            if (compoundtag != null) {
                dynamic = dynamic.set("tag", new Dynamic<>(NbtOps.INSTANCE, compoundtag).convert(pTag.getOps()));
            }
        }

        return pTag.set("item", dynamic);
    }

    @Nullable
    private static CompoundTag parseTag(String pTag) {
        try {
            return TagParser.parseTag(pTag);
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse tag: {}", pTag, exception);
            return null;
        }
    }

    private <T> Dynamic<T> updateBlock(Dynamic<T> pTag, String pBlock) {
        int i = pBlock.indexOf("[");
        Dynamic<T> dynamic = pTag.emptyMap();
        if (i == -1) {
            dynamic = dynamic.set("Name", pTag.createString(NamespacedSchema.ensureNamespaced(pBlock)));
        } else {
            dynamic = dynamic.set("Name", pTag.createString(NamespacedSchema.ensureNamespaced(pBlock.substring(0, i))));
            Map<Dynamic<T>, Dynamic<T>> map = parseBlockProperties(pTag, pBlock.substring(i));
            if (!map.isEmpty()) {
                dynamic = dynamic.set("Properties", pTag.createMap(map));
            }
        }

        return pTag.set("block_state", dynamic);
    }

    private static <T> Map<Dynamic<T>, Dynamic<T>> parseBlockProperties(Dynamic<T> pTag, String pProperties) {
        try {
            Map<Dynamic<T>, Dynamic<T>> map = new HashMap<>();
            StringReader stringreader = new StringReader(pProperties);
            stringreader.expect('[');
            stringreader.skipWhitespace();

            while (stringreader.canRead() && stringreader.peek() != ']') {
                stringreader.skipWhitespace();
                String s = stringreader.readString();
                stringreader.skipWhitespace();
                stringreader.expect('=');
                stringreader.skipWhitespace();
                String s1 = stringreader.readString();
                stringreader.skipWhitespace();
                map.put(pTag.createString(s), pTag.createString(s1));
                if (stringreader.canRead()) {
                    if (stringreader.peek() != ',') {
                        break;
                    }

                    stringreader.skip();
                }
            }

            stringreader.expect(']');
            return map;
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse block properties: {}", pProperties, exception);
            return Map.of();
        }
    }

    private static <T> Dynamic<T> readVector(Dynamic<T> pTag, StringReader pReader) throws CommandSyntaxException {
        float f = pReader.readFloat();
        pReader.expect(' ');
        float f1 = pReader.readFloat();
        pReader.expect(' ');
        float f2 = pReader.readFloat();
        return pTag.createList(Stream.of(f, f1, f2).map(pTag::createFloat));
    }

    private <T> Dynamic<T> updateDust(Dynamic<T> pTag, String pOptions) {
        try {
            StringReader stringreader = new StringReader(pOptions);
            Dynamic<T> dynamic = readVector(pTag, stringreader);
            stringreader.expect(' ');
            float f = stringreader.readFloat();
            return pTag.set("color", dynamic).set("scale", pTag.createFloat(f));
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse particle options: {}", pOptions, exception);
            return pTag;
        }
    }

    private <T> Dynamic<T> updateDustTransition(Dynamic<T> pTag, String pOptions) {
        try {
            StringReader stringreader = new StringReader(pOptions);
            Dynamic<T> dynamic = readVector(pTag, stringreader);
            stringreader.expect(' ');
            float f = stringreader.readFloat();
            stringreader.expect(' ');
            Dynamic<T> dynamic1 = readVector(pTag, stringreader);
            return pTag.set("from_color", dynamic).set("to_color", dynamic1).set("scale", pTag.createFloat(f));
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse particle options: {}", pOptions, exception);
            return pTag;
        }
    }

    private <T> Dynamic<T> updateSculkCharge(Dynamic<T> pTag, String pOptions) {
        try {
            StringReader stringreader = new StringReader(pOptions);
            float f = stringreader.readFloat();
            return pTag.set("roll", pTag.createFloat(f));
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse particle options: {}", pOptions, exception);
            return pTag;
        }
    }

    private <T> Dynamic<T> updateVibration(Dynamic<T> pTag, String pOptions) {
        try {
            StringReader stringreader = new StringReader(pOptions);
            float f = (float)stringreader.readDouble();
            stringreader.expect(' ');
            float f1 = (float)stringreader.readDouble();
            stringreader.expect(' ');
            float f2 = (float)stringreader.readDouble();
            stringreader.expect(' ');
            int i = stringreader.readInt();
            Dynamic<T> dynamic = (Dynamic<T>)pTag.createIntList(IntStream.of(Mth.floor(f), Mth.floor(f1), Mth.floor(f2)));
            Dynamic<T> dynamic1 = pTag.createMap(
                Map.of(pTag.createString("type"), pTag.createString("minecraft:block"), pTag.createString("pos"), dynamic)
            );
            return pTag.set("destination", dynamic1).set("arrival_in_ticks", pTag.createInt(i));
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse particle options: {}", pOptions, exception);
            return pTag;
        }
    }

    private <T> Dynamic<T> updateShriek(Dynamic<T> pTag, String pOptions) {
        try {
            StringReader stringreader = new StringReader(pOptions);
            int i = stringreader.readInt();
            return pTag.set("delay", pTag.createInt(i));
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse particle options: {}", pOptions, exception);
            return pTag;
        }
    }
}

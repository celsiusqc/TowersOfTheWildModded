package net.minecraft.client.gui.font;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum FontOption implements StringRepresentable {
    UNIFORM("uniform"),
    JAPANESE_VARIANTS("jp");

    public static final Codec<FontOption> CODEC = StringRepresentable.fromEnum(FontOption::values);
    private final String name;

    private FontOption(String pName) {
        this.name = pName;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Filter {
        private final Map<FontOption, Boolean> values;
        public static final Codec<FontOption.Filter> CODEC = Codec.unboundedMap(FontOption.CODEC, Codec.BOOL)
            .xmap(FontOption.Filter::new, p_326230_ -> p_326230_.values);
        public static final FontOption.Filter ALWAYS_PASS = new FontOption.Filter(Map.of());

        public Filter(Map<FontOption, Boolean> p_325963_) {
            this.values = p_325963_;
        }

        public boolean apply(Set<FontOption> pOptions) {
            for (Entry<FontOption, Boolean> entry : this.values.entrySet()) {
                if (pOptions.contains(entry.getKey()) != entry.getValue()) {
                    return false;
                }
            }

            return true;
        }

        public FontOption.Filter merge(FontOption.Filter pFilter) {
            Map<FontOption, Boolean> map = new HashMap<>(pFilter.values);
            map.putAll(this.values);
            return new FontOption.Filter(Map.copyOf(map));
        }
    }
}

package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public record TagPredicate<T>(TagKey<T> tag, boolean expected) {
    public static <T> Codec<TagPredicate<T>> codec(ResourceKey<? extends Registry<T>> pRegistryKey) {
        return RecordCodecBuilder.create(
            p_299212_ -> p_299212_.group(
                        TagKey.codec(pRegistryKey).fieldOf("id").forGetter(TagPredicate::tag), Codec.BOOL.fieldOf("expected").forGetter(TagPredicate::expected)
                    )
                    .apply(p_299212_, TagPredicate::new)
        );
    }

    public static <T> TagPredicate<T> is(TagKey<T> pTag) {
        return new TagPredicate<>(pTag, true);
    }

    public static <T> TagPredicate<T> isNot(TagKey<T> pTag) {
        return new TagPredicate<>(pTag, false);
    }

    public boolean matches(Holder<T> pValue) {
        return pValue.is(this.tag) == this.expected;
    }
}

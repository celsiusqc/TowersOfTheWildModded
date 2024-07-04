package net.minecraft.network.chat.contents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class KeybindContents implements ComponentContents {
    public static final MapCodec<KeybindContents> CODEC = RecordCodecBuilder.mapCodec(
        p_304454_ -> p_304454_.group(Codec.STRING.fieldOf("keybind").forGetter(p_304386_ -> p_304386_.name)).apply(p_304454_, KeybindContents::new)
    );
    public static final ComponentContents.Type<KeybindContents> TYPE = new ComponentContents.Type<>(CODEC, "keybind");
    private final String name;
    @Nullable
    private Supplier<Component> nameResolver;

    public KeybindContents(String p_237347_) {
        this.name = p_237347_;
    }

    private Component getNestedComponent() {
        if (this.nameResolver == null) {
            this.nameResolver = KeybindResolver.keyResolver.apply(this.name);
        }

        return this.nameResolver.get();
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> pContentConsumer) {
        return this.getNestedComponent().visit(pContentConsumer);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> pStyledContentConsumer, Style pStyle) {
        return this.getNestedComponent().visit(pStyledContentConsumer, pStyle);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            if (pOther instanceof KeybindContents keybindcontents && this.name.equals(keybindcontents.name)) {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return "keybind{" + this.name + "}";
    }

    public String getName() {
        return this.name;
    }

    @Override
    public ComponentContents.Type<?> type() {
        return TYPE;
    }
}

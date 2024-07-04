package net.minecraft.network.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

public interface ComponentContents {
    default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> pStyledContentConsumer, Style pStyle) {
        return Optional.empty();
    }

    default <T> Optional<T> visit(FormattedText.ContentConsumer<T> pContentConsumer) {
        return Optional.empty();
    }

    default MutableComponent resolve(@Nullable CommandSourceStack pNbtPathPattern, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
        return MutableComponent.create(this);
    }

    ComponentContents.Type<?> type();

    public static record Type<T extends ComponentContents>(MapCodec<T> codec, String id) implements StringRepresentable {
        @Override
        public String getSerializedName() {
            return this.id;
        }
    }
}

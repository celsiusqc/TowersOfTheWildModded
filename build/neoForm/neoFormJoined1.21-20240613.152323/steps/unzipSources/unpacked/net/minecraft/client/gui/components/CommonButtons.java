package net.minecraft.client.gui.components;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommonButtons {
    public static SpriteIconButton language(int pWidth, Button.OnPress pOnPress, boolean pIconOnly) {
        return SpriteIconButton.builder(Component.translatable("options.language"), pOnPress, pIconOnly)
            .width(pWidth)
            .sprite(ResourceLocation.withDefaultNamespace("icon/language"), 15, 15)
            .build();
    }

    public static SpriteIconButton accessibility(int pWidth, Button.OnPress pOnPress, boolean pIconOnly) {
        Component component = pIconOnly
            ? Component.translatable("options.accessibility")
            : Component.translatable("accessibility.onboarding.accessibility.button");
        return SpriteIconButton.builder(component, pOnPress, pIconOnly)
            .width(pWidth)
            .sprite(ResourceLocation.withDefaultNamespace("icon/accessibility"), 15, 15)
            .build();
    }
}

package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ImageButton extends Button {
    protected final WidgetSprites sprites;

    public ImageButton(int pX, int pY, int pWidth, int pHeight, WidgetSprites pSprites, Button.OnPress pOnPress) {
        this(pX, pY, pWidth, pHeight, pSprites, pOnPress, CommonComponents.EMPTY);
    }

    public ImageButton(int pX, int pY, int pWidth, int pHeight, WidgetSprites pSprites, Button.OnPress pOnPress, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, DEFAULT_NARRATION);
        this.sprites = pSprites;
    }

    public ImageButton(int pWidth, int pHeight, WidgetSprites pSprites, Button.OnPress pOnPress, Component pMessage) {
        this(0, 0, pWidth, pHeight, pSprites, pOnPress, pMessage);
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        ResourceLocation resourcelocation = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
        pGuiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.width, this.height);
    }
}

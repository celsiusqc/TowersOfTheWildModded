package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ImageWidget extends AbstractWidget {
    ImageWidget(int pX, int pY, int pWidth, int pHeight) {
        super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY);
    }

    public static ImageWidget texture(int pWidth, int pHeight, ResourceLocation pTexture, int pTextureWidth, int pTextureHeight) {
        return new ImageWidget.Texture(0, 0, pWidth, pHeight, pTexture, pTextureWidth, pTextureHeight);
    }

    public static ImageWidget sprite(int pWidth, int pHeight, ResourceLocation pSprite) {
        return new ImageWidget.Sprite(0, 0, pWidth, pHeight, pSprite);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
    }

    @Override
    public void playDownSound(SoundManager pHandler) {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    /**
     * Retrieves the next focus path based on the given focus navigation event.
     * <p>
     * @return the next focus path as a ComponentPath, or {@code null} if there is no next focus path.
     *
     * @param pEvent the focus navigation event.
     */
    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    static class Sprite extends ImageWidget {
        private final ResourceLocation sprite;

        public Sprite(int pX, int pY, int pWidth, int pHeight, ResourceLocation pSprite) {
            super(pX, pY, pWidth, pHeight);
            this.sprite = pSprite;
        }

        @Override
        public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.blitSprite(this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Texture extends ImageWidget {
        private final ResourceLocation texture;
        private final int textureWidth;
        private final int textureHeight;

        public Texture(int pX, int pY, int pWidth, int pHeight, ResourceLocation pTexture, int pTextureWidth, int pTextureHeight) {
            super(pX, pY, pWidth, pHeight);
            this.texture = pTexture;
            this.textureWidth = pTextureWidth;
            this.textureHeight = pTextureHeight;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.blit(
                this.texture,
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight(),
                0.0F,
                0.0F,
                this.getWidth(),
                this.getHeight(),
                this.textureWidth,
                this.textureHeight
            );
        }
    }
}

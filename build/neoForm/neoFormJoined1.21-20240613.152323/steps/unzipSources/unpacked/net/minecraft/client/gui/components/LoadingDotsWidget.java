package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoadingDotsWidget extends AbstractWidget {
    private final Font font;

    public LoadingDotsWidget(Font pFont, Component pMessage) {
        super(0, 0, pFont.width(pMessage), 9 * 3, pMessage);
        this.font = pFont;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int i = this.getX() + this.getWidth() / 2;
        int j = this.getY() + this.getHeight() / 2;
        Component component = this.getMessage();
        pGuiGraphics.drawString(this.font, component, i - this.font.width(component) / 2, j - 9, -1, false);
        String s = LoadingDotsText.get(Util.getMillis());
        pGuiGraphics.drawString(this.font, s, i - this.font.width(s) / 2, j + 9, -8355712, false);
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
}

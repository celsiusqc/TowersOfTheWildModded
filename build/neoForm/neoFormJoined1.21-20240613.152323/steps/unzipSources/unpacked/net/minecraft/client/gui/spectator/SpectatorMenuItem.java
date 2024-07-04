package net.minecraft.client.gui.spectator;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpectatorMenuItem {
    void selectItem(SpectatorMenu pMenu);

    Component getName();

    void renderIcon(GuiGraphics pGuiGraphics, float pShadeColor, int pAlpha);

    boolean isEnabled();
}

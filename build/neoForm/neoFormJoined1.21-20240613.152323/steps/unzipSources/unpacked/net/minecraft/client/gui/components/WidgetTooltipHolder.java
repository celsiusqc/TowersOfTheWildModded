package net.minecraft.client.gui.components;

import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WidgetTooltipHolder {
    @Nullable
    private Tooltip tooltip;
    private Duration delay = Duration.ZERO;
    private long displayStartTime;
    private boolean wasDisplayed;

    public void setDelay(Duration pDelay) {
        this.delay = pDelay;
    }

    public void set(@Nullable Tooltip pTooltip) {
        this.tooltip = pTooltip;
    }

    @Nullable
    public Tooltip get() {
        return this.tooltip;
    }

    public void refreshTooltipForNextRenderPass(boolean pHovering, boolean pFocused, ScreenRectangle pScreenRectangle) {
        if (this.tooltip == null) {
            this.wasDisplayed = false;
        } else {
            boolean flag = pHovering || pFocused && Minecraft.getInstance().getLastInputType().isKeyboard();
            if (flag != this.wasDisplayed) {
                if (flag) {
                    this.displayStartTime = Util.getMillis();
                }

                this.wasDisplayed = flag;
            }

            if (flag && Util.getMillis() - this.displayStartTime > this.delay.toMillis()) {
                Screen screen = Minecraft.getInstance().screen;
                if (screen != null) {
                    screen.setTooltipForNextRenderPass(this.tooltip, this.createTooltipPositioner(pScreenRectangle, pHovering, pFocused), pFocused);
                }
            }
        }
    }

    private ClientTooltipPositioner createTooltipPositioner(ScreenRectangle pScreenRectangle, boolean pHovering, boolean pFocused) {
        return (ClientTooltipPositioner)(!pHovering && pFocused && Minecraft.getInstance().getLastInputType().isKeyboard()
            ? new BelowOrAboveWidgetTooltipPositioner(pScreenRectangle)
            : new MenuTooltipPositioner(pScreenRectangle));
    }

    public void updateNarration(NarrationElementOutput pOutput) {
        if (this.tooltip != null) {
            this.tooltip.updateNarration(pOutput);
        }
    }
}

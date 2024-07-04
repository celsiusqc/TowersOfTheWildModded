package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StateSwitchingButton extends AbstractWidget {
    @Nullable
    protected WidgetSprites sprites;
    protected boolean isStateTriggered;

    public StateSwitchingButton(int pX, int pY, int pWidth, int pHeight, boolean pInitialState) {
        super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY);
        this.isStateTriggered = pInitialState;
    }

    public void initTextureValues(WidgetSprites pSprites) {
        this.sprites = pSprites;
    }

    public void setStateTriggered(boolean pTriggered) {
        this.isStateTriggered = pTriggered;
    }

    public boolean isStateTriggered() {
        return this.isStateTriggered;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        this.defaultButtonNarrationText(pNarrationElementOutput);
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.sprites != null) {
            RenderSystem.disableDepthTest();
            pGuiGraphics.blitSprite(this.sprites.get(this.isStateTriggered, this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height);
            RenderSystem.enableDepthTest();
        }
    }
}

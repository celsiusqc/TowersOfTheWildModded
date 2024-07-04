package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ClientTextTooltip implements ClientTooltipComponent {
    private final FormattedCharSequence text;

    public ClientTextTooltip(FormattedCharSequence pText) {
        this.text = pText;
    }

    @Override
    public int getWidth(Font pFont) {
        return pFont.width(this.text);
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void renderText(Font pFont, int pMouseX, int pMouseY, Matrix4f pMatrix, MultiBufferSource.BufferSource pBufferSource) {
        pFont.drawInBatch(this.text, (float)pMouseX, (float)pMouseY, -1, true, pMatrix, pBufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }
}

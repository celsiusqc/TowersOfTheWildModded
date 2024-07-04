package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.debugchart.SampleStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FpsDebugChart extends AbstractDebugChart {
    private static final int RED = -65536;
    private static final int YELLOW = -256;
    private static final int GREEN = -16711936;
    private static final int CHART_TOP_FPS = 30;
    private static final double CHART_TOP_VALUE = 33.333333333333336;

    public FpsDebugChart(Font pFont, SampleStorage pSampleStorage) {
        super(pFont, pSampleStorage);
    }

    @Override
    protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
        this.drawStringWithShade(pGuiGraphics, "30 FPS", pX + 1, pHeight - 60 + 1);
        this.drawStringWithShade(pGuiGraphics, "60 FPS", pX + 1, pHeight - 30 + 1);
        pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, pHeight - 30, -1);
        int i = Minecraft.getInstance().options.framerateLimit().get();
        if (i > 0 && i <= 250) {
            pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, pHeight - this.getSampleHeight(1.0E9 / (double)i) - 1, -16711681);
        }
    }

    @Override
    protected String toDisplayString(double pValue) {
        return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(pValue)));
    }

    @Override
    protected int getSampleHeight(double pValue) {
        return (int)Math.round(toMilliseconds(pValue) * 60.0 / 33.333333333333336);
    }

    @Override
    protected int getSampleColor(long pValue) {
        return this.getSampleColor(toMilliseconds((double)pValue), 0.0, -16711936, 28.0, -256, 56.0, -65536);
    }

    private static double toMilliseconds(double pValue) {
        return pValue / 1000000.0;
    }
}

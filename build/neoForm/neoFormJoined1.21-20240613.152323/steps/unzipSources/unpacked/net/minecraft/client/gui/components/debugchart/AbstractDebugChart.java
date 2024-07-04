package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractDebugChart {
    protected static final int COLOR_GREY = 14737632;
    protected static final int CHART_HEIGHT = 60;
    protected static final int LINE_WIDTH = 1;
    protected final Font font;
    protected final SampleStorage sampleStorage;

    protected AbstractDebugChart(Font pFont, SampleStorage pSampleStorage) {
        this.font = pFont;
        this.sampleStorage = pSampleStorage;
    }

    public int getWidth(int pMaxWidth) {
        return Math.min(this.sampleStorage.capacity() + 2, pMaxWidth);
    }

    public void drawChart(GuiGraphics pGuiGraphics, int pX, int pWidth) {
        int i = pGuiGraphics.guiHeight();
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, i - 60, pX + pWidth, i, -1873784752);
        long j = 0L;
        long k = 2147483647L;
        long l = -2147483648L;
        int i1 = Math.max(0, this.sampleStorage.capacity() - (pWidth - 2));
        int j1 = this.sampleStorage.size() - i1;

        for (int k1 = 0; k1 < j1; k1++) {
            int l1 = pX + k1 + 1;
            int i2 = i1 + k1;
            long j2 = this.getValueForAggregation(i2);
            k = Math.min(k, j2);
            l = Math.max(l, j2);
            j += j2;
            this.drawDimensions(pGuiGraphics, i, l1, i2);
        }

        pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, i - 60, -1);
        pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, i - 1, -1);
        pGuiGraphics.vLine(RenderType.guiOverlay(), pX, i - 60, i, -1);
        pGuiGraphics.vLine(RenderType.guiOverlay(), pX + pWidth - 1, i - 60, i, -1);
        if (j1 > 0) {
            String s = this.toDisplayString((double)k) + " min";
            String s1 = this.toDisplayString((double)j / (double)j1) + " avg";
            String s2 = this.toDisplayString((double)l) + " max";
            pGuiGraphics.drawString(this.font, s, pX + 2, i - 60 - 9, 14737632);
            pGuiGraphics.drawCenteredString(this.font, s1, pX + pWidth / 2, i - 60 - 9, 14737632);
            pGuiGraphics.drawString(this.font, s2, pX + pWidth - this.font.width(s2) - 2, i - 60 - 9, 14737632);
        }

        this.renderAdditionalLinesAndLabels(pGuiGraphics, pX, pWidth, i);
    }

    protected void drawDimensions(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
        this.drawMainDimension(pGuiGraphics, pHeight, pX, pIndex);
        this.drawAdditionalDimensions(pGuiGraphics, pHeight, pX, pIndex);
    }

    protected void drawMainDimension(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
        long i = this.sampleStorage.get(pIndex);
        int j = this.getSampleHeight((double)i);
        int k = this.getSampleColor(i);
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, pHeight - j, pX + 1, pHeight, k);
    }

    protected void drawAdditionalDimensions(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
    }

    protected long getValueForAggregation(int pIndex) {
        return this.sampleStorage.get(pIndex);
    }

    protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
    }

    protected void drawStringWithShade(GuiGraphics pGuiGraphics, String pText, int pX, int pY) {
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, pY, pX + this.font.width(pText) + 1, pY + 9, -1873784752);
        pGuiGraphics.drawString(this.font, pText, pX + 1, pY + 1, 14737632, false);
    }

    protected abstract String toDisplayString(double pValue);

    protected abstract int getSampleHeight(double pValue);

    protected abstract int getSampleColor(long pValue);

    protected int getSampleColor(double pValue, double pMinPosition, int pMinColor, double pMidPosition, int pMidColor, double pMaxPosition, int pGuiGraphics) {
        pValue = Mth.clamp(pValue, pMinPosition, pMaxPosition);
        return pValue < pMidPosition
            ? FastColor.ARGB32.lerp((float)((pValue - pMinPosition) / (pMidPosition - pMinPosition)), pMinColor, pMidColor)
            : FastColor.ARGB32.lerp((float)((pValue - pMidPosition) / (pMaxPosition - pMidPosition)), pMidColor, pGuiGraphics);
    }
}

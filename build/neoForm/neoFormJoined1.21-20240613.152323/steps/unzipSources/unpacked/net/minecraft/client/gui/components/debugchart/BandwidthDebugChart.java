package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BandwidthDebugChart extends AbstractDebugChart {
    private static final int MIN_COLOR = -16711681;
    private static final int MID_COLOR = -6250241;
    private static final int MAX_COLOR = -65536;
    private static final int KILOBYTE = 1024;
    private static final int MEGABYTE = 1048576;
    private static final int CHART_TOP_VALUE = 1048576;

    public BandwidthDebugChart(Font pFont, SampleStorage pSampleStorage) {
        super(pFont, pSampleStorage);
    }

    @Override
    protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
        this.drawLabeledLineAtValue(pGuiGraphics, pX, pWidth, pHeight, 64);
        this.drawLabeledLineAtValue(pGuiGraphics, pX, pWidth, pHeight, 1024);
        this.drawLabeledLineAtValue(pGuiGraphics, pX, pWidth, pHeight, 16384);
        this.drawStringWithShade(pGuiGraphics, toDisplayStringInternal(1048576.0), pX + 1, pHeight - getSampleHeightInternal(1048576.0) + 1);
    }

    private void drawLabeledLineAtValue(GuiGraphics pGuiGraphics, int pX, int pWidth, int pY, int pValue) {
        this.drawLineWithLabel(
            pGuiGraphics, pX, pWidth, pY - getSampleHeightInternal((double)pValue), toDisplayStringInternal((double)pValue)
        );
    }

    private void drawLineWithLabel(GuiGraphics pGuiGraphics, int pX, int pWidth, int pY, String pText) {
        this.drawStringWithShade(pGuiGraphics, pText, pX + 1, pY + 1);
        pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, pY, -1);
    }

    @Override
    protected String toDisplayString(double pValue) {
        return toDisplayStringInternal(toBytesPerSecond(pValue));
    }

    private static String toDisplayStringInternal(double pValue) {
        if (pValue >= 1048576.0) {
            return String.format(Locale.ROOT, "%.1f MiB/s", pValue / 1048576.0);
        } else {
            return pValue >= 1024.0
                ? String.format(Locale.ROOT, "%.1f KiB/s", pValue / 1024.0)
                : String.format(Locale.ROOT, "%d B/s", Mth.floor(pValue));
        }
    }

    @Override
    protected int getSampleHeight(double pValue) {
        return getSampleHeightInternal(toBytesPerSecond(pValue));
    }

    private static int getSampleHeightInternal(double pValue) {
        return (int)Math.round(Math.log(pValue + 1.0) * 60.0 / Math.log(1048576.0));
    }

    @Override
    protected int getSampleColor(long pValue) {
        return this.getSampleColor(toBytesPerSecond((double)pValue), 0.0, -16711681, 8192.0, -6250241, 1.048576E7, -65536);
    }

    private static double toBytesPerSecond(double pBytesPerTick) {
        return pBytesPerTick * 20.0;
    }
}

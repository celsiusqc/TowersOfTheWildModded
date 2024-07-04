package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TpsDebugChart extends AbstractDebugChart {
    private static final int RED = -65536;
    private static final int YELLOW = -256;
    private static final int GREEN = -16711936;
    private static final int TICK_METHOD_COLOR = -6745839;
    private static final int TASK_COLOR = -4548257;
    private static final int OTHER_COLOR = -10547572;
    private final Supplier<Float> msptSupplier;

    public TpsDebugChart(Font pFont, SampleStorage pSampleStorage, Supplier<Float> pMsptSupplier) {
        super(pFont, pSampleStorage);
        this.msptSupplier = pMsptSupplier;
    }

    @Override
    protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
        float f = (float)TimeUtil.MILLISECONDS_PER_SECOND / this.msptSupplier.get();
        this.drawStringWithShade(pGuiGraphics, String.format("%.1f TPS", f), pX + 1, pHeight - 60 + 1);
    }

    @Override
    protected void drawAdditionalDimensions(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
        long i = this.sampleStorage.get(pIndex, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
        int j = this.getSampleHeight((double)i);
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, pHeight - j, pX + 1, pHeight, -6745839);
        long k = this.sampleStorage.get(pIndex, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
        int l = this.getSampleHeight((double)k);
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, pHeight - j - l, pX + 1, pHeight - j, -4548257);
        long i1 = this.sampleStorage.get(pIndex) - this.sampleStorage.get(pIndex, TpsDebugDimensions.IDLE.ordinal()) - i - k;
        int j1 = this.getSampleHeight((double)i1);
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, pHeight - j1 - l - j, pX + 1, pHeight - l - j, -10547572);
    }

    @Override
    protected long getValueForAggregation(int pIndex) {
        return this.sampleStorage.get(pIndex) - this.sampleStorage.get(pIndex, TpsDebugDimensions.IDLE.ordinal());
    }

    @Override
    protected String toDisplayString(double pValue) {
        return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(pValue)));
    }

    @Override
    protected int getSampleHeight(double pValue) {
        return (int)Math.round(toMilliseconds(pValue) * 60.0 / (double)this.msptSupplier.get().floatValue());
    }

    @Override
    protected int getSampleColor(long pValue) {
        float f = this.msptSupplier.get();
        return this.getSampleColor(toMilliseconds((double)pValue), (double)f, -16711936, (double)f * 1.125, -256, (double)f * 1.25, -65536);
    }

    private static double toMilliseconds(double pValue) {
        return pValue / 1000000.0;
    }
}

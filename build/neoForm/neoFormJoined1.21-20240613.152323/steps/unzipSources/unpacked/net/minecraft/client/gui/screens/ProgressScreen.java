package net.minecraft.client.gui.screens;

import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProgressListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProgressScreen extends Screen implements ProgressListener {
    @Nullable
    private Component header;
    @Nullable
    private Component stage;
    private int progress;
    private boolean stop;
    private final boolean clearScreenAfterStop;

    public ProgressScreen(boolean pClearScreenAfterStop) {
        super(GameNarrator.NO_TITLE);
        this.clearScreenAfterStop = pClearScreenAfterStop;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    @Override
    public void progressStartNoAbort(Component pComponent) {
        this.progressStart(pComponent);
    }

    @Override
    public void progressStart(Component pComponent) {
        this.header = pComponent;
        this.progressStage(Component.translatable("menu.working"));
    }

    @Override
    public void progressStage(Component pComponent) {
        this.stage = pComponent;
        this.progressStagePercentage(0);
    }

    /**
     * Updates the progress bar on the loading screen to the specified amount.
     */
    @Override
    public void progressStagePercentage(int pProgress) {
        this.progress = pProgress;
    }

    @Override
    public void stop() {
        this.stop = true;
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param pGuiGraphics the GuiGraphics object used for rendering.
     * @param pMouseX      the x-coordinate of the mouse cursor.
     * @param pMouseY      the y-coordinate of the mouse cursor.
     * @param pPartialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.stop) {
            if (this.clearScreenAfterStop) {
                this.minecraft.setScreen(null);
            }
        } else {
            super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            if (this.header != null) {
                pGuiGraphics.drawCenteredString(this.font, this.header, this.width / 2, 70, 16777215);
            }

            if (this.stage != null && this.progress != 0) {
                pGuiGraphics.drawCenteredString(this.font, Component.empty().append(this.stage).append(" " + this.progress + "%"), this.width / 2, 90, 16777215);
            }
        }
    }
}

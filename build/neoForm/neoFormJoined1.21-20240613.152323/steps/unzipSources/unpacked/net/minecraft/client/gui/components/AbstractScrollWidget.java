package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractScrollWidget extends AbstractWidget implements Renderable, GuiEventListener {
    private static final WidgetSprites BACKGROUND_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("widget/text_field"), ResourceLocation.withDefaultNamespace("widget/text_field_highlighted")
    );
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final int INNER_PADDING = 4;
    private static final int SCROLL_BAR_WIDTH = 8;
    private double scrollAmount;
    private boolean scrolling;

    public AbstractScrollWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (!this.visible) {
            return false;
        } else {
            boolean flag = this.withinContentAreaPoint(pMouseX, pMouseY);
            boolean flag1 = this.scrollbarVisible()
                && pMouseX >= (double)(this.getX() + this.width)
                && pMouseX <= (double)(this.getX() + this.width + 8)
                && pMouseY >= (double)this.getY()
                && pMouseY < (double)(this.getY() + this.height);
            if (flag1 && pButton == 0) {
                this.scrolling = true;
                return true;
            } else {
                return flag || flag1;
            }
        }
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            this.scrolling = false;
        }

        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.visible && this.isFocused() && this.scrolling) {
            if (pMouseY < (double)this.getY()) {
                this.setScrollAmount(0.0);
            } else if (pMouseY > (double)(this.getY() + this.height)) {
                this.setScrollAmount((double)this.getMaxScrollAmount());
            } else {
                int i = this.getScrollBarHeight();
                double d0 = (double)Math.max(1, this.getMaxScrollAmount() / (this.height - i));
                this.setScrollAmount(this.scrollAmount + pDragY * d0);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (!this.visible) {
            return false;
        } else {
            this.setScrollAmount(this.scrollAmount - pScrollY * this.scrollRate());
            return true;
        }
    }

    /**
     * Called when a keyboard key is pressed within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pKeyCode   the key code of the pressed key.
     * @param pScanCode  the scan code of the pressed key.
     * @param pModifiers the keyboard modifiers.
     */
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        boolean flag = pKeyCode == 265;
        boolean flag1 = pKeyCode == 264;
        if (flag || flag1) {
            double d0 = this.scrollAmount;
            this.setScrollAmount(this.scrollAmount + (double)(flag ? -1 : 1) * this.scrollRate());
            if (d0 != this.scrollAmount) {
                return true;
            }
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.visible) {
            this.renderBackground(pGuiGraphics);
            pGuiGraphics.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0, -this.scrollAmount, 0.0);
            this.renderContents(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            pGuiGraphics.pose().popPose();
            pGuiGraphics.disableScissor();
            this.renderDecorations(pGuiGraphics);
        }
    }

    private int getScrollBarHeight() {
        return Mth.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
    }

    protected void renderDecorations(GuiGraphics pGuiGraphics) {
        if (this.scrollbarVisible()) {
            this.renderScrollBar(pGuiGraphics);
        }
    }

    protected int innerPadding() {
        return 4;
    }

    protected int totalInnerPadding() {
        return this.innerPadding() * 2;
    }

    protected double scrollAmount() {
        return this.scrollAmount;
    }

    protected void setScrollAmount(double pScrollAmount) {
        this.scrollAmount = Mth.clamp(pScrollAmount, 0.0, (double)this.getMaxScrollAmount());
    }

    protected int getMaxScrollAmount() {
        return Math.max(0, this.getContentHeight() - (this.height - 4));
    }

    private int getContentHeight() {
        return this.getInnerHeight() + 4;
    }

    protected void renderBackground(GuiGraphics pGuiGraphics) {
        this.renderBorder(pGuiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    protected void renderBorder(GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight) {
        ResourceLocation resourcelocation = BACKGROUND_SPRITES.get(this.isActive(), this.isFocused());
        pGuiGraphics.blitSprite(resourcelocation, pX, pY, pWidth, pHeight);
    }

    private void renderScrollBar(GuiGraphics pGuiGraphics) {
        int i = this.getScrollBarHeight();
        int j = this.getX() + this.width;
        int k = Math.max(this.getY(), (int)this.scrollAmount * (this.height - i) / this.getMaxScrollAmount() + this.getY());
        RenderSystem.enableBlend();
        pGuiGraphics.blitSprite(SCROLLER_SPRITE, j, k, 8, i);
        RenderSystem.disableBlend();
    }

    protected boolean withinContentAreaTopBottom(int pTop, int pBottom) {
        return (double)pBottom - this.scrollAmount >= (double)this.getY() && (double)pTop - this.scrollAmount <= (double)(this.getY() + this.height);
    }

    protected boolean withinContentAreaPoint(double pX, double pY) {
        return pX >= (double)this.getX()
            && pX < (double)(this.getX() + this.width)
            && pY >= (double)this.getY()
            && pY < (double)(this.getY() + this.height);
    }

    protected boolean scrollbarVisible() {
        return this.getInnerHeight() > this.getHeight();
    }

    public int scrollbarWidth() {
        return 8;
    }

    protected abstract int getInnerHeight();

    protected abstract double scrollRate();

    protected abstract void renderContents(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick);
}

package net.minecraft.client.gui.components;

import java.time.Duration;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractWidget implements Renderable, GuiEventListener, LayoutElement, NarratableEntry, net.neoforged.neoforge.client.extensions.IAbstractWidgetExtension {
    private static final double PERIOD_PER_SCROLLED_PIXEL = 0.5;
    private static final double MIN_SCROLL_PERIOD = 3.0;
    protected int width;
    protected int height;
    private int x;
    private int y;
    private Component message;
    protected boolean isHovered;
    public boolean active = true;
    public boolean visible = true;
    protected float alpha = 1.0F;
    private int tabOrderGroup;
    private boolean focused;
    private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

    public AbstractWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        this.x = pX;
        this.y = pY;
        this.width = pWidth;
        this.height = pHeight;
        this.message = pMessage;
    }

    @Override
    public int getHeight() {
        return this.height;
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
    public final void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.visible) {
            this.isHovered = pGuiGraphics.containsPointInScissor(pMouseX, pMouseY)
                && pMouseX >= this.getX()
                && pMouseY >= this.getY()
                && pMouseX < this.getX() + this.width
                && pMouseY < this.getY() + this.height;
            this.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            this.tooltip.refreshTooltipForNextRenderPass(this.isHovered(), this.isFocused(), this.getRectangle());
        }
    }

    public void setTooltip(@Nullable Tooltip pTooltip) {
        this.tooltip.set(pTooltip);
    }

    @Nullable
    public Tooltip getTooltip() {
        return this.tooltip.get();
    }

    public void setTooltipDelay(Duration pTooltipDelay) {
        this.tooltip.setDelay(pTooltipDelay);
    }

    protected MutableComponent createNarrationMessage() {
        return wrapDefaultNarrationMessage(this.getMessage());
    }

    public static MutableComponent wrapDefaultNarrationMessage(Component pMessage) {
        return Component.translatable("gui.narrate.button", pMessage);
    }

    protected abstract void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick);

    public static void renderScrollingString(
        GuiGraphics pGuiGraphics, Font pFont, Component pText, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor
    ) {
        renderScrollingString(pGuiGraphics, pFont, pText, (pMinX + pMaxX) / 2, pMinX, pMinY, pMaxX, pMaxY, pColor);
    }

    public static void renderScrollingString(
        GuiGraphics pGuiGraphics, Font pFont, Component pText, int pCenterX, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor
    ) {
        int i = pFont.width(pText);
        int j = (pMinY + pMaxY - 9) / 2 + 1;
        int k = pMaxX - pMinX;
        if (i > k) {
            int l = i - k;
            double d0 = (double)Util.getMillis() / 1000.0;
            double d1 = Math.max((double)l * 0.5, 3.0);
            double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
            double d3 = Mth.lerp(d2, 0.0, (double)l);
            pGuiGraphics.enableScissor(pMinX, pMinY, pMaxX, pMaxY);
            pGuiGraphics.drawString(pFont, pText, pMinX - (int)d3, j, pColor);
            pGuiGraphics.disableScissor();
        } else {
            int i1 = Mth.clamp(pCenterX, pMinX + i / 2, pMaxX - i / 2);
            pGuiGraphics.drawCenteredString(pFont, pText, i1, j, pColor);
        }
    }

    protected void renderScrollingString(GuiGraphics pGuiGraphics, Font pFont, int pWidth, int pColor) {
        int i = this.getX() + pWidth;
        int j = this.getX() + this.getWidth() - pWidth;
        renderScrollingString(pGuiGraphics, pFont, this.getMessage(), i, this.getY(), j, this.getY() + this.getHeight(), pColor);
    }

    /**
 * @deprecated Neo: Use {@link #onClick(double, double, int)} instead.
 */
    @Deprecated
    public void onClick(double pMouseX, double pMouseY) {
    }

    public void onRelease(double pMouseX, double pMouseY) {
    }

    protected void onDrag(double pMouseX, double pMouseY, double pDragX, double pDragY) {
    }

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was clicked.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(pButton)) {
                boolean flag = this.clicked(pMouseX, pMouseY);
                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onClick(pMouseX, pMouseY, pButton);
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was released.
     */
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (this.isValidClickButton(pButton)) {
            this.onRelease(pMouseX, pMouseY);
            return true;
        } else {
            return false;
        }
    }

    protected boolean isValidClickButton(int pButton) {
        return pButton == 0;
    }

    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that is being dragged.
     * @param pDragX  the X distance of the drag.
     * @param pDragY  the Y distance of the drag.
     */
    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.isValidClickButton(pButton)) {
            this.onDrag(pMouseX, pMouseY, pDragX, pDragY);
            return true;
        } else {
            return false;
        }
    }

    protected boolean clicked(double pMouseX, double pMouseY) {
        return this.active
            && this.visible
            && pMouseX >= (double)this.getX()
            && pMouseY >= (double)this.getY()
            && pMouseX < (double)(this.getX() + this.getWidth())
            && pMouseY < (double)(this.getY() + this.getHeight());
    }

    /**
     * Retrieves the next focus path based on the given focus navigation event.
     * <p>
     * @return the next focus path as a ComponentPath, or {@code null} if there is no next focus path.
     *
     * @param pEvent the focus navigation event.
     */
    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
        if (!this.active || !this.visible) {
            return null;
        } else {
            return !this.isFocused() ? ComponentPath.leaf(this) : null;
        }
    }

    /**
     * Checks if the given mouse coordinates are over the GUI element.
     * <p>
     * @return {@code true} if the mouse is over the GUI element, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     */
    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return this.active
            && this.visible
            && pMouseX >= (double)this.getX()
            && pMouseY >= (double)this.getY()
            && pMouseX < (double)(this.getX() + this.width)
            && pMouseY < (double)(this.getY() + this.height);
    }

    public void playDownSound(SoundManager pHandler) {
        pHandler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    public void setWidth(int pWidth) {
        this.width = pWidth;
    }

    public void setHeight(int pHeight) {
        this.height = pHeight;
    }

    public void setAlpha(float pAlpha) {
        this.alpha = pAlpha;
    }

    public void setMessage(Component pMessage) {
        this.message = pMessage;
    }

    public Component getMessage() {
        return this.message;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    public boolean isHovered() {
        return this.isHovered;
    }

    public boolean isHoveredOrFocused() {
        return this.isHovered() || this.isFocused();
    }

    @Override
    public boolean isActive() {
        return this.visible && this.active;
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param pFocused {@code true} to apply focus, {@code false} to remove focus
     */
    @Override
    public void setFocused(boolean pFocused) {
        this.focused = pFocused;
    }

    public static final int UNSET_FG_COLOR = -1;
    protected int packedFGColor = UNSET_FG_COLOR;
    public int getFGColor() {
        if (packedFGColor != UNSET_FG_COLOR) return packedFGColor;
        return this.active ? 16777215 : 10526880; // White : Light Grey
    }
    public void setFGColor(int color) {
        this.packedFGColor = color;
    }
    public void clearFGColor() {
        this.packedFGColor = UNSET_FG_COLOR;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        } else {
            return this.isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
        }
    }

    /**
     * Updates the narration output with the current narration information.
     *
     * @param pNarrationElementOutput the output to update with narration information.
     */
    @Override
    public final void updateNarration(NarrationElementOutput pNarrationElementOutput) {
        this.updateWidgetNarration(pNarrationElementOutput);
        this.tooltip.updateNarration(pNarrationElementOutput);
    }

    protected abstract void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput);

    protected void defaultButtonNarrationText(NarrationElementOutput pNarrationElementOutput) {
        pNarrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
            } else {
                pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
            }
        }
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public void setX(int pX) {
        this.x = pX;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public void setY(int pY) {
        this.y = pY;
    }

    public int getRight() {
        return this.getX() + this.getWidth();
    }

    public int getBottom() {
        return this.getY() + this.getHeight();
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> pConsumer) {
        pConsumer.accept(this);
    }

    public void setSize(int pWidth, int pHeight) {
        this.width = pWidth;
        this.height = pHeight;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }

    public void setRectangle(int pWidth, int pHeight, int pX, int pY) {
        this.setSize(pWidth, pHeight);
        this.setPosition(pX, pY);
    }

    @Override
    public int getTabOrderGroup() {
        return this.tabOrderGroup;
    }

    public void setTabOrderGroup(int pTabOrderGroup) {
        this.tabOrderGroup = pTabOrderGroup;
    }
}

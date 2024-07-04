package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractContainerWidget {
    protected static final int SCROLLBAR_WIDTH = 6;
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final ResourceLocation SCROLLER_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller_background");
    private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    protected final Minecraft minecraft;
    protected final int itemHeight;
    private final List<E> children = new AbstractSelectionList.TrackedList();
    protected boolean centerListVertically = true;
    private double scrollAmount;
    private boolean renderHeader;
    protected int headerHeight;
    private boolean scrolling;
    @Nullable
    private E selected;
    @Nullable
    private E hovered;

    public AbstractSelectionList(Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
        super(0, pY, pWidth, pHeight, CommonComponents.EMPTY);
        this.minecraft = pMinecraft;
        this.itemHeight = pItemHeight;
    }

    protected void setRenderHeader(boolean pRenderHeader, int pHeaderHeight) {
        this.renderHeader = pRenderHeader;
        this.headerHeight = pHeaderHeight;
        if (!pRenderHeader) {
            this.headerHeight = 0;
        }
    }

    public int getRowWidth() {
        return 220;
    }

    @Nullable
    public E getSelected() {
        return this.selected;
    }

    public void setSelected(@Nullable E pSelected) {
        this.selected = pSelected;
    }

    public E getFirstElement() {
        return this.children.get(0);
    }

    @Nullable
    public E getFocused() {
        return (E)super.getFocused();
    }

    @Override
    public final List<E> children() {
        return this.children;
    }

    protected void clearEntries() {
        this.children.clear();
        this.selected = null;
    }

    protected void replaceEntries(Collection<E> pEntries) {
        this.clearEntries();
        this.children.addAll(pEntries);
    }

    protected E getEntry(int pIndex) {
        return this.children().get(pIndex);
    }

    protected int addEntry(E pEntry) {
        this.children.add(pEntry);
        return this.children.size() - 1;
    }

    protected void addEntryToTop(E pEntry) {
        double d0 = (double)this.getMaxScroll() - this.getScrollAmount();
        this.children.add(0, pEntry);
        this.setScrollAmount((double)this.getMaxScroll() - d0);
    }

    protected boolean removeEntryFromTop(E pEntry) {
        double d0 = (double)this.getMaxScroll() - this.getScrollAmount();
        boolean flag = this.removeEntry(pEntry);
        this.setScrollAmount((double)this.getMaxScroll() - d0);
        return flag;
    }

    protected int getItemCount() {
        return this.children().size();
    }

    protected boolean isSelectedItem(int pIndex) {
        return Objects.equals(this.getSelected(), this.children().get(pIndex));
    }

    @Nullable
    protected final E getEntryAtPosition(double pMouseX, double pMouseY) {
        int i = this.getRowWidth() / 2;
        int j = this.getX() + this.width / 2;
        int k = j - i;
        int l = j + i;
        int i1 = Mth.floor(pMouseY - (double)this.getY()) - this.headerHeight + (int)this.getScrollAmount() - 4;
        int j1 = i1 / this.itemHeight;
        return pMouseX >= (double)k && pMouseX <= (double)l && j1 >= 0 && i1 >= 0 && j1 < this.getItemCount() ? this.children().get(j1) : null;
    }

    public void updateSize(int pWidth, HeaderAndFooterLayout pLayout) {
        this.updateSizeAndPosition(pWidth, pLayout.getContentHeight(), pLayout.getHeaderHeight());
    }

    public void updateSizeAndPosition(int pWidth, int pHeight, int pY) {
        this.setSize(pWidth, pHeight);
        this.setPosition(0, pY);
        this.clampScrollAmount();
    }

    protected int getMaxPosition() {
        return this.getItemCount() * this.itemHeight + this.headerHeight;
    }

    protected boolean clickedHeader(int pX, int pY) {
        return false;
    }

    protected void renderHeader(GuiGraphics pGuiGraphics, int pX, int pY) {
    }

    protected void renderDecorations(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.hovered = this.isMouseOver((double)pMouseX, (double)pMouseY) ? this.getEntryAtPosition((double)pMouseX, (double)pMouseY) : null;
        this.renderListBackground(pGuiGraphics);
        this.enableScissor(pGuiGraphics);
        if (this.renderHeader) {
            int i = this.getRowLeft();
            int j = this.getY() + 4 - (int)this.getScrollAmount();
            this.renderHeader(pGuiGraphics, i, j);
        }

        this.renderListItems(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.disableScissor();
        this.renderListSeparators(pGuiGraphics);
        if (this.scrollbarVisible()) {
            int l = this.getScrollbarPosition();
            int i1 = (int)((float)(this.height * this.height) / (float)this.getMaxPosition());
            i1 = Mth.clamp(i1, 32, this.height - 8);
            int k = (int)this.getScrollAmount() * (this.height - i1) / this.getMaxScroll() + this.getY();
            if (k < this.getY()) {
                k = this.getY();
            }

            RenderSystem.enableBlend();
            pGuiGraphics.blitSprite(SCROLLER_BACKGROUND_SPRITE, l, this.getY(), 6, this.getHeight());
            pGuiGraphics.blitSprite(SCROLLER_SPRITE, l, k, 6, i1);
            RenderSystem.disableBlend();
        }

        this.renderDecorations(pGuiGraphics, pMouseX, pMouseY);
        RenderSystem.disableBlend();
    }

    protected boolean scrollbarVisible() {
        return this.getMaxScroll() > 0;
    }

    protected void renderListSeparators(GuiGraphics pGuiGraphics) {
        RenderSystem.enableBlend();
        ResourceLocation resourcelocation = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
        ResourceLocation resourcelocation1 = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        pGuiGraphics.blit(resourcelocation, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
        pGuiGraphics.blit(resourcelocation1, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
        RenderSystem.disableBlend();
    }

    protected void renderListBackground(GuiGraphics pGuiGraphics) {
        RenderSystem.enableBlend();
        ResourceLocation resourcelocation = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
        pGuiGraphics.blit(
            resourcelocation,
            this.getX(),
            this.getY(),
            (float)this.getRight(),
            (float)(this.getBottom() + (int)this.getScrollAmount()),
            this.getWidth(),
            this.getHeight(),
            32,
            32
        );
        RenderSystem.disableBlend();
    }

    protected void enableScissor(GuiGraphics pGuiGraphics) {
        pGuiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
    }

    protected void centerScrollOn(E pEntry) {
        this.setScrollAmount((double)(this.children().indexOf(pEntry) * this.itemHeight + this.itemHeight / 2 - this.height / 2));
    }

    protected void ensureVisible(E pEntry) {
        int i = this.getRowTop(this.children().indexOf(pEntry));
        int j = i - this.getY() - 4 - this.itemHeight;
        if (j < 0) {
            this.scroll(j);
        }

        int k = this.getBottom() - i - this.itemHeight - this.itemHeight;
        if (k < 0) {
            this.scroll(-k);
        }
    }

    private void scroll(int pScroll) {
        this.setScrollAmount(this.getScrollAmount() + (double)pScroll);
    }

    public double getScrollAmount() {
        return this.scrollAmount;
    }

    public void setClampedScrollAmount(double p_345538_) {
        this.scrollAmount = Mth.clamp(p_345538_, 0.0, (double)this.getMaxScroll());
    }

    public void setScrollAmount(double pScroll) {
        this.setClampedScrollAmount(pScroll);
    }

    public void clampScrollAmount() {
        this.setClampedScrollAmount(this.getScrollAmount());
    }

    public int getMaxScroll() {
        return Math.max(0, this.getMaxPosition() - (this.height - 4));
    }

    protected void updateScrollingState(double pMouseX, double pMouseY, int pButton) {
        this.scrolling = pButton == 0 && pMouseX >= (double)this.getScrollbarPosition() && pMouseX < (double)(this.getScrollbarPosition() + 6);
    }

    protected int getScrollbarPosition() {
        return this.getDefaultScrollbarPosition();
    }

    protected int getDefaultScrollbarPosition() {
        return this.getRealRowRight() + this.getListOutlinePadding();
    }

    private int getListOutlinePadding() {
        return 10;
    }

    protected boolean isValidMouseClick(int pButton) {
        return pButton == 0;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (!this.isValidMouseClick(pButton)) {
            return false;
        } else {
            this.updateScrollingState(pMouseX, pMouseY, pButton);
            if (!this.isMouseOver(pMouseX, pMouseY)) {
                return false;
            } else {
                E e = this.getEntryAtPosition(pMouseX, pMouseY);
                if (e != null) {
                    if (e.mouseClicked(pMouseX, pMouseY, pButton)) {
                        E e1 = this.getFocused();
                        if (e1 != e && e1 instanceof ContainerEventHandler containereventhandler) {
                            containereventhandler.setFocused(null);
                        }

                        this.setFocused(e);
                        this.setDragging(true);
                        return true;
                    }
                } else if (this.clickedHeader(
                    (int)(pMouseX - (double)(this.getX() + this.width / 2 - this.getRowWidth() / 2)),
                    (int)(pMouseY - (double)this.getY()) + (int)this.getScrollAmount() - 4
                )) {
                    return true;
                }

                return this.scrolling;
            }
        }
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return this.getFocused() != null ? this.getFocused().mouseReleased(pMouseX, pMouseY, pButton) : false;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) {
            return true;
        } else if (pButton == 0 && this.scrolling) {
            if (pMouseY < (double)this.getY()) {
                this.setScrollAmount(0.0);
            } else if (pMouseY > (double)this.getBottom()) {
                this.setScrollAmount((double)this.getMaxScroll());
            } else {
                double d0 = (double)Math.max(1, this.getMaxScroll());
                int i = this.height;
                int j = Mth.clamp((int)((float)(i * i) / (float)this.getMaxPosition()), 32, i - 8);
                double d1 = Math.max(1.0, d0 / (double)(i - j));
                this.setScrollAmount(this.getScrollAmount() + pDragY * d1);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        this.setScrollAmount(this.getScrollAmount() - pScrollY * (double)this.itemHeight / 2.0);
        return true;
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param pFocused the focused GUI element.
     */
    @Override
    public void setFocused(@Nullable GuiEventListener pFocused) {
        super.setFocused(pFocused);
        int i = this.children.indexOf(pFocused);
        if (i >= 0) {
            E e = this.children.get(i);
            this.setSelected(e);
            if (this.minecraft.getLastInputType().isKeyboard()) {
                this.ensureVisible(e);
            }
        }
    }

    @Nullable
    protected E nextEntry(ScreenDirection pDirection) {
        return this.nextEntry(pDirection, p_93510_ -> true);
    }

    @Nullable
    protected E nextEntry(ScreenDirection pDirection, Predicate<E> pPredicate) {
        return this.nextEntry(pDirection, pPredicate, this.getSelected());
    }

    @Nullable
    protected E nextEntry(ScreenDirection pDirection, Predicate<E> pPredicate, @Nullable E pSelected) {
        int i = switch (pDirection) {
            case RIGHT, LEFT -> 0;
            case UP -> -1;
            case DOWN -> 1;
        };
        if (!this.children().isEmpty() && i != 0) {
            int j;
            if (pSelected == null) {
                j = i > 0 ? 0 : this.children().size() - 1;
            } else {
                j = this.children().indexOf(pSelected) + i;
            }

            for (int k = j; k >= 0 && k < this.children.size(); k += i) {
                E e = this.children().get(k);
                if (pPredicate.test(e)) {
                    return e;
                }
            }
        }

        return null;
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return pMouseY >= (double)this.getY()
            && pMouseY <= (double)this.getBottom()
            && pMouseX >= (double)this.getX()
            && pMouseX <= (double)this.getRight();
    }

    protected void renderListItems(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int i = this.getRowLeft();
        int j = this.getRowWidth();
        int k = this.itemHeight - 4;
        int l = this.getItemCount();

        for (int i1 = 0; i1 < l; i1++) {
            int j1 = this.getRowTop(i1);
            int k1 = this.getRowBottom(i1);
            if (k1 >= this.getY() && j1 <= this.getBottom()) {
                this.renderItem(pGuiGraphics, pMouseX, pMouseY, pPartialTick, i1, i, j1, j, k);
            }
        }
    }

    protected void renderItem(
        GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, int pIndex, int pLeft, int pTop, int pWidth, int pHeight
    ) {
        E e = this.getEntry(pIndex);
        e.renderBack(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, Objects.equals(this.hovered, e), pPartialTick);
        if (this.isSelectedItem(pIndex)) {
            int i = this.isFocused() ? -1 : -8355712;
            this.renderSelection(pGuiGraphics, pTop, pWidth, pHeight, i, -16777216);
        }

        e.render(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, Objects.equals(this.hovered, e), pPartialTick);
    }

    protected void renderSelection(GuiGraphics pGuiGraphics, int pTop, int pWidth, int pHeight, int pOuterColor, int pInnerColor) {
        int i = this.getX() + (this.width - pWidth) / 2;
        int j = this.getX() + (this.width + pWidth) / 2;
        pGuiGraphics.fill(i, pTop - 2, j, pTop + pHeight + 2, pOuterColor);
        pGuiGraphics.fill(i + 1, pTop - 1, j - 1, pTop + pHeight + 1, pInnerColor);
    }

    public int getRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
    }

    private int getRealRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2;
    }

    public int getRowRight() {
        return this.getRowLeft() + this.getRowWidth();
    }

    private int getRealRowRight() {
        return this.getRealRowLeft() + this.getRowWidth();
    }

    protected int getRowTop(int pIndex) {
        return this.getY() + 4 - (int)this.getScrollAmount() + pIndex * this.itemHeight + this.headerHeight;
    }

    protected int getRowBottom(int pIndex) {
        return this.getRowTop(pIndex) + this.itemHeight;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        } else {
            return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
        }
    }

    @Nullable
    protected E remove(int pIndex) {
        E e = this.children.get(pIndex);
        return this.removeEntry(this.children.get(pIndex)) ? e : null;
    }

    protected boolean removeEntry(E pEntry) {
        boolean flag = this.children.remove(pEntry);
        if (flag && pEntry == this.getSelected()) {
            this.setSelected(null);
        }

        return flag;
    }

    @Nullable
    protected E getHovered() {
        return this.hovered;
    }

    void bindEntryToSelf(AbstractSelectionList.Entry<E> pEntry) {
        pEntry.list = this;
    }

    protected void narrateListElementPosition(NarrationElementOutput pNarrationElementOutput, E pEntry) {
        List<E> list = this.children();
        if (list.size() > 1) {
            int i = list.indexOf(pEntry);
            if (i != -1) {
                pNarrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", i + 1, list.size()));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener {
        @Deprecated
        protected AbstractSelectionList<E> list;

        /**
         * Sets the focus state of the GUI element.
         *
         * @param pFocused {@code true} to apply focus, {@code false} to remove focus
         */
        @Override
        public void setFocused(boolean pFocused) {
        }

        @Override
        public boolean isFocused() {
            return this.list.getFocused() == this;
        }

        public abstract void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        );

        public void renderBack(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pIsMouseOver,
            float pPartialTick
        ) {
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
            return Objects.equals(this.list.getEntryAtPosition(pMouseX, pMouseY), this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class TrackedList extends AbstractList<E> {
        private final List<E> delegate = Lists.newArrayList();

        public E get(int pIndex) {
            return this.delegate.get(pIndex);
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        public E set(int pIndex, E pEntry) {
            E e = this.delegate.set(pIndex, pEntry);
            AbstractSelectionList.this.bindEntryToSelf(pEntry);
            return e;
        }

        public void add(int pIndex, E pEntry) {
            this.delegate.add(pIndex, pEntry);
            AbstractSelectionList.this.bindEntryToSelf(pEntry);
        }

        public E remove(int pIndex) {
            return this.delegate.remove(pIndex);
        }
    }
}

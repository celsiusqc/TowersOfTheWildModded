package net.minecraft.client.gui.screens.inventory;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BookViewScreen extends Screen {
    public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
    public static final int PAGE_TEXT_X_OFFSET = 36;
    public static final int PAGE_TEXT_Y_OFFSET = 30;
    public static final BookViewScreen.BookAccess EMPTY_ACCESS = new BookViewScreen.BookAccess(List.of());
    public static final ResourceLocation BOOK_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    protected static final int TEXT_WIDTH = 114;
    protected static final int TEXT_HEIGHT = 128;
    protected static final int IMAGE_WIDTH = 192;
    protected static final int IMAGE_HEIGHT = 192;
    private BookViewScreen.BookAccess bookAccess;
    private int currentPage;
    /**
     * Holds a copy of the page text, split into page width lines
     */
    private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
    private int cachedPage = -1;
    private Component pageMsg = CommonComponents.EMPTY;
    private PageButton forwardButton;
    private PageButton backButton;
    /**
     * Determines if a sound is played when the page is turned
     */
    private final boolean playTurnSound;

    public BookViewScreen(BookViewScreen.BookAccess pBookAccess) {
        this(pBookAccess, true);
    }

    public BookViewScreen() {
        this(EMPTY_ACCESS, false);
    }

    private BookViewScreen(BookViewScreen.BookAccess pBookAccess, boolean pPlayTurnSound) {
        super(GameNarrator.NO_TITLE);
        this.bookAccess = pBookAccess;
        this.playTurnSound = pPlayTurnSound;
    }

    public void setBookAccess(BookViewScreen.BookAccess pBookAccess) {
        this.bookAccess = pBookAccess;
        this.currentPage = Mth.clamp(this.currentPage, 0, pBookAccess.getPageCount());
        this.updateButtonVisibility();
        this.cachedPage = -1;
    }

    /**
     * Moves the book to the specified page and returns true if it exists, {@code false} otherwise.
     */
    public boolean setPage(int pPageNum) {
        int i = Mth.clamp(pPageNum, 0, this.bookAccess.getPageCount() - 1);
        if (i != this.currentPage) {
            this.currentPage = i;
            this.updateButtonVisibility();
            this.cachedPage = -1;
            return true;
        } else {
            return false;
        }
    }

    /**
     * I'm not sure why this exists. The function it calls is public and does all the work.
     */
    protected boolean forcePage(int pPageNum) {
        return this.setPage(pPageNum);
    }

    @Override
    protected void init() {
        this.createMenuControls();
        this.createPageControlButtons();
    }

    protected void createMenuControls() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_315823_ -> this.onClose()).bounds(this.width / 2 - 100, 196, 200, 20).build());
    }

    protected void createPageControlButtons() {
        int i = (this.width - 192) / 2;
        int j = 2;
        this.forwardButton = this.addRenderableWidget(new PageButton(i + 116, 159, true, p_98297_ -> this.pageForward(), this.playTurnSound));
        this.backButton = this.addRenderableWidget(new PageButton(i + 43, 159, false, p_98287_ -> this.pageBack(), this.playTurnSound));
        this.updateButtonVisibility();
    }

    private int getNumPages() {
        return this.bookAccess.getPageCount();
    }

    protected void pageBack() {
        if (this.currentPage > 0) {
            this.currentPage--;
        }

        this.updateButtonVisibility();
    }

    protected void pageForward() {
        if (this.currentPage < this.getNumPages() - 1) {
            this.currentPage++;
        }

        this.updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
        this.backButton.visible = this.currentPage > 0;
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
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else {
            switch (pKeyCode) {
                case 266:
                    this.backButton.onPress();
                    return true;
                case 267:
                    this.forwardButton.onPress();
                    return true;
                default:
                    return false;
            }
        }
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
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        int i = (this.width - 192) / 2;
        int j = 2;
        if (this.cachedPage != this.currentPage) {
            FormattedText formattedtext = this.bookAccess.getPage(this.currentPage);
            this.cachedPageComponents = this.font.split(formattedtext, 114);
            this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
        }

        this.cachedPage = this.currentPage;
        int i1 = this.font.width(this.pageMsg);
        pGuiGraphics.drawString(this.font, this.pageMsg, i - i1 + 192 - 44, 18, 0, false);
        int k = Math.min(128 / 9, this.cachedPageComponents.size());

        for (int l = 0; l < k; l++) {
            FormattedCharSequence formattedcharsequence = this.cachedPageComponents.get(l);
            pGuiGraphics.drawString(this.font, formattedcharsequence, i + 36, 32 + l * 9, 0, false);
        }

        Style style = this.getClickedComponentStyleAt((double)pMouseX, (double)pMouseY);
        if (style != null) {
            pGuiGraphics.renderComponentHoverEffect(this.font, style, pMouseX, pMouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderTransparentBackground(pGuiGraphics);
        pGuiGraphics.blit(BOOK_LOCATION, (this.width - 192) / 2, 2, 0, 0, 192, 192);
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
        if (pButton == 0) {
            Style style = this.getClickedComponentStyleAt(pMouseX, pMouseY);
            if (style != null && this.handleComponentClicked(style)) {
                return true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean handleComponentClicked(Style pStyle) {
        ClickEvent clickevent = pStyle.getClickEvent();
        if (clickevent == null) {
            return false;
        } else if (clickevent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            String s = clickevent.getValue();

            try {
                int i = Integer.parseInt(s) - 1;
                return this.forcePage(i);
            } catch (Exception exception) {
                return false;
            }
        } else {
            boolean flag = super.handleComponentClicked(pStyle);
            if (flag && clickevent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                this.closeScreen();
            }

            return flag;
        }
    }

    protected void closeScreen() {
        this.minecraft.setScreen(null);
    }

    @Nullable
    public Style getClickedComponentStyleAt(double pMouseX, double pMouseY) {
        if (this.cachedPageComponents.isEmpty()) {
            return null;
        } else {
            int i = Mth.floor(pMouseX - (double)((this.width - 192) / 2) - 36.0);
            int j = Mth.floor(pMouseY - 2.0 - 30.0);
            if (i >= 0 && j >= 0) {
                int k = Math.min(128 / 9, this.cachedPageComponents.size());
                if (i <= 114 && j < 9 * k + k) {
                    int l = j / 9;
                    if (l >= 0 && l < this.cachedPageComponents.size()) {
                        FormattedCharSequence formattedcharsequence = this.cachedPageComponents.get(l);
                        return this.minecraft.font.getSplitter().componentStyleAtWidth(formattedcharsequence, i);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record BookAccess(List<Component> pages) {
        public int getPageCount() {
            return this.pages.size();
        }

        public FormattedText getPage(int pPage) {
            return pPage >= 0 && pPage < this.getPageCount() ? this.pages.get(pPage) : FormattedText.EMPTY;
        }

        @Nullable
        public static BookViewScreen.BookAccess fromItem(ItemStack pStack) {
            boolean flag = Minecraft.getInstance().isTextFilteringEnabled();
            WrittenBookContent writtenbookcontent = pStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (writtenbookcontent != null) {
                return new BookViewScreen.BookAccess(writtenbookcontent.getPages(flag));
            } else {
                WritableBookContent writablebookcontent = pStack.get(DataComponents.WRITABLE_BOOK_CONTENT);
                return writablebookcontent != null ? new BookViewScreen.BookAccess(writablebookcontent.getPages(flag).<Component>map(Component::literal).toList()) : null;
            }
        }
    }
}

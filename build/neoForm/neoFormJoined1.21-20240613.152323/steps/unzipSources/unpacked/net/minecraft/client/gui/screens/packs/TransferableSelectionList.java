package net.minecraft.client.gui.screens.packs;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TransferableSelectionList extends ObjectSelectionList<TransferableSelectionList.PackEntry> {
    static final ResourceLocation SELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted");
    static final ResourceLocation SELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select");
    static final ResourceLocation UNSELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect_highlighted");
    static final ResourceLocation UNSELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect");
    static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted");
    static final ResourceLocation MOVE_UP_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up");
    static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted");
    static final ResourceLocation MOVE_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down");
    static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
    static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
    private final Component title;
    final PackSelectionScreen screen;

    public TransferableSelectionList(Minecraft pMinecraft, PackSelectionScreen pScreen, int pWidth, int pHeight, Component pTitle) {
        super(pMinecraft, pWidth, pHeight, 33, 36);
        this.screen = pScreen;
        this.title = pTitle;
        this.centerListVertically = false;
        this.setRenderHeader(true, (int)(9.0F * 1.5F));
    }

    @Override
    protected void renderHeader(GuiGraphics pGuiGraphics, int pX, int pY) {
        Component component = Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
        pGuiGraphics.drawString(
            this.minecraft.font,
            component,
            pX + this.width / 2 - this.minecraft.font.width(component) / 2,
            Math.min(this.getY() + 3, pY),
            -1,
            false
        );
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRight() - 6;
    }

    @Override
    protected void renderSelection(GuiGraphics pGuiGraphics, int pTop, int pWidth, int pHeight, int pOuterColor, int pInnerColor) {
        if (this.scrollbarVisible()) {
            int i = 2;
            int j = this.getRowLeft() - 2;
            int k = this.getRight() - 6 - 1;
            int l = pTop - 2;
            int i1 = pTop + pHeight + 2;
            pGuiGraphics.fill(j, l, k, i1, pOuterColor);
            pGuiGraphics.fill(j + 1, l + 1, k - 1, i1 - 1, pInnerColor);
        } else {
            super.renderSelection(pGuiGraphics, pTop, pWidth, pHeight, pOuterColor, pInnerColor);
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
        if (this.getSelected() != null) {
            switch (pKeyCode) {
                case 32:
                case 257:
                    this.getSelected().keyboardSelection();
                    return true;
                default:
                    if (Screen.hasShiftDown()) {
                        switch (pKeyCode) {
                            case 264:
                                this.getSelected().keyboardMoveDown();
                                return true;
                            case 265:
                                this.getSelected().keyboardMoveUp();
                                return true;
                        }
                    }
            }
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @OnlyIn(Dist.CLIENT)
    public static class PackEntry extends ObjectSelectionList.Entry<TransferableSelectionList.PackEntry> {
        private static final int MAX_DESCRIPTION_WIDTH_PIXELS = 157;
        private static final int MAX_NAME_WIDTH_PIXELS = 157;
        private static final String TOO_LONG_NAME_SUFFIX = "...";
        private final TransferableSelectionList parent;
        protected final Minecraft minecraft;
        private final PackSelectionModel.Entry pack;
        private final FormattedCharSequence nameDisplayCache;
        private final MultiLineLabel descriptionDisplayCache;
        private final FormattedCharSequence incompatibleNameDisplayCache;
        private final MultiLineLabel incompatibleDescriptionDisplayCache;

        public PackEntry(Minecraft pMinecraft, TransferableSelectionList pParent, PackSelectionModel.Entry pPack) {
            this.minecraft = pMinecraft;
            this.pack = pPack;
            this.parent = pParent;
            this.nameDisplayCache = cacheName(pMinecraft, pPack.getTitle());
            this.descriptionDisplayCache = cacheDescription(pMinecraft, pPack.getExtendedDescription());
            this.incompatibleNameDisplayCache = cacheName(pMinecraft, TransferableSelectionList.INCOMPATIBLE_TITLE);
            this.incompatibleDescriptionDisplayCache = cacheDescription(pMinecraft, pPack.getCompatibility().getDescription());
        }

        private static FormattedCharSequence cacheName(Minecraft pMinecraft, Component pName) {
            int i = pMinecraft.font.width(pName);
            if (i > 157) {
                FormattedText formattedtext = FormattedText.composite(
                    pMinecraft.font.substrByWidth(pName, 157 - pMinecraft.font.width("...")), FormattedText.of("...")
                );
                return Language.getInstance().getVisualOrder(formattedtext);
            } else {
                return pName.getVisualOrderText();
            }
        }

        private static MultiLineLabel cacheDescription(Minecraft pMinecraft, Component pText) {
            return MultiLineLabel.create(pMinecraft.font, 157, 2, pText);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.pack.getTitle());
        }

        @Override
        public void render(
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
        ) {
            PackCompatibility packcompatibility = this.pack.getCompatibility();
            if (!packcompatibility.isCompatible()) {
                int i = pLeft + pWidth - 3 - (this.parent.scrollbarVisible() ? 7 : 0);
                pGuiGraphics.fill(pLeft - 1, pTop - 1, i, pTop + pHeight + 1, -8978432);
            }

            pGuiGraphics.blit(this.pack.getIconTexture(), pLeft, pTop, 0.0F, 0.0F, 32, 32, 32, 32);
            FormattedCharSequence formattedcharsequence = this.nameDisplayCache;
            MultiLineLabel multilinelabel = this.descriptionDisplayCache;
            if (this.showHoverOverlay()
                && (this.minecraft.options.touchscreen().get() || pHovering || this.parent.getSelected() == this && this.parent.isFocused())) {
                pGuiGraphics.fill(pLeft, pTop, pLeft + 32, pTop + 32, -1601138544);
                int j = pMouseX - pLeft;
                int k = pMouseY - pTop;
                if (!this.pack.getCompatibility().isCompatible()) {
                    formattedcharsequence = this.incompatibleNameDisplayCache;
                    multilinelabel = this.incompatibleDescriptionDisplayCache;
                }

                if (this.pack.canSelect()) {
                    if (j < 32) {
                        pGuiGraphics.blitSprite(TransferableSelectionList.SELECT_HIGHLIGHTED_SPRITE, pLeft, pTop, 32, 32);
                    } else {
                        pGuiGraphics.blitSprite(TransferableSelectionList.SELECT_SPRITE, pLeft, pTop, 32, 32);
                    }
                } else {
                    if (this.pack.canUnselect()) {
                        if (j < 16) {
                            pGuiGraphics.blitSprite(TransferableSelectionList.UNSELECT_HIGHLIGHTED_SPRITE, pLeft, pTop, 32, 32);
                        } else {
                            pGuiGraphics.blitSprite(TransferableSelectionList.UNSELECT_SPRITE, pLeft, pTop, 32, 32);
                        }
                    }

                    if (this.pack.canMoveUp()) {
                        if (j < 32 && j > 16 && k < 16) {
                            pGuiGraphics.blitSprite(TransferableSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, pLeft, pTop, 32, 32);
                        } else {
                            pGuiGraphics.blitSprite(TransferableSelectionList.MOVE_UP_SPRITE, pLeft, pTop, 32, 32);
                        }
                    }

                    if (this.pack.canMoveDown()) {
                        if (j < 32 && j > 16 && k > 16) {
                            pGuiGraphics.blitSprite(TransferableSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, pLeft, pTop, 32, 32);
                        } else {
                            pGuiGraphics.blitSprite(TransferableSelectionList.MOVE_DOWN_SPRITE, pLeft, pTop, 32, 32);
                        }
                    }
                }
            }

            pGuiGraphics.drawString(this.minecraft.font, formattedcharsequence, pLeft + 32 + 2, pTop + 1, 16777215);
            multilinelabel.renderLeftAligned(pGuiGraphics, pLeft + 32 + 2, pTop + 12, 10, -8355712);
        }

        public String getPackId() {
            return this.pack.getId();
        }

        private boolean showHoverOverlay() {
            return !this.pack.isFixedPosition() || !this.pack.isRequired();
        }

        public void keyboardSelection() {
            if (this.pack.canSelect() && this.handlePackSelection()) {
                this.parent.screen.updateFocus(this.parent);
            } else if (this.pack.canUnselect()) {
                this.pack.unselect();
                this.parent.screen.updateFocus(this.parent);
            }
        }

        void keyboardMoveUp() {
            if (this.pack.canMoveUp()) {
                this.pack.moveUp();
            }
        }

        void keyboardMoveDown() {
            if (this.pack.canMoveDown()) {
                this.pack.moveDown();
            }
        }

        private boolean handlePackSelection() {
            if (this.pack.getCompatibility().isCompatible()) {
                this.pack.select();
                return true;
            } else {
                Component component = this.pack.getCompatibility().getConfirmation();
                this.minecraft.setScreen(new ConfirmScreen(p_264693_ -> {
                    this.minecraft.setScreen(this.parent.screen);
                    if (p_264693_) {
                        this.pack.select();
                    }
                }, TransferableSelectionList.INCOMPATIBLE_CONFIRM_TITLE, component));
                return false;
            }
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
            double d0 = pMouseX - (double)this.parent.getRowLeft();
            double d1 = pMouseY - (double)this.parent.getRowTop(this.parent.children().indexOf(this));
            if (this.showHoverOverlay() && d0 <= 32.0) {
                this.parent.screen.clearSelected();
                if (this.pack.canSelect()) {
                    this.handlePackSelection();
                    return true;
                }

                if (d0 < 16.0 && this.pack.canUnselect()) {
                    this.pack.unselect();
                    return true;
                }

                if (d0 > 16.0 && d1 < 16.0 && this.pack.canMoveUp()) {
                    this.pack.moveUp();
                    return true;
                }

                if (d0 > 16.0 && d1 > 16.0 && this.pack.canMoveDown()) {
                    this.pack.moveDown();
                    return true;
                }
            }

            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }
    }
}

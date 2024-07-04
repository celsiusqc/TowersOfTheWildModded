package net.minecraft.client.gui.components;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EditBox extends AbstractWidget implements Renderable {
    private static final WidgetSprites SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("widget/text_field"), ResourceLocation.withDefaultNamespace("widget/text_field_highlighted")
    );
    public static final int BACKWARDS = -1;
    public static final int FORWARDS = 1;
    private static final int CURSOR_INSERT_WIDTH = 1;
    private static final int CURSOR_INSERT_COLOR = -3092272;
    private static final String CURSOR_APPEND_CHARACTER = "_";
    public static final int DEFAULT_TEXT_COLOR = 14737632;
    private static final int CURSOR_BLINK_INTERVAL_MS = 300;
    private final Font font;
    /**
     * Has the current text being edited on the textbox.
     */
    private String value = "";
    private int maxLength = 32;
    private boolean bordered = true;
    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    private boolean canLoseFocus = true;
    /**
     * If this value is true along with isFocused, keyTyped will process the keys.
     */
    private boolean isEditable = true;
    /**
     * The current character index that should be used as start of the rendered text.
     */
    private int displayPos;
    private int cursorPos;
    /**
     * other selection position, maybe the same as the cursor
     */
    private int highlightPos;
    private int textColor = 14737632;
    private int textColorUneditable = 7368816;
    @Nullable
    private String suggestion;
    @Nullable
    private Consumer<String> responder;
    /**
     * Called to check if the text is valid
     */
    private Predicate<String> filter = Objects::nonNull;
    private BiFunction<String, Integer, FormattedCharSequence> formatter = (p_94147_, p_94148_) -> FormattedCharSequence.forward(p_94147_, Style.EMPTY);
    @Nullable
    private Component hint;
    private long focusedTime = Util.getMillis();

    public EditBox(Font pFont, int pWidth, int pHeight, Component pMessage) {
        this(pFont, 0, 0, pWidth, pHeight, pMessage);
    }

    public EditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        this(pFont, pX, pY, pWidth, pHeight, null, pMessage);
    }

    public EditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, @Nullable EditBox pEditBox, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage);
        this.font = pFont;
        if (pEditBox != null) {
            this.setValue(pEditBox.getValue());
        }
    }

    public void setResponder(Consumer<String> pResponder) {
        this.responder = pResponder;
    }

    public void setFormatter(BiFunction<String, Integer, FormattedCharSequence> pTextFormatter) {
        this.formatter = pTextFormatter;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        Component component = this.getMessage();
        return Component.translatable("gui.narrate.editBox", component, this.value);
    }

    /**
     * Sets the text of the textbox, and moves the cursor to the end.
     */
    public void setValue(String pText) {
        if (this.filter.test(pText)) {
            if (pText.length() > this.maxLength) {
                this.value = pText.substring(0, this.maxLength);
            } else {
                this.value = pText;
            }

            this.moveCursorToEnd(false);
            this.setHighlightPos(this.cursorPos);
            this.onValueChange(pText);
        }
    }

    public String getValue() {
        return this.value;
    }

    public String getHighlighted() {
        int i = Math.min(this.cursorPos, this.highlightPos);
        int j = Math.max(this.cursorPos, this.highlightPos);
        return this.value.substring(i, j);
    }

    public void setFilter(Predicate<String> pValidator) {
        this.filter = pValidator;
    }

    /**
     * Adds the given text after the cursor, or replaces the currently selected text if there is a selection.
     */
    public void insertText(String pTextToWrite) {
        int i = Math.min(this.cursorPos, this.highlightPos);
        int j = Math.max(this.cursorPos, this.highlightPos);
        int k = this.maxLength - this.value.length() - (i - j);
        if (k > 0) {
            String s = StringUtil.filterText(pTextToWrite);
            int l = s.length();
            if (k < l) {
                if (Character.isHighSurrogate(s.charAt(k - 1))) {
                    k--;
                }

                s = s.substring(0, k);
                l = k;
            }

            String s1 = new StringBuilder(this.value).replace(i, j, s).toString();
            if (this.filter.test(s1)) {
                this.value = s1;
                this.setCursorPosition(i + l);
                this.setHighlightPos(this.cursorPos);
                this.onValueChange(this.value);
            }
        }
    }

    private void onValueChange(String pNewText) {
        if (this.responder != null) {
            this.responder.accept(pNewText);
        }
    }

    private void deleteText(int pCount) {
        if (Screen.hasControlDown()) {
            this.deleteWords(pCount);
        } else {
            this.deleteChars(pCount);
        }
    }

    /**
     * Deletes the given number of words from the current cursor's position, unless there is currently a selection, in which case the selection is deleted instead.
     */
    public void deleteWords(int pNum) {
        if (!this.value.isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                this.deleteCharsToPos(this.getWordPosition(pNum));
            }
        }
    }

    /**
     * Deletes the given number of characters from the current cursor's position, unless there is currently a selection, in which case the selection is deleted instead.
     */
    public void deleteChars(int pNum) {
        this.deleteCharsToPos(this.getCursorPos(pNum));
    }

    public void deleteCharsToPos(int pNum) {
        if (!this.value.isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                int i = Math.min(pNum, this.cursorPos);
                int j = Math.max(pNum, this.cursorPos);
                if (i != j) {
                    String s = new StringBuilder(this.value).delete(i, j).toString();
                    if (this.filter.test(s)) {
                        this.value = s;
                        this.moveCursorTo(i, false);
                    }
                }
            }
        }
    }

    /**
     * Gets the starting index of the word at the specified number of words away from the cursor position.
     */
    public int getWordPosition(int pNumWords) {
        return this.getWordPosition(pNumWords, this.getCursorPosition());
    }

    /**
     * Gets the starting index of the word at a distance of the specified number of words away from the given position.
     */
    private int getWordPosition(int pNumWords, int pPos) {
        return this.getWordPosition(pNumWords, pPos, true);
    }

    /**
     * Like getNthWordFromPos (which wraps this), but adds option for skipping consecutive spaces
     */
    private int getWordPosition(int pNumWords, int pPos, boolean pSkipConsecutiveSpaces) {
        int i = pPos;
        boolean flag = pNumWords < 0;
        int j = Math.abs(pNumWords);

        for (int k = 0; k < j; k++) {
            if (!flag) {
                int l = this.value.length();
                i = this.value.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while (pSkipConsecutiveSpaces && i < l && this.value.charAt(i) == ' ') {
                        i++;
                    }
                }
            } else {
                while (pSkipConsecutiveSpaces && i > 0 && this.value.charAt(i - 1) == ' ') {
                    i--;
                }

                while (i > 0 && this.value.charAt(i - 1) != ' ') {
                    i--;
                }
            }
        }

        return i;
    }

    public void moveCursor(int pDelta, boolean pSelect) {
        this.moveCursorTo(this.getCursorPos(pDelta), pSelect);
    }

    private int getCursorPos(int pDelta) {
        return Util.offsetByCodepoints(this.value, this.cursorPos, pDelta);
    }

    public void moveCursorTo(int pDelta, boolean pSelect) {
        this.setCursorPosition(pDelta);
        if (!pSelect) {
            this.setHighlightPos(this.cursorPos);
        }

        this.onValueChange(this.value);
    }

    public void setCursorPosition(int pPos) {
        this.cursorPos = Mth.clamp(pPos, 0, this.value.length());
        this.scrollTo(this.cursorPos);
    }

    public void moveCursorToStart(boolean pSelect) {
        this.moveCursorTo(0, pSelect);
    }

    public void moveCursorToEnd(boolean pSelect) {
        this.moveCursorTo(this.value.length(), pSelect);
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
        if (this.isActive() && this.isFocused()) {
            switch (pKeyCode) {
                case 259:
                    if (this.isEditable) {
                        this.deleteText(-1);
                    }

                    return true;
                case 260:
                case 264:
                case 265:
                case 266:
                case 267:
                default:
                    if (Screen.isSelectAll(pKeyCode)) {
                        this.moveCursorToEnd(false);
                        this.setHighlightPos(0);
                        return true;
                    } else if (Screen.isCopy(pKeyCode)) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                        return true;
                    } else if (Screen.isPaste(pKeyCode)) {
                        if (this.isEditable()) {
                            this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                        }

                        return true;
                    } else {
                        if (Screen.isCut(pKeyCode)) {
                            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                            if (this.isEditable()) {
                                this.insertText("");
                            }

                            return true;
                        }

                        return false;
                    }
                case 261:
                    if (this.isEditable) {
                        this.deleteText(1);
                    }

                    return true;
                case 262:
                    if (Screen.hasControlDown()) {
                        this.moveCursorTo(this.getWordPosition(1), Screen.hasShiftDown());
                    } else {
                        this.moveCursor(1, Screen.hasShiftDown());
                    }

                    return true;
                case 263:
                    if (Screen.hasControlDown()) {
                        this.moveCursorTo(this.getWordPosition(-1), Screen.hasShiftDown());
                    } else {
                        this.moveCursor(-1, Screen.hasShiftDown());
                    }

                    return true;
                case 268:
                    this.moveCursorToStart(Screen.hasShiftDown());
                    return true;
                case 269:
                    this.moveCursorToEnd(Screen.hasShiftDown());
                    return true;
            }
        } else {
            return false;
        }
    }

    public boolean canConsumeInput() {
        return this.isActive() && this.isFocused() && this.isEditable();
    }

    /**
     * Called when a character is typed within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pCodePoint the code point of the typed character.
     * @param pModifiers the keyboard modifiers.
     */
    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (!this.canConsumeInput()) {
            return false;
        } else if (StringUtil.isAllowedChatCharacter(pCodePoint)) {
            if (this.isEditable) {
                this.insertText(Character.toString(pCodePoint));
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick(double pMouseX, double pMouseY) {
        int i = Mth.floor(pMouseX) - this.getX();
        if (this.bordered) {
            i -= 4;
        }

        String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        this.moveCursorTo(this.font.plainSubstrByWidth(s, i).length() + this.displayPos, Screen.hasShiftDown());
    }

    @Override
    public void playDownSound(SoundManager pHandler) {
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isVisible()) {
            if (this.isBordered()) {
                ResourceLocation resourcelocation = SPRITES.get(this.isActive(), this.isFocused());
                pGuiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            }

            int l1 = this.isEditable ? this.textColor : this.textColorUneditable;
            int i = this.cursorPos - this.displayPos;
            String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
            boolean flag = i >= 0 && i <= s.length();
            boolean flag1 = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L && flag;
            int j = this.bordered ? this.getX() + 4 : this.getX();
            int k = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
            int l = j;
            int i1 = Mth.clamp(this.highlightPos - this.displayPos, 0, s.length());
            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, i) : s;
                l = pGuiGraphics.drawString(this.font, this.formatter.apply(s1, this.displayPos), j, k, l1);
            }

            boolean flag2 = this.cursorPos < this.value.length() || this.value.length() >= this.getMaxLength();
            int j1 = l;
            if (!flag) {
                j1 = i > 0 ? j + this.width : j;
            } else if (flag2) {
                j1 = l - 1;
                l--;
            }

            if (!s.isEmpty() && flag && i < s.length()) {
                pGuiGraphics.drawString(this.font, this.formatter.apply(s.substring(i), this.cursorPos), l, k, l1);
            }

            if (this.hint != null && s.isEmpty() && !this.isFocused()) {
                pGuiGraphics.drawString(this.font, this.hint, l, k, l1);
            }

            if (!flag2 && this.suggestion != null) {
                pGuiGraphics.drawString(this.font, this.suggestion, j1 - 1, k, -8355712);
            }

            if (flag1) {
                if (flag2) {
                    pGuiGraphics.fill(RenderType.guiOverlay(), j1, k - 1, j1 + 1, k + 1 + 9, -3092272);
                } else {
                    pGuiGraphics.drawString(this.font, "_", j1, k, l1);
                }
            }

            if (i1 != i) {
                int k1 = j + this.font.width(s.substring(0, i1));
                this.renderHighlight(pGuiGraphics, j1, k - 1, k1 - 1, k + 1 + 9);
            }
        }
    }

    private void renderHighlight(GuiGraphics pGuiGraphics, int pMinX, int pMinY, int pMaxX, int pMaxY) {
        if (pMinX < pMaxX) {
            int i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            int j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        if (pMaxX > this.getX() + this.width) {
            pMaxX = this.getX() + this.width;
        }

        if (pMinX > this.getX() + this.width) {
            pMinX = this.getX() + this.width;
        }

        pGuiGraphics.fill(RenderType.guiTextHighlight(), pMinX, pMinY, pMaxX, pMaxY, -16776961);
    }

    /**
     * Sets the maximum length for the text in this text box. If the current text is longer than this length, the current text will be trimmed.
     */
    public void setMaxLength(int pLength) {
        this.maxLength = pLength;
        if (this.value.length() > pLength) {
            this.value = this.value.substring(0, pLength);
            this.onValueChange(this.value);
        }
    }

    private int getMaxLength() {
        return this.maxLength;
    }

    public int getCursorPosition() {
        return this.cursorPos;
    }

    public boolean isBordered() {
        return this.bordered;
    }

    /**
     * Sets whether the background and outline of this text box should be drawn.
     */
    public void setBordered(boolean pEnableBackgroundDrawing) {
        this.bordered = pEnableBackgroundDrawing;
    }

    /**
     * Sets the color to use when drawing this text box's text. A different color is used if this text box is disabled.
     */
    public void setTextColor(int pColor) {
        this.textColor = pColor;
    }

    /**
     * Sets the color to use for text in this text box when this text box is disabled.
     */
    public void setTextColorUneditable(int pColor) {
        this.textColorUneditable = pColor;
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param pFocused {@code true} to apply focus, {@code false} to remove focus
     */
    @Override
    public void setFocused(boolean pFocused) {
        if (this.canLoseFocus || pFocused) {
            super.setFocused(pFocused);
            if (pFocused) {
                this.focusedTime = Util.getMillis();
            }
        }
    }

    private boolean isEditable() {
        return this.isEditable;
    }

    /**
     * Sets whether this text box is enabled. Disabled text boxes cannot be typed in.
     */
    public void setEditable(boolean pEnabled) {
        this.isEditable = pEnabled;
    }

    public int getInnerWidth() {
        return this.isBordered() ? this.width - 8 : this.width;
    }

    /**
     * Sets the position of the selection anchor (the selection anchor and the cursor position mark the edges of the selection). If the anchor is set beyond the bounds of the current text, it will be put back inside.
     */
    public void setHighlightPos(int pPosition) {
        this.highlightPos = Mth.clamp(pPosition, 0, this.value.length());
        this.scrollTo(this.highlightPos);
    }

    private void scrollTo(int pPosition) {
        if (this.font != null) {
            this.displayPos = Math.min(this.displayPos, this.value.length());
            int i = this.getInnerWidth();
            String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), i);
            int j = s.length() + this.displayPos;
            if (pPosition == this.displayPos) {
                this.displayPos = this.displayPos - this.font.plainSubstrByWidth(this.value, i, true).length();
            }

            if (pPosition > j) {
                this.displayPos += pPosition - j;
            } else if (pPosition <= this.displayPos) {
                this.displayPos = this.displayPos - (this.displayPos - pPosition);
            }

            this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
        }
    }

    /**
     * Sets whether this text box loses focus when something other than it is clicked.
     */
    public void setCanLoseFocus(boolean pCanLoseFocus) {
        this.canLoseFocus = pCanLoseFocus;
    }

    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Sets whether this textbox is visible.
     */
    public void setVisible(boolean pIsVisible) {
        this.visible = pIsVisible;
    }

    public void setSuggestion(@Nullable String pSuggestion) {
        this.suggestion = pSuggestion;
    }

    public int getScreenX(int pCharNum) {
        return pCharNum > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, pCharNum));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        pNarrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    public void setHint(Component pHint) {
        this.hint = pHint;
    }
}

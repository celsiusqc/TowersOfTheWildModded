package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmLinkScreen extends ConfirmScreen {
    private static final Component COPY_BUTTON_TEXT = Component.translatable("chat.copy");
    private static final Component WARNING_TEXT = Component.translatable("chat.link.warning");
    private final String url;
    private final boolean showWarning;

    public ConfirmLinkScreen(BooleanConsumer pCallback, String pUrl, boolean pTrusted) {
        this(
            pCallback,
            confirmMessage(pTrusted),
            Component.literal(pUrl),
            pUrl,
            pTrusted ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO,
            pTrusted
        );
    }

    public ConfirmLinkScreen(BooleanConsumer pCallback, Component pTitle, String pUrl, boolean pTrusted) {
        this(
            pCallback, pTitle, confirmMessage(pTrusted, pUrl), pUrl, pTrusted ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO, pTrusted
        );
    }

    public ConfirmLinkScreen(BooleanConsumer p_352448_, Component p_352313_, URI p_352270_, boolean p_352104_) {
        this(p_352448_, p_352313_, p_352270_.toString(), p_352104_);
    }

    public ConfirmLinkScreen(BooleanConsumer p_352145_, Component p_352090_, Component p_352169_, URI p_352197_, Component p_352365_, boolean p_352117_) {
        this(p_352145_, p_352090_, p_352169_, p_352197_.toString(), p_352365_, true);
    }

    public ConfirmLinkScreen(BooleanConsumer pCallback, Component pTitle, Component pMessage, String pUrl, Component pNoButton, boolean pTrusted) {
        super(pCallback, pTitle, pMessage);
        this.yesButton = (Component)(pTrusted ? Component.translatable("chat.link.open") : CommonComponents.GUI_YES);
        this.noButton = pNoButton;
        this.showWarning = !pTrusted;
        this.url = pUrl;
    }

    protected static MutableComponent confirmMessage(boolean pTrusted, String pExtraInfo) {
        return confirmMessage(pTrusted).append(CommonComponents.SPACE).append(Component.literal(pExtraInfo));
    }

    protected static MutableComponent confirmMessage(boolean pTrusted) {
        return Component.translatable(pTrusted ? "chat.link.confirmTrusted" : "chat.link.confirm");
    }

    @Override
    protected void addButtons(int pY) {
        this.addRenderableWidget(
            Button.builder(this.yesButton, p_169249_ -> this.callback.accept(true)).bounds(this.width / 2 - 50 - 105, pY, 100, 20).build()
        );
        this.addRenderableWidget(Button.builder(COPY_BUTTON_TEXT, p_169247_ -> {
            this.copyToClipboard();
            this.callback.accept(false);
        }).bounds(this.width / 2 - 50, pY, 100, 20).build());
        this.addRenderableWidget(
            Button.builder(this.noButton, p_169245_ -> this.callback.accept(false)).bounds(this.width / 2 - 50 + 105, pY, 100, 20).build()
        );
    }

    public void copyToClipboard() {
        this.minecraft.keyboardHandler.setClipboard(this.url);
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
        if (this.showWarning) {
            pGuiGraphics.drawCenteredString(this.font, WARNING_TEXT, this.width / 2, 110, 16764108);
        }
    }

    public static void confirmLinkNow(Screen pLastScreen, String pUrl, boolean p_350852_) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmLinkScreen(p_274671_ -> {
            if (p_274671_) {
                Util.getPlatform().openUri(pUrl);
            }

            minecraft.setScreen(pLastScreen);
        }, pUrl, p_350852_));
    }

    public static void confirmLinkNow(Screen p_352415_, URI p_352168_, boolean p_352122_) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmLinkScreen(p_351650_ -> {
            if (p_351650_) {
                Util.getPlatform().openUri(p_352168_);
            }

            minecraft.setScreen(p_352415_);
        }, p_352168_.toString(), p_352122_));
    }

    public static void confirmLinkNow(Screen p_352190_, URI p_352392_) {
        confirmLinkNow(p_352190_, p_352392_, true);
    }

    public static void confirmLinkNow(Screen pLastScreen, String pUrl) {
        confirmLinkNow(pLastScreen, pUrl, true);
    }

    public static Button.OnPress confirmLink(Screen pLastScreen, String pUrl, boolean p_350962_) {
        return p_349796_ -> confirmLinkNow(pLastScreen, pUrl, p_350962_);
    }

    public static Button.OnPress confirmLink(Screen p_352068_, URI p_352436_, boolean p_352216_) {
        return p_351646_ -> confirmLinkNow(p_352068_, p_352436_, p_352216_);
    }

    public static Button.OnPress confirmLink(Screen pLastScreen, String pUrl) {
        return confirmLink(pLastScreen, pUrl, true);
    }

    public static Button.OnPress confirmLink(Screen p_352385_, URI p_352416_) {
        return confirmLink(p_352385_, p_352416_, true);
    }
}

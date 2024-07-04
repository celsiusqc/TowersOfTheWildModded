package net.minecraft.client.gui.screens;

import com.mojang.text2speech.Narrator;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AccessibilityOnboardingScreen extends Screen {
    private static final Component TITLE = Component.translatable("accessibility.onboarding.screen.title");
    private static final Component ONBOARDING_NARRATOR_MESSAGE = Component.translatable("accessibility.onboarding.screen.narrator");
    private static final int PADDING = 4;
    private static final int TITLE_PADDING = 16;
    private final LogoRenderer logoRenderer;
    private final Options options;
    private final boolean narratorAvailable;
    private boolean hasNarrated;
    private float timer;
    private final Runnable onClose;
    @Nullable
    private FocusableTextWidget textWidget;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, this.initTitleYPos(), 33);

    public AccessibilityOnboardingScreen(Options pOptions, Runnable pOnClose) {
        super(TITLE);
        this.options = pOptions;
        this.onClose = pOnClose;
        this.logoRenderer = new LogoRenderer(true);
        this.narratorAvailable = Minecraft.getInstance().getNarrator().isActive();
    }

    @Override
    public void init() {
        LinearLayout linearlayout = this.layout.addToContents(LinearLayout.vertical());
        linearlayout.defaultCellSetting().alignHorizontallyCenter().padding(4);
        this.textWidget = linearlayout.addChild(new FocusableTextWidget(this.width, this.title, this.font), p_329717_ -> p_329717_.padding(8));
        if (this.options.narrator().createButton(this.options) instanceof CycleButton cyclebutton) {
            this.narratorButton = cyclebutton;
            this.narratorButton.active = this.narratorAvailable;
            linearlayout.addChild(this.narratorButton);
        }

        linearlayout.addChild(
            CommonButtons.accessibility(150, p_344155_ -> this.closeAndSetScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), false)
        );
        linearlayout.addChild(
            CommonButtons.language(
                150, p_344154_ -> this.closeAndSetScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), false
            )
        );
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_CONTINUE, p_267841_ -> this.onClose()).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.textWidget != null) {
            this.textWidget.containWithin(this.width);
        }

        this.layout.arrangeElements();
    }

    @Override
    protected void setInitialFocus() {
        if (this.narratorAvailable && this.narratorButton != null) {
            this.setInitialFocus(this.narratorButton);
        } else {
            super.setInitialFocus();
        }
    }

    private int initTitleYPos() {
        return 90;
    }

    @Override
    public void onClose() {
        this.close(true, this.onClose);
    }

    private void closeAndSetScreen(Screen pScreen) {
        this.close(false, () -> this.minecraft.setScreen(pScreen));
    }

    private void close(boolean p_352950_, Runnable p_299978_) {
        if (p_352950_) {
            this.options.onboardingAccessibilityFinished();
        }

        Narrator.getNarrator().clear();
        p_299978_.run();
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
        this.handleInitialNarrationDelay();
        this.logoRenderer.renderLogo(pGuiGraphics, this.width, 1.0F);
    }

    @Override
    protected void renderPanorama(GuiGraphics pGuiGraphics, float pPartialTick) {
        PANORAMA.render(pGuiGraphics, this.width, this.height, 1.0F, 0.0F);
    }

    private void handleInitialNarrationDelay() {
        if (!this.hasNarrated && this.narratorAvailable) {
            if (this.timer < 40.0F) {
                this.timer++;
            } else if (this.minecraft.isWindowActive()) {
                Narrator.getNarrator().say(ONBOARDING_NARRATOR_MESSAGE.getString(), true);
                this.hasNarrated = true;
            }
        }
    }
}

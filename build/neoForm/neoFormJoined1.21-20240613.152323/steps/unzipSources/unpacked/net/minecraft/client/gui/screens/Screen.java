package net.minecraft.client.gui.screens;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.ScreenNarrationCollector;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class Screen extends AbstractContainerEventHandler implements Renderable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component USAGE_NARRATION = Component.translatable("narrator.screen.usage");
    protected static final CubeMap CUBE_MAP = new CubeMap(ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama"));
    protected static final PanoramaRenderer PANORAMA = new PanoramaRenderer(CUBE_MAP);
    public static final ResourceLocation MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_background.png");
    public static final ResourceLocation HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/header_separator.png");
    public static final ResourceLocation FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/footer_separator.png");
    private static final ResourceLocation INWORLD_MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_background.png");
    public static final ResourceLocation INWORLD_HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_header_separator.png");
    public static final ResourceLocation INWORLD_FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_footer_separator.png");
    protected final Component title;
    private final List<GuiEventListener> children = Lists.newArrayList();
    private final List<NarratableEntry> narratables = Lists.newArrayList();
    @Nullable
    protected Minecraft minecraft;
    private boolean initialized;
    public int width;
    public int height;
    public final List<Renderable> renderables = Lists.newArrayList();
    protected Font font;
    private static final long NARRATE_SUPPRESS_AFTER_INIT_TIME = TimeUnit.SECONDS.toMillis(2L);
    private static final long NARRATE_DELAY_NARRATOR_ENABLED = NARRATE_SUPPRESS_AFTER_INIT_TIME;
    private static final long NARRATE_DELAY_MOUSE_MOVE = 750L;
    private static final long NARRATE_DELAY_MOUSE_ACTION = 200L;
    private static final long NARRATE_DELAY_KEYBOARD_ACTION = 200L;
    private final ScreenNarrationCollector narrationState = new ScreenNarrationCollector();
    private long narrationSuppressTime = Long.MIN_VALUE;
    private long nextNarrationTime = Long.MAX_VALUE;
    @Nullable
    protected CycleButton<NarratorStatus> narratorButton;
    @Nullable
    private NarratableEntry lastNarratable;
    @Nullable
    private Screen.DeferredTooltipRendering deferredTooltipRendering;
    protected final Executor screenExecutor = p_289626_ -> this.minecraft.execute(() -> {
            if (this.minecraft.screen == this) {
                p_289626_.run();
            }
        });

    protected Screen(Component pTitle) {
        this.title = pTitle;
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getNarrationMessage() {
        return this.getTitle();
    }

    public final void renderWithTooltip(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        if (this.deferredTooltipRendering != null) {
            pGuiGraphics.renderTooltip(this.font, this.deferredTooltipRendering.tooltip(), this.deferredTooltipRendering.positioner(), pMouseX, pMouseY);
            this.deferredTooltipRendering = null;
        }
    }

    /**
 * Neo: mixins targeting this method won't fire for container screens as
 * {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen#render}
 * replicates this method in place of a super call to insert an event
 * <p>
 * Renders the graphical user interface (GUI) element.
 *
 * @param pGuiGraphics the GuiGraphics object used for rendering.
 * @param pMouseX      the x-coordinate of the mouse cursor.
 * @param pMouseY      the y-coordinate of the mouse cursor.
 * @param pPartialTick the partial tick time.
 */
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        for (Renderable renderable : this.renderables) {
            renderable.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
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
        if (pKeyCode == 256 && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        } else if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else {
            FocusNavigationEvent focusnavigationevent = (FocusNavigationEvent)(switch (pKeyCode) {
                case 258 -> this.createTabEvent();
                default -> null;
                case 262 -> this.createArrowEvent(ScreenDirection.RIGHT);
                case 263 -> this.createArrowEvent(ScreenDirection.LEFT);
                case 264 -> this.createArrowEvent(ScreenDirection.DOWN);
                case 265 -> this.createArrowEvent(ScreenDirection.UP);
            });
            if (focusnavigationevent != null) {
                ComponentPath componentpath = super.nextFocusPath(focusnavigationevent);
                if (componentpath == null && focusnavigationevent instanceof FocusNavigationEvent.TabNavigation) {
                    this.clearFocus();
                    componentpath = super.nextFocusPath(focusnavigationevent);
                }

                if (componentpath != null) {
                    this.changeFocus(componentpath);
                }
            }

            return false;
        }
    }

    private FocusNavigationEvent.TabNavigation createTabEvent() {
        boolean flag = !hasShiftDown();
        return new FocusNavigationEvent.TabNavigation(flag);
    }

    private FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection pDirection) {
        return new FocusNavigationEvent.ArrowNavigation(pDirection);
    }

    protected void setInitialFocus() {
        if (this.minecraft.getLastInputType().isKeyboard()) {
            FocusNavigationEvent.TabNavigation focusnavigationevent$tabnavigation = new FocusNavigationEvent.TabNavigation(true);
            ComponentPath componentpath = super.nextFocusPath(focusnavigationevent$tabnavigation);
            if (componentpath != null) {
                this.changeFocus(componentpath);
            }
        }
    }

    protected void setInitialFocus(GuiEventListener pListener) {
        ComponentPath componentpath = ComponentPath.path(this, pListener.nextFocusPath(new FocusNavigationEvent.InitialFocus()));
        if (componentpath != null) {
            this.changeFocus(componentpath);
        }
    }

    public void clearFocus() {
        ComponentPath componentpath = this.getCurrentFocusPath();
        if (componentpath != null) {
            componentpath.applyFocus(false);
        }
    }

    @VisibleForTesting
    protected void changeFocus(ComponentPath pPath) {
        this.clearFocus();
        pPath.applyFocus(true);
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    public void onClose() {
        this.minecraft.popGuiLayer();
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T pWidget) {
        this.renderables.add(pWidget);
        return this.addWidget(pWidget);
    }

    protected <T extends Renderable> T addRenderableOnly(T pRenderable) {
        this.renderables.add(pRenderable);
        return pRenderable;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T pListener) {
        this.children.add(pListener);
        this.narratables.add(pListener);
        return pListener;
    }

    protected void removeWidget(GuiEventListener pListener) {
        if (pListener instanceof Renderable) {
            this.renderables.remove((Renderable)pListener);
        }

        if (pListener instanceof NarratableEntry) {
            this.narratables.remove((NarratableEntry)pListener);
        }

        this.children.remove(pListener);
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children.clear();
        this.narratables.clear();
    }

    public static List<Component> getTooltipFromItem(Minecraft pMinecraft, ItemStack pItem) {
        return pItem.getTooltipLines(
            Item.TooltipContext.of(pMinecraft.level),
            pMinecraft.player,
            pMinecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
    }

    protected void insertText(String pText, boolean pOverwrite) {
    }

    public boolean handleComponentClicked(@Nullable Style pStyle) {
        if (pStyle == null) {
            return false;
        } else {
            ClickEvent clickevent = pStyle.getClickEvent();
            if (hasShiftDown()) {
                if (pStyle.getInsertion() != null) {
                    this.insertText(pStyle.getInsertion(), false);
                }
            } else if (clickevent != null) {
                if (clickevent.getAction() == ClickEvent.Action.OPEN_URL) {
                    if (!this.minecraft.options.chatLinks().get()) {
                        return false;
                    }

                    try {
                        URI uri = Util.parseAndValidateUntrustedUri(clickevent.getValue());
                        if (this.minecraft.options.chatLinksPrompt().get()) {
                            this.minecraft.setScreen(new ConfirmLinkScreen(p_351659_ -> {
                                if (p_351659_) {
                                    Util.getPlatform().openUri(uri);
                                }

                                this.minecraft.setScreen(this);
                            }, clickevent.getValue(), false));
                        } else {
                            Util.getPlatform().openUri(uri);
                        }
                    } catch (URISyntaxException urisyntaxexception) {
                        LOGGER.error("Can't open url for {}", clickevent, urisyntaxexception);
                    }
                } else if (clickevent.getAction() == ClickEvent.Action.OPEN_FILE) {
                    Util.getPlatform().openFile(new File(clickevent.getValue()));
                } else if (clickevent.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
                    this.insertText(StringUtil.filterText(clickevent.getValue()), true);
                } else if (clickevent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                    String s = StringUtil.filterText(clickevent.getValue());
                    if (s.startsWith("/")) {
                        if (!this.minecraft.player.connection.sendUnsignedCommand(s.substring(1))) {
                            LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", s);
                        }
                    } else {
                        LOGGER.error("Failed to run command without '/' prefix from click event: '{}'", s);
                    }
                } else if (clickevent.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD) {
                    this.minecraft.keyboardHandler.setClipboard(clickevent.getValue());
                } else {
                    LOGGER.error("Don't know how to handle {}", clickevent);
                }

                return true;
            }

            return false;
        }
    }

    public final void init(Minecraft pMinecraft, int pWidth, int pHeight) {
        this.minecraft = pMinecraft;
        this.font = pMinecraft.font;
        this.width = pWidth;
        this.height = pHeight;
        if (!this.initialized) {
            if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Pre(this, this.children, this::addEventWidget, this::removeWidget)).isCanceled()) {
            this.init();
            this.setInitialFocus();
            }
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Post(this, this.children, this::addEventWidget, this::removeWidget));
        } else {
            this.repositionElements();
        }

        this.initialized = true;
        this.triggerImmediateNarration(false);
        this.suppressNarration(NARRATE_SUPPRESS_AFTER_INIT_TIME);
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.clearFocus();
        if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Pre(this, this.children, this::addEventWidget, this::removeWidget)).isCanceled()) {
        this.init();
        this.setInitialFocus();
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Post(this, this.children, this::addEventWidget, this::removeWidget));
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    protected void init() {
    }

    public void tick() {
    }

    public void removed() {
    }

    public void added() {
    }

    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.minecraft.level == null) {
            this.renderPanorama(pGuiGraphics, pPartialTick);
        }

        this.renderBlurredBackground(pPartialTick);
        this.renderMenuBackground(pGuiGraphics);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.BackgroundRendered(this, pGuiGraphics));
    }

    protected void renderBlurredBackground(float pPartialTick) {
        this.minecraft.gameRenderer.processBlurEffect(pPartialTick);
        this.minecraft.getMainRenderTarget().bindWrite(false);
    }

    protected void renderPanorama(GuiGraphics pGuiGraphics, float pPartialTick) {
        PANORAMA.render(pGuiGraphics, this.width, this.height, 1.0F, pPartialTick);
    }

    protected void renderMenuBackground(GuiGraphics pPartialTick) {
        this.renderMenuBackground(pPartialTick, 0, 0, this.width, this.height);
    }

    protected void renderMenuBackground(GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight) {
        renderMenuBackgroundTexture(
            pGuiGraphics, this.minecraft.level == null ? MENU_BACKGROUND : INWORLD_MENU_BACKGROUND, pX, pY, 0.0F, 0.0F, pWidth, pHeight
        );
    }

    public static void renderMenuBackgroundTexture(
        GuiGraphics pGuiGraphics, ResourceLocation pTexture, int pX, int pY, float pUOffset, float pVOffset, int pWidth, int pHeight
    ) {
        int i = 32;
        RenderSystem.enableBlend();
        pGuiGraphics.blit(pTexture, pX, pY, 0, pUOffset, pVOffset, pWidth, pHeight, 32, 32);
        RenderSystem.disableBlend();
    }

    public void renderTransparentBackground(GuiGraphics pGuiGraphics) {
        pGuiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    public boolean isPauseScreen() {
        return true;
    }

    public static boolean hasControlDown() {
        return Minecraft.ON_OSX
            ? InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 343)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 347)
            : InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 341)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 345);
    }

    public static boolean hasShiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344);
    }

    public static boolean hasAltDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342)
            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 346);
    }

    public static boolean isCut(int pKeyCode) {
        return pKeyCode == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isPaste(int pKeyCode) {
        return pKeyCode == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isCopy(int pKeyCode) {
        return pKeyCode == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isSelectAll(int pKeyCode) {
        return pKeyCode == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    protected void repositionElements() {
        this.rebuildWidgets();
    }

    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        this.width = pWidth;
        this.height = pHeight;
        this.repositionElements();
    }

    public static void wrapScreenError(Runnable pAction, String pErrorDesc, String pScreenName) {
        try {
            pAction.run();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, pErrorDesc);
            CrashReportCategory crashreportcategory = crashreport.addCategory("Affected screen");
            crashreportcategory.setDetail("Screen name", () -> pScreenName);
            throw new ReportedException(crashreport);
        }
    }

    protected boolean isValidCharacterForName(String pText, char pCharTyped, int pCursorPos) {
        int i = pText.indexOf(58);
        int j = pText.indexOf(47);
        if (pCharTyped == ':') {
            return (j == -1 || pCursorPos <= j) && i == -1;
        } else {
            return pCharTyped == '/'
                ? pCursorPos > i
                : pCharTyped == '_' || pCharTyped == '-' || pCharTyped >= 'a' && pCharTyped <= 'z' || pCharTyped >= '0' && pCharTyped <= '9' || pCharTyped == '.';
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
        return true;
    }

    public void onFilesDrop(List<Path> pPacks) {
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    private void scheduleNarration(long pDelay, boolean pStopSuppression) {
        this.nextNarrationTime = Util.getMillis() + pDelay;
        if (pStopSuppression) {
            this.narrationSuppressTime = Long.MIN_VALUE;
        }
    }

    private void suppressNarration(long pTime) {
        this.narrationSuppressTime = Util.getMillis() + pTime;
    }

    public void afterMouseMove() {
        this.scheduleNarration(750L, false);
    }

    public void afterMouseAction() {
        this.scheduleNarration(200L, true);
    }

    public void afterKeyboardAction() {
        this.scheduleNarration(200L, true);
    }

    private boolean shouldRunNarration() {
        return this.minecraft.getNarrator().isActive();
    }

    public void handleDelayedNarration() {
        if (this.shouldRunNarration()) {
            long i = Util.getMillis();
            if (i > this.nextNarrationTime && i > this.narrationSuppressTime) {
                this.runNarration(true);
                this.nextNarrationTime = Long.MAX_VALUE;
            }
        }
    }

    public void triggerImmediateNarration(boolean pOnlyNarrateNew) {
        if (this.shouldRunNarration()) {
            this.runNarration(pOnlyNarrateNew);
        }
    }

    private void runNarration(boolean pOnlyNarrateNew) {
        this.narrationState.update(this::updateNarrationState);
        String s = this.narrationState.collectNarrationText(!pOnlyNarrateNew);
        if (!s.isEmpty()) {
            this.minecraft.getNarrator().sayNow(s);
        }
    }

    protected boolean shouldNarrateNavigation() {
        return true;
    }

    protected void updateNarrationState(NarrationElementOutput p_169396_) {
        p_169396_.add(NarratedElementType.TITLE, this.getNarrationMessage());
        if (this.shouldNarrateNavigation()) {
            p_169396_.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }

        this.updateNarratedWidget(p_169396_);
    }

    protected void updateNarratedWidget(NarrationElementOutput pNarrationElementOutput) {
        List<NarratableEntry> list = this.narratables
            .stream()
            .filter(NarratableEntry::isActive)
            .sorted(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup))
            .toList();
        Screen.NarratableSearchResult screen$narratablesearchresult = findNarratableWidget(list, this.lastNarratable);
        if (screen$narratablesearchresult != null) {
            if (screen$narratablesearchresult.priority.isTerminal()) {
                this.lastNarratable = screen$narratablesearchresult.entry;
            }

            if (list.size() > 1) {
                pNarrationElementOutput.add(
                    NarratedElementType.POSITION, Component.translatable("narrator.position.screen", screen$narratablesearchresult.index + 1, list.size())
                );
                if (screen$narratablesearchresult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                    pNarrationElementOutput.add(NarratedElementType.USAGE, this.getUsageNarration());
                }
            }

            screen$narratablesearchresult.entry.updateNarration(pNarrationElementOutput.nest());
        }
    }

    protected Component getUsageNarration() {
        return Component.translatable("narration.component_list.usage");
    }

    @Nullable
    public static Screen.NarratableSearchResult findNarratableWidget(List<? extends NarratableEntry> pEntries, @Nullable NarratableEntry pTarget) {
        Screen.NarratableSearchResult screen$narratablesearchresult = null;
        Screen.NarratableSearchResult screen$narratablesearchresult1 = null;
        int i = 0;

        for (int j = pEntries.size(); i < j; i++) {
            NarratableEntry narratableentry = pEntries.get(i);
            NarratableEntry.NarrationPriority narratableentry$narrationpriority = narratableentry.narrationPriority();
            if (narratableentry$narrationpriority.isTerminal()) {
                if (narratableentry != pTarget) {
                    return new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
                }

                screen$narratablesearchresult1 = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
            } else if (narratableentry$narrationpriority.compareTo(
                    screen$narratablesearchresult != null ? screen$narratablesearchresult.priority : NarratableEntry.NarrationPriority.NONE
                )
                > 0) {
                screen$narratablesearchresult = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
            }
        }

        return screen$narratablesearchresult != null ? screen$narratablesearchresult : screen$narratablesearchresult1;
    }

    public void updateNarratorStatus(boolean p_352211_) {
        if (p_352211_) {
            this.scheduleNarration(NARRATE_DELAY_NARRATOR_ENABLED, false);
        }

        if (this.narratorButton != null) {
            this.narratorButton.setValue(this.minecraft.options.narrator().get());
        }
    }

    protected void clearTooltipForNextRenderPass() {
        this.deferredTooltipRendering = null;
    }

    public void setTooltipForNextRenderPass(List<FormattedCharSequence> pTooltip) {
        this.setTooltipForNextRenderPass(pTooltip, DefaultTooltipPositioner.INSTANCE, true);
    }

    public void setTooltipForNextRenderPass(List<FormattedCharSequence> pTooltip, ClientTooltipPositioner pPositioner, boolean pOverride) {
        if (this.deferredTooltipRendering == null || pOverride) {
            this.deferredTooltipRendering = new Screen.DeferredTooltipRendering(pTooltip, pPositioner);
        }
    }

    public void setTooltipForNextRenderPass(Component pTooltip) {
        this.setTooltipForNextRenderPass(Tooltip.splitTooltip(this.minecraft, pTooltip));
    }

    public void setTooltipForNextRenderPass(Tooltip pTooltip, ClientTooltipPositioner pPositioner, boolean pOverride) {
        this.setTooltipForNextRenderPass(pTooltip.toCharSequence(this.minecraft), pPositioner, pOverride);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(0, 0, this.width, this.height);
    }

    @Nullable
    public Music getBackgroundMusic() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    static record DeferredTooltipRendering(List<FormattedCharSequence> tooltip, ClientTooltipPositioner positioner) {
    }

    @OnlyIn(Dist.CLIENT)
    public static class NarratableSearchResult {
        public final NarratableEntry entry;
        public final int index;
        public final NarratableEntry.NarrationPriority priority;

        public NarratableSearchResult(NarratableEntry pEntry, int pIndex, NarratableEntry.NarrationPriority pPriority) {
            this.entry = pEntry;
            this.index = pIndex;
            this.priority = pPriority;
        }
    }

    private void addEventWidget(GuiEventListener b) {
        if (b instanceof Renderable r)
            this.renderables.add(r);
        if (b instanceof NarratableEntry ne)
            this.narratables.add(ne);
        children.add(b);
    }
}

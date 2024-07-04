package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.TextRenderingUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonLinks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSelectWorldTemplateScreen extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
    private static final Component SELECT_BUTTON_NAME = Component.translatable("mco.template.button.select");
    private static final Component TRAILER_BUTTON_NAME = Component.translatable("mco.template.button.trailer");
    private static final Component PUBLISHER_BUTTON_NAME = Component.translatable("mco.template.button.publisher");
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_SPACING = 10;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    final Consumer<WorldTemplate> callback;
    RealmsSelectWorldTemplateScreen.WorldTemplateList worldTemplateList;
    private final RealmsServer.WorldType worldType;
    private Button selectButton;
    private Button trailerButton;
    private Button publisherButton;
    @Nullable
    WorldTemplate selectedTemplate = null;
    @Nullable
    String currentLink;
    @Nullable
    private Component[] warning;
    @Nullable
    List<TextRenderingUtils.Line> noTemplatesMessage;

    public RealmsSelectWorldTemplateScreen(Component pTitle, Consumer<WorldTemplate> pCallback, RealmsServer.WorldType pWorldType) {
        this(pTitle, pCallback, pWorldType, null);
    }

    public RealmsSelectWorldTemplateScreen(
        Component pTitle, Consumer<WorldTemplate> pCallback, RealmsServer.WorldType pWorldType, @Nullable WorldTemplatePaginatedList pWorldTemplatePaginatedList
    ) {
        super(pTitle);
        this.callback = pCallback;
        this.worldType = pWorldType;
        if (pWorldTemplatePaginatedList == null) {
            this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList();
            this.fetchTemplatesAsync(new WorldTemplatePaginatedList(10));
        } else {
            this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList(Lists.newArrayList(pWorldTemplatePaginatedList.templates));
            this.fetchTemplatesAsync(pWorldTemplatePaginatedList);
        }
    }

    public void setWarning(Component... pWarning) {
        this.warning = pWarning;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(this.title, this.font);
        this.worldTemplateList = this.layout.addToContents(new RealmsSelectWorldTemplateScreen.WorldTemplateList(this.worldTemplateList.getTemplates()));
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(10));
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        this.trailerButton = linearlayout.addChild(Button.builder(TRAILER_BUTTON_NAME, p_89701_ -> this.onTrailer()).width(100).build());
        this.selectButton = linearlayout.addChild(Button.builder(SELECT_BUTTON_NAME, p_89696_ -> this.selectTemplate()).width(100).build());
        linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_89691_ -> this.onClose()).width(100).build());
        this.publisherButton = linearlayout.addChild(Button.builder(PUBLISHER_BUTTON_NAME, p_89679_ -> this.onPublish()).width(100).build());
        this.updateButtonStates();
        this.layout.visitWidgets(p_321348_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_321348_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.worldTemplateList.setSize(this.width, this.height - this.layout.getFooterHeight() - this.getHeaderHeight());
        this.layout.arrangeElements();
    }

    @Override
    public Component getNarrationMessage() {
        List<Component> list = Lists.newArrayListWithCapacity(2);
        list.add(this.title);
        if (this.warning != null) {
            list.addAll(Arrays.asList(this.warning));
        }

        return CommonComponents.joinLines(list);
    }

    void updateButtonStates() {
        this.publisherButton.visible = this.selectedTemplate != null && !this.selectedTemplate.link.isEmpty();
        this.trailerButton.visible = this.selectedTemplate != null && !this.selectedTemplate.trailer.isEmpty();
        this.selectButton.active = this.selectedTemplate != null;
    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    private void selectTemplate() {
        if (this.selectedTemplate != null) {
            this.callback.accept(this.selectedTemplate);
        }
    }

    private void onTrailer() {
        if (this.selectedTemplate != null && !this.selectedTemplate.trailer.isBlank()) {
            ConfirmLinkScreen.confirmLinkNow(this, this.selectedTemplate.trailer);
        }
    }

    private void onPublish() {
        if (this.selectedTemplate != null && !this.selectedTemplate.link.isBlank()) {
            ConfirmLinkScreen.confirmLinkNow(this, this.selectedTemplate.link);
        }
    }

    private void fetchTemplatesAsync(final WorldTemplatePaginatedList pOutput) {
        (new Thread("realms-template-fetcher") {
                @Override
                public void run() {
                    WorldTemplatePaginatedList worldtemplatepaginatedlist = pOutput;
                    RealmsClient realmsclient = RealmsClient.create();

                    while (worldtemplatepaginatedlist != null) {
                        Either<WorldTemplatePaginatedList, Exception> either = RealmsSelectWorldTemplateScreen.this.fetchTemplates(
                            worldtemplatepaginatedlist, realmsclient
                        );
                        worldtemplatepaginatedlist = RealmsSelectWorldTemplateScreen.this.minecraft
                            .submit(
                                () -> {
                                    if (either.right().isPresent()) {
                                        RealmsSelectWorldTemplateScreen.LOGGER.error("Couldn't fetch templates", either.right().get());
                                        if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                            RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(
                                                I18n.get("mco.template.select.failure")
                                            );
                                        }

                                        return null;
                                    } else {
                                        WorldTemplatePaginatedList worldtemplatepaginatedlist1 = either.left().get();

                                        for (WorldTemplate worldtemplate : worldtemplatepaginatedlist1.templates) {
                                            RealmsSelectWorldTemplateScreen.this.worldTemplateList.addEntry(worldtemplate);
                                        }

                                        if (worldtemplatepaginatedlist1.templates.isEmpty()) {
                                            if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                                String s = I18n.get("mco.template.select.none", "%link");
                                                TextRenderingUtils.LineSegment textrenderingutils$linesegment = TextRenderingUtils.LineSegment.link(
                                                    I18n.get("mco.template.select.none.linkTitle"), CommonLinks.REALMS_CONTENT_CREATION.toString()
                                                );
                                                RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(
                                                    s, textrenderingutils$linesegment
                                                );
                                            }

                                            return null;
                                        } else {
                                            return worldtemplatepaginatedlist1;
                                        }
                                    }
                                }
                            )
                            .join();
                    }
                }
            })
            .start();
    }

    Either<WorldTemplatePaginatedList, Exception> fetchTemplates(WorldTemplatePaginatedList pTemplates, RealmsClient pRealmsClient) {
        try {
            return Either.left(pRealmsClient.fetchWorldTemplates(pTemplates.page + 1, pTemplates.size, this.worldType));
        } catch (RealmsServiceException realmsserviceexception) {
            return Either.right(realmsserviceexception);
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
        this.currentLink = null;
        if (this.noTemplatesMessage != null) {
            this.renderMultilineMessage(pGuiGraphics, pMouseX, pMouseY, this.noTemplatesMessage);
        }

        if (this.warning != null) {
            for (int i = 0; i < this.warning.length; i++) {
                Component component = this.warning[i];
                pGuiGraphics.drawCenteredString(this.font, component, this.width / 2, row(-1 + i), -6250336);
            }
        }
    }

    private void renderMultilineMessage(GuiGraphics pGuiGraphics, int pX, int pY, List<TextRenderingUtils.Line> pLines) {
        for (int i = 0; i < pLines.size(); i++) {
            TextRenderingUtils.Line textrenderingutils$line = pLines.get(i);
            int j = row(4 + i);
            int k = textrenderingutils$line.segments.stream().mapToInt(p_280748_ -> this.font.width(p_280748_.renderedText())).sum();
            int l = this.width / 2 - k / 2;

            for (TextRenderingUtils.LineSegment textrenderingutils$linesegment : textrenderingutils$line.segments) {
                int i1 = textrenderingutils$linesegment.isLink() ? 3368635 : -1;
                int j1 = pGuiGraphics.drawString(this.font, textrenderingutils$linesegment.renderedText(), l, j, i1);
                if (textrenderingutils$linesegment.isLink() && pX > l && pX < j1 && pY > j - 3 && pY < j + 8) {
                    this.setTooltipForNextRenderPass(Component.literal(textrenderingutils$linesegment.getLinkUrl()));
                    this.currentLink = textrenderingutils$linesegment.getLinkUrl();
                }

                l = j1;
            }
        }
    }

    int getHeaderHeight() {
        return this.warning != null ? row(1) : 33;
    }

    @OnlyIn(Dist.CLIENT)
    class Entry extends ObjectSelectionList.Entry<RealmsSelectWorldTemplateScreen.Entry> {
        private static final WidgetSprites WEBSITE_LINK_SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("icon/link"), ResourceLocation.withDefaultNamespace("icon/link_highlighted")
        );
        private static final WidgetSprites TRAILER_LINK_SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("icon/video_link"), ResourceLocation.withDefaultNamespace("icon/video_link_highlighted")
        );
        private static final Component PUBLISHER_LINK_TOOLTIP = Component.translatable("mco.template.info.tooltip");
        private static final Component TRAILER_LINK_TOOLTIP = Component.translatable("mco.template.trailer.tooltip");
        public final WorldTemplate template;
        private long lastClickTime;
        @Nullable
        private ImageButton websiteButton;
        @Nullable
        private ImageButton trailerButton;

        public Entry(WorldTemplate pTemplate) {
            this.template = pTemplate;
            if (!pTemplate.link.isBlank()) {
                this.websiteButton = new ImageButton(
                    15, 15, WEBSITE_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, pTemplate.link), PUBLISHER_LINK_TOOLTIP
                );
                this.websiteButton.setTooltip(Tooltip.create(PUBLISHER_LINK_TOOLTIP));
            }

            if (!pTemplate.trailer.isBlank()) {
                this.trailerButton = new ImageButton(
                    15, 15, TRAILER_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, pTemplate.trailer), TRAILER_LINK_TOOLTIP
                );
                this.trailerButton.setTooltip(Tooltip.create(TRAILER_LINK_TOOLTIP));
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
            RealmsSelectWorldTemplateScreen.this.selectedTemplate = this.template;
            RealmsSelectWorldTemplateScreen.this.updateButtonStates();
            if (Util.getMillis() - this.lastClickTime < 250L && this.isFocused()) {
                RealmsSelectWorldTemplateScreen.this.callback.accept(this.template);
            }

            this.lastClickTime = Util.getMillis();
            if (this.websiteButton != null) {
                this.websiteButton.mouseClicked(pMouseX, pMouseY, pButton);
            }

            if (this.trailerButton != null) {
                this.trailerButton.mouseClicked(pMouseX, pMouseY, pButton);
            }

            return super.mouseClicked(pMouseX, pMouseY, pButton);
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
            pGuiGraphics.blit(
                RealmsTextureManager.worldTemplate(this.template.id, this.template.image), pLeft + 1, pTop + 1 + 1, 0.0F, 0.0F, 38, 38, 38, 38
            );
            pGuiGraphics.blitSprite(RealmsSelectWorldTemplateScreen.SLOT_FRAME_SPRITE, pLeft, pTop + 1, 40, 40);
            int i = 5;
            int j = RealmsSelectWorldTemplateScreen.this.font.width(this.template.version);
            if (this.websiteButton != null) {
                this.websiteButton.setPosition(pLeft + pWidth - j - this.websiteButton.getWidth() - 10, pTop);
                this.websiteButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            if (this.trailerButton != null) {
                this.trailerButton.setPosition(pLeft + pWidth - j - this.trailerButton.getWidth() * 2 - 15, pTop);
                this.trailerButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            int k = pLeft + 45 + 20;
            int l = pTop + 5;
            pGuiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.name, k, l, -1, false);
            pGuiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.version, pLeft + pWidth - j - 5, l, 7105644, false);
            pGuiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.author, k, l + 9 + 5, -6250336, false);
            if (!this.template.recommendedPlayers.isBlank()) {
                pGuiGraphics.drawString(
                    RealmsSelectWorldTemplateScreen.this.font, this.template.recommendedPlayers, k, pTop + pHeight - 9 / 2 - 5, 5000268, false
                );
            }
        }

        @Override
        public Component getNarration() {
            Component component = CommonComponents.joinLines(
                Component.literal(this.template.name),
                Component.translatable("mco.template.select.narrate.authors", this.template.author),
                Component.literal(this.template.recommendedPlayers),
                Component.translatable("mco.template.select.narrate.version", this.template.version)
            );
            return Component.translatable("narrator.select", component);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class WorldTemplateList extends RealmsObjectSelectionList<RealmsSelectWorldTemplateScreen.Entry> {
        public WorldTemplateList() {
            this(Collections.emptyList());
        }

        public WorldTemplateList(Iterable<WorldTemplate> pTemplates) {
            super(RealmsSelectWorldTemplateScreen.this.width, RealmsSelectWorldTemplateScreen.this.height - 33 - RealmsSelectWorldTemplateScreen.this.getHeaderHeight(), RealmsSelectWorldTemplateScreen.this.getHeaderHeight(), 46);
            pTemplates.forEach(this::addEntry);
        }

        public void addEntry(WorldTemplate p_313857_) {
            this.addEntry(RealmsSelectWorldTemplateScreen.this.new Entry(p_313857_));
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (currentLink != null) {
                ConfirmLinkScreen.confirmLinkNow(RealmsSelectWorldTemplateScreen.this, currentLink);
                return true;
            } else {
                return super.mouseClicked(pMouseX, pMouseY, pButton);
            }
        }

        public void setSelected(@Nullable RealmsSelectWorldTemplateScreen.Entry pSelected) {
            super.setSelected(pSelected);
            selectedTemplate = pSelected == null ? null : pSelected.template;
            updateButtonStates();
        }

        @Override
        public int getMaxPosition() {
            return this.getItemCount() * 46;
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        public boolean isEmpty() {
            return this.getItemCount() == 0;
        }

        public List<WorldTemplate> getTemplates() {
            return this.children().stream().map(p_313890_ -> p_313890_.template).collect(Collectors.toList());
        }
    }
}

package net.minecraft.client.gui.screens.worldselection;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmExperimentalFeaturesScreen extends Screen {
    private static final Component TITLE = Component.translatable("selectWorld.experimental.title");
    private static final Component MESSAGE = Component.translatable("selectWorld.experimental.message");
    private static final Component DETAILS_BUTTON = Component.translatable("selectWorld.experimental.details");
    private static final int COLUMN_SPACING = 10;
    private static final int DETAILS_BUTTON_WIDTH = 100;
    private final BooleanConsumer callback;
    final Collection<Pack> enabledPacks;
    private final GridLayout layout = new GridLayout().columnSpacing(10).rowSpacing(20);

    public ConfirmExperimentalFeaturesScreen(Collection<Pack> pEnabledPacks, BooleanConsumer pCallback) {
        super(TITLE);
        this.enabledPacks = pEnabledPacks;
        this.callback = pCallback;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), MESSAGE);
    }

    @Override
    protected void init() {
        super.init();
        GridLayout.RowHelper gridlayout$rowhelper = this.layout.createRowHelper(2);
        LayoutSettings layoutsettings = gridlayout$rowhelper.newCellSettings().alignHorizontallyCenter();
        gridlayout$rowhelper.addChild(new StringWidget(this.title, this.font), 2, layoutsettings);
        MultiLineTextWidget multilinetextwidget = gridlayout$rowhelper.addChild(
            new MultiLineTextWidget(MESSAGE, this.font).setCentered(true), 2, layoutsettings
        );
        multilinetextwidget.setMaxWidth(310);
        gridlayout$rowhelper.addChild(
            Button.builder(DETAILS_BUTTON, p_280898_ -> this.minecraft.setScreen(new ConfirmExperimentalFeaturesScreen.DetailsScreen())).width(100).build(),
            2,
            layoutsettings
        );
        gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_PROCEED, p_252248_ -> this.callback.accept(true)).build());
        gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_BACK, p_250397_ -> this.callback.accept(false)).build());
        this.layout.visitWidgets(p_321368_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_321368_);
        });
        this.layout.arrangeElements();
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        FrameLayout.alignInRectangle(this.layout, 0, 0, this.width, this.height, 0.5F, 0.5F);
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    @OnlyIn(Dist.CLIENT)
    class DetailsScreen extends Screen {
        private static final Component TITLE = Component.translatable("selectWorld.experimental.details.title");
        final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        @Nullable
        private ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList list;

        DetailsScreen() {
            super(TITLE);
        }

        @Override
        protected void init() {
            this.layout.addTitleHeader(TITLE, this.font);
            this.list = this.layout
                .addToContents(
                    new ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList(this.minecraft, ConfirmExperimentalFeaturesScreen.this.enabledPacks)
                );
            this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_251286_ -> this.onClose()).build());
            this.layout.visitWidgets(p_329748_ -> {
                AbstractWidget abstractwidget = this.addRenderableWidget(p_329748_);
            });
            this.repositionElements();
        }

        @Override
        protected void repositionElements() {
            if (this.list != null) {
                this.list.updateSize(this.width, this.layout);
            }

            this.layout.arrangeElements();
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(ConfirmExperimentalFeaturesScreen.this);
        }

        @OnlyIn(Dist.CLIENT)
        class PackList extends ObjectSelectionList<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
            public PackList(Minecraft pMinecraft, Collection<Pack> pEnabledPacks) {
                super(
                    pMinecraft, DetailsScreen.this.width, DetailsScreen.this.layout.getContentHeight(), DetailsScreen.this.layout.getHeaderHeight(), (9 + 2) * 3
                );

                for (Pack pack : pEnabledPacks) {
                    String s = FeatureFlags.printMissingFlags(FeatureFlags.VANILLA_SET, pack.getRequestedFeatures());
                    if (!s.isEmpty()) {
                        Component component = ComponentUtils.mergeStyles(pack.getTitle().copy(), Style.EMPTY.withBold(true));
                        Component component1 = Component.translatable("selectWorld.experimental.details.entry", s);
                        this.addEntry(
                            DetailsScreen.this.new PackListEntry(
                                component, component1, MultiLineLabel.create(DetailsScreen.this.font, component1, this.getRowWidth())
                            )
                        );
                    }
                }
            }

            @Override
            public int getRowWidth() {
                return this.width * 3 / 4;
            }
        }

        @OnlyIn(Dist.CLIENT)
        class PackListEntry extends ObjectSelectionList.Entry<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
            private final Component packId;
            private final Component message;
            private final MultiLineLabel splitMessage;

            PackListEntry(Component pPackId, Component pMessage, MultiLineLabel pSplitMessage) {
                this.packId = pPackId;
                this.message = pMessage;
                this.splitMessage = pSplitMessage;
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
                pGuiGraphics.drawString(DetailsScreen.this.minecraft.font, this.packId, pLeft, pTop, -1);
                this.splitMessage.renderLeftAligned(pGuiGraphics, pLeft, pTop + 12, 9, -1);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.packId, this.message));
            }
        }
    }
}

package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.dto.Backup;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsBackupInfoScreen extends RealmsScreen {
    private static final Component TITLE = Component.translatable("mco.backup.info.title");
    private static final Component UNKNOWN = Component.translatable("mco.backup.unknown");
    private final Screen lastScreen;
    final Backup backup;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private RealmsBackupInfoScreen.BackupInfoList backupInfoList;

    public RealmsBackupInfoScreen(Screen pLastScreen, Backup pBackup) {
        super(TITLE);
        this.lastScreen = pLastScreen;
        this.backup = pBackup;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.backupInfoList = this.layout.addToContents(new RealmsBackupInfoScreen.BackupInfoList(this.minecraft));
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_299754_ -> this.onClose()).build());
        this.repositionElements();
        this.layout.visitWidgets(p_321336_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_321336_);
        });
    }

    @Override
    protected void repositionElements() {
        this.backupInfoList.setSize(this.width, this.layout.getContentHeight());
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    Component checkForSpecificMetadata(String pKey, String pValue) {
        String s = pKey.toLowerCase(Locale.ROOT);
        if (s.contains("game") && s.contains("mode")) {
            return this.gameModeMetadata(pValue);
        } else {
            return (Component)(s.contains("game") && s.contains("difficulty") ? this.gameDifficultyMetadata(pValue) : Component.literal(pValue));
        }
    }

    private Component gameDifficultyMetadata(String pValue) {
        try {
            return RealmsSlotOptionsScreen.DIFFICULTIES.get(Integer.parseInt(pValue)).getDisplayName();
        } catch (Exception exception) {
            return UNKNOWN;
        }
    }

    private Component gameModeMetadata(String pValue) {
        try {
            return RealmsSlotOptionsScreen.GAME_MODES.get(Integer.parseInt(pValue)).getShortDisplayName();
        } catch (Exception exception) {
            return UNKNOWN;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class BackupInfoList extends ObjectSelectionList<RealmsBackupInfoScreen.BackupInfoListEntry> {
        public BackupInfoList(Minecraft pMinecraft) {
            super(
                pMinecraft,
                RealmsBackupInfoScreen.this.width,
                RealmsBackupInfoScreen.this.layout.getContentHeight(),
                RealmsBackupInfoScreen.this.layout.getHeaderHeight(),
                36
            );
            if (RealmsBackupInfoScreen.this.backup.changeList != null) {
                RealmsBackupInfoScreen.this.backup
                    .changeList
                    .forEach((p_88084_, p_88085_) -> this.addEntry(RealmsBackupInfoScreen.this.new BackupInfoListEntry(p_88084_, p_88085_)));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class BackupInfoListEntry extends ObjectSelectionList.Entry<RealmsBackupInfoScreen.BackupInfoListEntry> {
        private static final Component TEMPLATE_NAME = Component.translatable("mco.backup.entry.templateName");
        private static final Component GAME_DIFFICULTY = Component.translatable("mco.backup.entry.gameDifficulty");
        private static final Component NAME = Component.translatable("mco.backup.entry.name");
        private static final Component GAME_SERVER_VERSION = Component.translatable("mco.backup.entry.gameServerVersion");
        private static final Component UPLOADED = Component.translatable("mco.backup.entry.uploaded");
        private static final Component ENABLED_PACK = Component.translatable("mco.backup.entry.enabledPack");
        private static final Component DESCRIPTION = Component.translatable("mco.backup.entry.description");
        private static final Component GAME_MODE = Component.translatable("mco.backup.entry.gameMode");
        private static final Component SEED = Component.translatable("mco.backup.entry.seed");
        private static final Component WORLD_TYPE = Component.translatable("mco.backup.entry.worldType");
        private static final Component UNDEFINED = Component.translatable("mco.backup.entry.undefined");
        private final String key;
        private final String value;

        public BackupInfoListEntry(String pKey, String pValue) {
            this.key = pKey;
            this.value = pValue;
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
            pGuiGraphics.drawString(RealmsBackupInfoScreen.this.font, this.translateKey(this.key), pLeft, pTop, -6250336);
            pGuiGraphics.drawString(
                RealmsBackupInfoScreen.this.font, RealmsBackupInfoScreen.this.checkForSpecificMetadata(this.key, this.value), pLeft, pTop + 12, -1
            );
        }

        private Component translateKey(String pKey) {
            return switch (pKey) {
                case "template_name" -> TEMPLATE_NAME;
                case "game_difficulty" -> GAME_DIFFICULTY;
                case "name" -> NAME;
                case "game_server_version" -> GAME_SERVER_VERSION;
                case "uploaded" -> UPLOADED;
                case "enabled_packs" -> ENABLED_PACK;
                case "description" -> DESCRIPTION;
                case "game_mode" -> GAME_MODE;
                case "seed" -> SEED;
                case "world_type" -> WORLD_TYPE;
                default -> UNDEFINED;
            };
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
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.key + " " + this.value);
        }
    }
}

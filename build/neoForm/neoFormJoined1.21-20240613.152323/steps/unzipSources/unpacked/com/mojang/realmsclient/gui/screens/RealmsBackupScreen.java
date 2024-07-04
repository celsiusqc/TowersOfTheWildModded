package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBackupScreen extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.configure.world.backup");
    static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
    static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
    private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
    private static final Component DOWNLOAD_LATEST = Component.translatable("mco.backup.button.download");
    private static final String UPLOADED_KEY = "uploaded";
    private static final int PADDING = 8;
    final RealmsConfigureWorldScreen lastScreen;
    List<Backup> backups = Collections.emptyList();
    @Nullable
    RealmsBackupScreen.BackupObjectSelectionList backupList;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final int slotId;
    @Nullable
    Button downloadButton;
    final RealmsServer serverData;
    boolean noBackups = false;

    public RealmsBackupScreen(RealmsConfigureWorldScreen pLastScreen, RealmsServer pServerData, int pSlotId) {
        super(TITLE);
        this.lastScreen = pLastScreen;
        this.serverData = pServerData;
        this.slotId = pSlotId;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.backupList = this.layout.addToContents(new RealmsBackupScreen.BackupObjectSelectionList());
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.downloadButton = linearlayout.addChild(Button.builder(DOWNLOAD_LATEST, p_88185_ -> this.downloadClicked()).build());
        this.downloadButton.active = false;
        linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_329634_ -> this.onClose()).build());
        this.layout.visitWidgets(p_329637_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_329637_);
        });
        this.repositionElements();
        this.fetchRealmsBackups();
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
        if (this.noBackups && this.backupList != null) {
            pGuiGraphics.drawString(
                this.font,
                NO_BACKUPS_LABEL,
                this.width / 2 - this.font.width(NO_BACKUPS_LABEL) / 2,
                this.backupList.getY() + this.backupList.getHeight() / 2 - 9 / 2,
                -1,
                false
            );
        }
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.backupList != null) {
            this.backupList.updateSize(this.width, this.layout);
        }
    }

    private void fetchRealmsBackups() {
        (new Thread("Realms-fetch-backups") {
            @Override
            public void run() {
                RealmsClient realmsclient = RealmsClient.create();

                try {
                    List<Backup> list = realmsclient.backupsFor(RealmsBackupScreen.this.serverData.id).backups;
                    RealmsBackupScreen.this.minecraft.execute(() -> {
                        RealmsBackupScreen.this.backups = list;
                        RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                        if (!RealmsBackupScreen.this.noBackups && RealmsBackupScreen.this.downloadButton != null) {
                            RealmsBackupScreen.this.downloadButton.active = true;
                        }

                        if (RealmsBackupScreen.this.backupList != null) {
                            RealmsBackupScreen.this.backupList.children().clear();

                            for (Backup backup : RealmsBackupScreen.this.backups) {
                                RealmsBackupScreen.this.backupList.addEntry(backup);
                            }
                        }
                    });
                } catch (RealmsServiceException realmsserviceexception) {
                    RealmsBackupScreen.LOGGER.error("Couldn't request backups", (Throwable)realmsserviceexception);
                }
            }
        }).start();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void downloadClicked() {
        this.minecraft
            .setScreen(
                RealmsPopups.infoPopupScreen(
                    this,
                    Component.translatable("mco.configure.world.restore.download.question.line1"),
                    p_344114_ -> this.minecraft
                            .setScreen(
                                new RealmsLongRunningMcoTaskScreen(
                                    this.lastScreen.getNewScreen(),
                                    new DownloadTask(
                                        this.serverData.id,
                                        this.slotId,
                                        this.serverData.name
                                            + " ("
                                            + this.serverData.slots.get(this.serverData.activeSlot).getSlotName(this.serverData.activeSlot)
                                            + ")",
                                        this
                                    )
                                )
                            )
                )
            );
    }

    @OnlyIn(Dist.CLIENT)
    class BackupObjectSelectionList extends ContainerObjectSelectionList<RealmsBackupScreen.Entry> {
        private static final int ITEM_HEIGHT = 36;

        public BackupObjectSelectionList() {
            super(
                Minecraft.getInstance(),
                RealmsBackupScreen.this.width,
                RealmsBackupScreen.this.layout.getContentHeight(),
                RealmsBackupScreen.this.layout.getHeaderHeight(),
                36
            );
        }

        public void addEntry(Backup pBackup) {
            this.addEntry(RealmsBackupScreen.this.new Entry(pBackup));
        }

        @Override
        public int getMaxPosition() {
            return this.getItemCount() * 36 + this.headerHeight;
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Entry extends ContainerObjectSelectionList.Entry<RealmsBackupScreen.Entry> {
        private static final int Y_PADDING = 2;
        private final Backup backup;
        @Nullable
        private Button restoreButton;
        @Nullable
        private Button changesButton;
        private final List<AbstractWidget> children = new ArrayList<>();

        public Entry(Backup pBackup) {
            this.backup = pBackup;
            this.populateChangeList(pBackup);
            if (!pBackup.changeList.isEmpty()) {
                this.changesButton = Button.builder(
                        RealmsBackupScreen.HAS_CHANGES_TOOLTIP,
                        p_344115_ -> RealmsBackupScreen.this.minecraft.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, this.backup))
                    )
                    .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
                    .createNarration(
                        p_329639_ -> CommonComponents.joinForNarration(
                                Component.translatable("mco.backup.narration", this.getShortBackupDate()), p_329639_.get()
                            )
                    )
                    .build();
                this.children.add(this.changesButton);
            }

            if (!RealmsBackupScreen.this.serverData.expired) {
                this.restoreButton = Button.builder(RealmsBackupScreen.RESTORE_TOOLTIP, p_329642_ -> this.restoreClicked())
                    .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
                    .createNarration(
                        p_329640_ -> CommonComponents.joinForNarration(
                                Component.translatable("mco.backup.narration", this.getShortBackupDate()), p_329640_.get()
                            )
                    )
                    .build();
                this.children.add(this.restoreButton);
            }
        }

        private void populateChangeList(Backup pBackup) {
            int i = RealmsBackupScreen.this.backups.indexOf(pBackup);
            if (i != RealmsBackupScreen.this.backups.size() - 1) {
                Backup backup = RealmsBackupScreen.this.backups.get(i + 1);

                for (String s : pBackup.metadata.keySet()) {
                    if (!s.contains("uploaded") && backup.metadata.containsKey(s)) {
                        if (!pBackup.metadata.get(s).equals(backup.metadata.get(s))) {
                            this.addToChangeList(s);
                        }
                    } else {
                        this.addToChangeList(s);
                    }
                }
            }
        }

        private void addToChangeList(String pChange) {
            if (pChange.contains("uploaded")) {
                String s = DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
                this.backup.changeList.put(pChange, s);
                this.backup.setUploadedVersion(true);
            } else {
                this.backup.changeList.put(pChange, this.backup.metadata.get(pChange));
            }
        }

        private String getShortBackupDate() {
            return DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
        }

        private void restoreClicked() {
            Component component = RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate);
            Component component1 = Component.translatable("mco.configure.world.restore.question.line1", this.getShortBackupDate(), component);
            RealmsBackupScreen.this.minecraft
                .setScreen(
                    RealmsPopups.warningPopupScreen(
                        RealmsBackupScreen.this,
                        component1,
                        p_344116_ -> RealmsBackupScreen.this.minecraft
                                .setScreen(
                                    new RealmsLongRunningMcoTaskScreen(
                                        RealmsBackupScreen.this.lastScreen.getNewScreen(),
                                        new RestoreTask(this.backup, RealmsBackupScreen.this.serverData.id, RealmsBackupScreen.this.lastScreen)
                                    )
                                )
                    )
                );
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
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
            int i = pTop + pHeight / 2;
            int j = i - 9 - 2;
            int k = i + 2;
            int l = this.backup.isUploadedVersion() ? -8388737 : -1;
            pGuiGraphics.drawString(
                RealmsBackupScreen.this.font,
                Component.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate)),
                pLeft,
                j,
                l,
                false
            );
            pGuiGraphics.drawString(RealmsBackupScreen.this.font, this.getMediumDatePresentation(this.backup.lastModifiedDate), pLeft, k, 5000268, false);
            int i1 = 0;
            int j1 = pTop + pHeight / 2 - 10;
            if (this.restoreButton != null) {
                i1 += this.restoreButton.getWidth() + 8;
                this.restoreButton.setX(pLeft + pWidth - i1);
                this.restoreButton.setY(j1);
                this.restoreButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            if (this.changesButton != null) {
                i1 += this.changesButton.getWidth() + 8;
                this.changesButton.setX(pLeft + pWidth - i1);
                this.changesButton.setY(j1);
                this.changesButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }
        }

        private String getMediumDatePresentation(Date pDate) {
            return DateFormat.getDateTimeInstance(3, 3).format(pDate);
        }
    }
}

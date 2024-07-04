package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBrokenWorldScreen extends RealmsScreen {
    private static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_BUTTON_WIDTH = 80;
    private final Screen lastScreen;
    @Nullable
    private RealmsServer serverData;
    private final long serverId;
    private final Component[] message = new Component[]{
        Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")
    };
    private int leftX;
    private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
    private int animTick;

    public RealmsBrokenWorldScreen(Screen pLastScreen, long pServerId, boolean pIsMinigame) {
        super(pIsMinigame ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
        this.lastScreen = pLastScreen;
        this.serverId = pServerId;
    }

    @Override
    public void init() {
        this.leftX = this.width / 2 - 150;
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK, p_300624_ -> this.onClose()).bounds((this.width - 150) / 2, row(13) - 5, 150, 20).build()
        );
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        } else {
            this.addButtons();
        }
    }

    @Override
    public Component getNarrationMessage() {
        return ComponentUtils.formatList(Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), CommonComponents.SPACE);
    }

    private void addButtons() {
        for (Entry<Integer, RealmsWorldOptions> entry : this.serverData.slots.entrySet()) {
            int i = entry.getKey();
            boolean flag = i != this.serverData.activeSlot || this.serverData.isMinigameActive();
            Button button;
            if (flag) {
                button = Button.builder(
                        Component.translatable("mco.brokenworld.play"),
                        p_305620_ -> this.minecraft
                                .setScreen(
                                    new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, i, this::doSwitchOrReset))
                                )
                    )
                    .bounds(this.getFramePositionX(i), row(8), 80, 20)
                    .build();
                button.active = !this.serverData.slots.get(i).empty;
            } else {
                button = Button.builder(
                        Component.translatable("mco.brokenworld.download"),
                        p_344120_ -> this.minecraft
                                .setScreen(
                                    RealmsPopups.infoPopupScreen(
                                        this, Component.translatable("mco.configure.world.restore.download.question.line1"), p_344118_ -> this.downloadWorld(i)
                                    )
                                )
                    )
                    .bounds(this.getFramePositionX(i), row(8), 80, 20)
                    .build();
            }

            if (this.slotsThatHasBeenDownloaded.contains(i)) {
                button.active = false;
                button.setMessage(Component.translatable("mco.brokenworld.downloaded"));
            }

            this.addRenderableWidget(button);
        }
    }

    @Override
    public void tick() {
        this.animTick++;
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
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);

        for (int i = 0; i < this.message.length; i++) {
            pGuiGraphics.drawCenteredString(this.font, this.message[i], this.width / 2, row(-1) + 3 + i * 12, -6250336);
        }

        if (this.serverData != null) {
            for (Entry<Integer, RealmsWorldOptions> entry : this.serverData.slots.entrySet()) {
                if (entry.getValue().templateImage != null && entry.getValue().templateId != -1L) {
                    this.drawSlotFrame(
                        pGuiGraphics,
                        this.getFramePositionX(entry.getKey()),
                        row(1) + 5,
                        pMouseX,
                        pMouseY,
                        this.serverData.activeSlot == entry.getKey() && !this.isMinigame(),
                        entry.getValue().getSlotName(entry.getKey()),
                        entry.getKey(),
                        entry.getValue().templateId,
                        entry.getValue().templateImage,
                        entry.getValue().empty
                    );
                } else {
                    this.drawSlotFrame(
                        pGuiGraphics,
                        this.getFramePositionX(entry.getKey()),
                        row(1) + 5,
                        pMouseX,
                        pMouseY,
                        this.serverData.activeSlot == entry.getKey() && !this.isMinigame(),
                        entry.getValue().getSlotName(entry.getKey()),
                        entry.getKey(),
                        -1L,
                        null,
                        entry.getValue().empty
                    );
                }
            }
        }
    }

    private int getFramePositionX(int pIndex) {
        return this.leftX + (pIndex - 1) * 110;
    }

    private void fetchServerData(long pServerId) {
        new Thread(() -> {
            RealmsClient realmsclient = RealmsClient.create();

            try {
                this.serverData = realmsclient.getOwnRealm(pServerId);
                this.addButtons();
            } catch (RealmsServiceException realmsserviceexception) {
                LOGGER.error("Couldn't get own world", (Throwable)realmsserviceexception);
                this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this.lastScreen));
            }
        }).start();
    }

    public void doSwitchOrReset() {
        new Thread(
                () -> {
                    RealmsClient realmsclient = RealmsClient.create();
                    if (this.serverData.state == RealmsServer.State.CLOSED) {
                        this.minecraft
                            .execute(
                                () -> this.minecraft
                                        .setScreen(new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, true, this.minecraft)))
                            );
                    } else {
                        try {
                            RealmsServer realmsserver = realmsclient.getOwnRealm(this.serverId);
                            this.minecraft.execute(() -> RealmsMainScreen.play(realmsserver, this));
                        } catch (RealmsServiceException realmsserviceexception) {
                            LOGGER.error("Couldn't get own world", (Throwable)realmsserviceexception);
                            this.minecraft.execute(() -> this.minecraft.setScreen(this.lastScreen));
                        }
                    }
                }
            )
            .start();
    }

    private void downloadWorld(int pSlotIndex) {
        RealmsClient realmsclient = RealmsClient.create();

        try {
            WorldDownload worlddownload = realmsclient.requestDownloadInfo(this.serverData.id, pSlotIndex);
            RealmsDownloadLatestWorldScreen realmsdownloadlatestworldscreen = new RealmsDownloadLatestWorldScreen(
                this, worlddownload, this.serverData.getWorldName(pSlotIndex), p_329645_ -> {
                    if (p_329645_) {
                        this.slotsThatHasBeenDownloaded.add(pSlotIndex);
                        this.clearWidgets();
                        this.addButtons();
                    } else {
                        this.minecraft.setScreen(this);
                    }
                }
            );
            this.minecraft.setScreen(realmsdownloadlatestworldscreen);
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't download world data", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private boolean isMinigame() {
        return this.serverData != null && this.serverData.isMinigameActive();
    }

    private void drawSlotFrame(
        GuiGraphics pGuiGraphics,
        int pX,
        int pY,
        int pMouseX,
        int pMouseY,
        boolean pIsActiveNonMinigame,
        String pText,
        int pSlotIndex,
        long pTemplateId,
        @Nullable String pTemplateImage,
        boolean pHasTemplateImage
    ) {
        ResourceLocation resourcelocation;
        if (pHasTemplateImage) {
            resourcelocation = RealmsWorldSlotButton.EMPTY_SLOT_LOCATION;
        } else if (pTemplateImage != null && pTemplateId != -1L) {
            resourcelocation = RealmsTextureManager.worldTemplate(String.valueOf(pTemplateId), pTemplateImage);
        } else if (pSlotIndex == 1) {
            resourcelocation = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1;
        } else if (pSlotIndex == 2) {
            resourcelocation = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2;
        } else if (pSlotIndex == 3) {
            resourcelocation = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3;
        } else {
            resourcelocation = RealmsTextureManager.worldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage);
        }

        if (!pIsActiveNonMinigame) {
            pGuiGraphics.setColor(0.56F, 0.56F, 0.56F, 1.0F);
        } else if (pIsActiveNonMinigame) {
            float f = 0.9F + 0.1F * Mth.cos((float)this.animTick * 0.2F);
            pGuiGraphics.setColor(f, f, f, 1.0F);
        }

        pGuiGraphics.blit(resourcelocation, pX + 3, pY + 3, 0.0F, 0.0F, 74, 74, 74, 74);
        if (pIsActiveNonMinigame) {
            pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            pGuiGraphics.setColor(0.56F, 0.56F, 0.56F, 1.0F);
        }

        pGuiGraphics.blitSprite(SLOT_FRAME_SPRITE, pX, pY, 80, 80);
        pGuiGraphics.drawCenteredString(this.font, pText, pX + 40, pY + 66, -1);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}

package com.mojang.realmsclient.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.util.RealmsTextureManager;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldSlotButton extends Button {
    private static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
    private static final ResourceLocation CHECKMARK_SPRITE = ResourceLocation.withDefaultNamespace("icon/checkmark");
    public static final ResourceLocation EMPTY_SLOT_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/empty_frame.png");
    public static final ResourceLocation DEFAULT_WORLD_SLOT_1 = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_0.png");
    public static final ResourceLocation DEFAULT_WORLD_SLOT_2 = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_2.png");
    public static final ResourceLocation DEFAULT_WORLD_SLOT_3 = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_3.png");
    private static final Component SLOT_ACTIVE_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.active");
    private static final Component SWITCH_TO_MINIGAME_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.minigame");
    private static final Component SWITCH_TO_WORLD_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip");
    static final Component MINIGAME = Component.translatable("mco.worldSlot.minigame");
    private final int slotIndex;
    @Nullable
    private RealmsWorldSlotButton.State state;

    public RealmsWorldSlotButton(int pX, int pY, int pWidth, int pHeight, int pSlotIndex, Button.OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY, pOnPress, DEFAULT_NARRATION);
        this.slotIndex = pSlotIndex;
    }

    @Nullable
    public RealmsWorldSlotButton.State getState() {
        return this.state;
    }

    public void setServerData(RealmsServer pServerData) {
        this.state = new RealmsWorldSlotButton.State(pServerData, this.slotIndex);
        this.setTooltipAndNarration(this.state, pServerData.minigameName);
    }

    private void setTooltipAndNarration(RealmsWorldSlotButton.State pState, @Nullable String pMinigameName) {
        Component component = switch (pState.action) {
            case SWITCH_SLOT -> pState.minigame ? SWITCH_TO_MINIGAME_SLOT_TOOLTIP : SWITCH_TO_WORLD_SLOT_TOOLTIP;
            case JOIN -> SLOT_ACTIVE_TOOLTIP;
            default -> null;
        };
        if (component != null) {
            this.setTooltip(Tooltip.create(component));
        }

        MutableComponent mutablecomponent = Component.literal(pState.slotName);
        if (pState.minigame && pMinigameName != null) {
            mutablecomponent = mutablecomponent.append(CommonComponents.SPACE).append(pMinigameName);
        }

        this.setMessage(mutablecomponent);
    }

    static RealmsWorldSlotButton.Action getAction(RealmsServer pRealmsServer, boolean pIsCurrentlyActiveSlot, boolean pMinigame) {
        if (pIsCurrentlyActiveSlot && !pRealmsServer.expired && pRealmsServer.state != RealmsServer.State.UNINITIALIZED) {
            return RealmsWorldSlotButton.Action.JOIN;
        } else {
            return pIsCurrentlyActiveSlot || pMinigame && pRealmsServer.expired ? RealmsWorldSlotButton.Action.NOTHING : RealmsWorldSlotButton.Action.SWITCH_SLOT;
        }
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.state != null) {
            int i = this.getX();
            int j = this.getY();
            boolean flag = this.isHoveredOrFocused();
            ResourceLocation resourcelocation;
            if (this.state.minigame) {
                resourcelocation = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
            } else if (this.state.empty) {
                resourcelocation = EMPTY_SLOT_LOCATION;
            } else if (this.state.image != null && this.state.imageId != -1L) {
                resourcelocation = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
            } else if (this.slotIndex == 1) {
                resourcelocation = DEFAULT_WORLD_SLOT_1;
            } else if (this.slotIndex == 2) {
                resourcelocation = DEFAULT_WORLD_SLOT_2;
            } else if (this.slotIndex == 3) {
                resourcelocation = DEFAULT_WORLD_SLOT_3;
            } else {
                resourcelocation = EMPTY_SLOT_LOCATION;
            }

            if (this.state.isCurrentlyActiveSlot) {
                pGuiGraphics.setColor(0.56F, 0.56F, 0.56F, 1.0F);
            }

            pGuiGraphics.blit(resourcelocation, i + 3, j + 3, 0.0F, 0.0F, 74, 74, 74, 74);
            boolean flag1 = flag && this.state.action != RealmsWorldSlotButton.Action.NOTHING;
            if (flag1) {
                pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (this.state.isCurrentlyActiveSlot) {
                pGuiGraphics.setColor(0.8F, 0.8F, 0.8F, 1.0F);
            } else {
                pGuiGraphics.setColor(0.56F, 0.56F, 0.56F, 1.0F);
            }

            pGuiGraphics.blitSprite(SLOT_FRAME_SPRITE, i, j, 80, 80);
            pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.state.isCurrentlyActiveSlot) {
                RenderSystem.enableBlend();
                pGuiGraphics.blitSprite(CHECKMARK_SPRITE, i + 67, j + 4, 9, 8);
                RenderSystem.disableBlend();
            }

            Font font = Minecraft.getInstance().font;
            pGuiGraphics.drawCenteredString(font, this.state.slotName, i + 40, j + 66, -1);
            pGuiGraphics.drawCenteredString(
                font, RealmsMainScreen.getVersionComponent(this.state.slotVersion, this.state.compatibility.isCompatible()), i + 40, j + 80 + 2, -1
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Action {
        NOTHING,
        SWITCH_SLOT,
        JOIN;
    }

    @OnlyIn(Dist.CLIENT)
    public static class State {
        final boolean isCurrentlyActiveSlot;
        final String slotName;
        final String slotVersion;
        final RealmsServer.Compatibility compatibility;
        final long imageId;
        @Nullable
        final String image;
        public final boolean empty;
        public final boolean minigame;
        public final RealmsWorldSlotButton.Action action;

        public State(RealmsServer pServer, int pSlot) {
            this.minigame = pSlot == 4;
            if (this.minigame) {
                this.isCurrentlyActiveSlot = pServer.isMinigameActive();
                this.slotName = RealmsWorldSlotButton.MINIGAME.getString();
                this.imageId = (long)pServer.minigameId;
                this.image = pServer.minigameImage;
                this.empty = pServer.minigameId == -1;
                this.slotVersion = "";
                this.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
            } else {
                RealmsWorldOptions realmsworldoptions = pServer.slots.get(pSlot);
                this.isCurrentlyActiveSlot = pServer.activeSlot == pSlot && !pServer.isMinigameActive();
                this.slotName = realmsworldoptions.getSlotName(pSlot);
                this.imageId = realmsworldoptions.templateId;
                this.image = realmsworldoptions.templateImage;
                this.empty = realmsworldoptions.empty;
                this.slotVersion = realmsworldoptions.version;
                this.compatibility = realmsworldoptions.compatibility;
            }

            this.action = RealmsWorldSlotButton.getAction(pServer, this.isCurrentlyActiveSlot, this.minigame);
        }
    }
}

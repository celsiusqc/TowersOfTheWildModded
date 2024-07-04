package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LockIconButton extends Button {
    private boolean locked;

    public LockIconButton(int pX, int pY, Button.OnPress pOnPress) {
        super(pX, pY, 20, 20, Component.translatable("narrator.button.difficulty_lock"), pOnPress, DEFAULT_NARRATION);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return CommonComponents.joinForNarration(
            super.createNarrationMessage(),
            this.isLocked()
                ? Component.translatable("narrator.button.difficulty_lock.locked")
                : Component.translatable("narrator.button.difficulty_lock.unlocked")
        );
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean pLocked) {
        this.locked = pLocked;
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        LockIconButton.Icon lockiconbutton$icon;
        if (!this.active) {
            lockiconbutton$icon = this.locked ? LockIconButton.Icon.LOCKED_DISABLED : LockIconButton.Icon.UNLOCKED_DISABLED;
        } else if (this.isHoveredOrFocused()) {
            lockiconbutton$icon = this.locked ? LockIconButton.Icon.LOCKED_HOVER : LockIconButton.Icon.UNLOCKED_HOVER;
        } else {
            lockiconbutton$icon = this.locked ? LockIconButton.Icon.LOCKED : LockIconButton.Icon.UNLOCKED;
        }

        pGuiGraphics.blitSprite(lockiconbutton$icon.sprite, this.getX(), this.getY(), this.width, this.height);
    }

    @OnlyIn(Dist.CLIENT)
    static enum Icon {
        LOCKED(ResourceLocation.withDefaultNamespace("widget/locked_button")),
        LOCKED_HOVER(ResourceLocation.withDefaultNamespace("widget/locked_button_highlighted")),
        LOCKED_DISABLED(ResourceLocation.withDefaultNamespace("widget/locked_button_disabled")),
        UNLOCKED(ResourceLocation.withDefaultNamespace("widget/unlocked_button")),
        UNLOCKED_HOVER(ResourceLocation.withDefaultNamespace("widget/unlocked_button_highlighted")),
        UNLOCKED_DISABLED(ResourceLocation.withDefaultNamespace("widget/unlocked_button_disabled"));

        final ResourceLocation sprite;

        private Icon(ResourceLocation pSprite) {
            this.sprite = pSprite;
        }
    }
}

package net.minecraft.client.gui.screens.options.controls;

import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyBindsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("controls.keybinds.title");
    @Nullable
    public KeyMapping selectedKey;
    public long lastKeySelection;
    private KeyBindsList keyBindsList;
    private Button resetButton;

    public KeyBindsScreen(Screen p_344996_, Options p_344771_) {
        super(p_344996_, p_344771_, TITLE);
    }

    @Override
    protected void addContents() {
        this.keyBindsList = this.layout.addToContents(new KeyBindsList(this, this.minecraft));
    }

    @Override
    protected void addOptions() {
    }

    @Override
    protected void addFooter() {
        this.resetButton = Button.builder(Component.translatable("controls.resetAll"), p_346345_ -> {
            for (KeyMapping keymapping : this.options.keyMappings) {
                keymapping.setToDefault();
            }

            this.keyBindsList.resetMappingAndUpdateButtons();
        }).build();
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearlayout.addChild(this.resetButton);
        linearlayout.addChild(Button.builder(CommonComponents.GUI_DONE, p_345169_ -> this.onClose()).build());
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.keyBindsList.updateSize(this.width, this.layout);
    }

    @Override
    public boolean mouseClicked(double p_346251_, double p_345041_, int p_345147_) {
        if (this.selectedKey != null) {
            this.options.setKey(this.selectedKey, InputConstants.Type.MOUSE.getOrCreate(p_345147_));
            this.selectedKey = null;
            this.keyBindsList.resetMappingAndUpdateButtons();
            return true;
        } else {
            return super.mouseClicked(p_346251_, p_345041_, p_345147_);
        }
    }

    @Override
    public boolean keyPressed(int p_345810_, int p_345447_, int p_344981_) {
        if (this.selectedKey != null) {
            if (p_345810_ == 256) {
                this.selectedKey.setKeyModifierAndCode(net.neoforged.neoforge.client.settings.KeyModifier.getActiveModifier(), InputConstants.UNKNOWN);
                this.options.setKey(this.selectedKey, InputConstants.UNKNOWN);
            } else {
                this.selectedKey.setKeyModifierAndCode(net.neoforged.neoforge.client.settings.KeyModifier.getActiveModifier(), InputConstants.getKey(p_345810_, p_345447_));
                this.options.setKey(this.selectedKey, InputConstants.getKey(p_345810_, p_345447_));
            }

            if(!net.neoforged.neoforge.client.settings.KeyModifier.isKeyCodeModifier(this.selectedKey.getKey()))
            this.selectedKey = null;
            this.lastKeySelection = Util.getMillis();
            this.keyBindsList.resetMappingAndUpdateButtons();
            return true;
        } else {
            return super.keyPressed(p_345810_, p_345447_, p_344981_);
        }
    }

    @Override
    public void render(GuiGraphics p_346209_, int p_344846_, int p_346350_, float p_345601_) {
        super.render(p_346209_, p_344846_, p_346350_, p_345601_);
        boolean flag = false;

        for (KeyMapping keymapping : this.options.keyMappings) {
            if (!keymapping.isDefault()) {
                flag = true;
                break;
            }
        }

        this.resetButton.active = flag;
    }
}

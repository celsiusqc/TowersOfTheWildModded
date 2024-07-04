package net.minecraft.client.gui.screens.recipebook;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeBookTabButton extends StateSwitchingButton {
    private static final WidgetSprites SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/tab"), ResourceLocation.withDefaultNamespace("recipe_book/tab_selected")
    );
    private final RecipeBookCategories category;
    private static final float ANIMATION_TIME = 15.0F;
    private float animationTime;

    public RecipeBookTabButton(RecipeBookCategories pCategory) {
        super(0, 0, 35, 27, false);
        this.category = pCategory;
        this.initTextureValues(SPRITES);
    }

    public void startAnimation(Minecraft pMinecraft) {
        ClientRecipeBook clientrecipebook = pMinecraft.player.getRecipeBook();
        List<RecipeCollection> list = clientrecipebook.getCollection(this.category);
        if (pMinecraft.player.containerMenu instanceof RecipeBookMenu) {
            for (RecipeCollection recipecollection : list) {
                for (RecipeHolder<?> recipeholder : recipecollection.getRecipes(
                    clientrecipebook.isFiltering((RecipeBookMenu<?, ?>)pMinecraft.player.containerMenu)
                )) {
                    if (clientrecipebook.willHighlight(recipeholder)) {
                        this.animationTime = 15.0F;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.sprites != null) {
            if (this.animationTime > 0.0F) {
                float f = 1.0F + 0.1F * (float)Math.sin((double)(this.animationTime / 15.0F * (float) Math.PI));
                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose().translate((float)(this.getX() + 8), (float)(this.getY() + 12), 0.0F);
                pGuiGraphics.pose().scale(1.0F, f, 1.0F);
                pGuiGraphics.pose().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)), 0.0F);
            }

            Minecraft minecraft = Minecraft.getInstance();
            RenderSystem.disableDepthTest();
            ResourceLocation resourcelocation = this.sprites.get(true, this.isStateTriggered);
            int i = this.getX();
            if (this.isStateTriggered) {
                i -= 2;
            }

            pGuiGraphics.blitSprite(resourcelocation, i, this.getY(), this.width, this.height);
            RenderSystem.enableDepthTest();
            this.renderIcon(pGuiGraphics, minecraft.getItemRenderer());
            if (this.animationTime > 0.0F) {
                pGuiGraphics.pose().popPose();
                this.animationTime -= pPartialTick;
            }
        }
    }

    private void renderIcon(GuiGraphics pGuiGraphics, ItemRenderer pItemRenderer) {
        List<ItemStack> list = this.category.getIconItems();
        int i = this.isStateTriggered ? -2 : 0;
        if (list.size() == 1) {
            pGuiGraphics.renderFakeItem(list.get(0), this.getX() + 9 + i, this.getY() + 5);
        } else if (list.size() == 2) {
            pGuiGraphics.renderFakeItem(list.get(0), this.getX() + 3 + i, this.getY() + 5);
            pGuiGraphics.renderFakeItem(list.get(1), this.getX() + 14 + i, this.getY() + 5);
        }
    }

    public RecipeBookCategories getCategory() {
        return this.category;
    }

    public boolean updateVisibility(ClientRecipeBook pRecipeBook) {
        List<RecipeCollection> list = pRecipeBook.getCollection(this.category);
        this.visible = false;
        if (list != null) {
            for (RecipeCollection recipecollection : list) {
                if (recipecollection.hasKnownRecipes() && recipecollection.hasFitting()) {
                    this.visible = true;
                    break;
                }
            }
        }

        return this.visible;
    }
}

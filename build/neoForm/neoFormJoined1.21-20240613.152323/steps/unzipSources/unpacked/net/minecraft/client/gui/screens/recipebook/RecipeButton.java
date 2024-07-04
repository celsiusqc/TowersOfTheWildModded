package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeButton extends AbstractWidget {
    private static final ResourceLocation SLOT_MANY_CRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_craftable");
    private static final ResourceLocation SLOT_CRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    private static final ResourceLocation SLOT_MANY_UNCRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_uncraftable");
    private static final ResourceLocation SLOT_UNCRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final float ANIMATION_TIME = 15.0F;
    private static final int BACKGROUND_SIZE = 25;
    public static final int TICKS_TO_SWAP = 30;
    private static final Component MORE_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.moreRecipes");
    private RecipeBookMenu<?, ?> menu;
    private RecipeBook book;
    private RecipeCollection collection;
    private float time;
    private float animationTime;
    private int currentIndex;

    public RecipeButton() {
        super(0, 0, 25, 25, CommonComponents.EMPTY);
    }

    public void init(RecipeCollection pCollection, RecipeBookPage pRecipeBookPage) {
        this.collection = pCollection;
        this.menu = (RecipeBookMenu<?, ?>)pRecipeBookPage.getMinecraft().player.containerMenu;
        this.book = pRecipeBookPage.getRecipeBook();
        List<RecipeHolder<?>> list = pCollection.getRecipes(this.book.isFiltering(this.menu));

        for (RecipeHolder<?> recipeholder : list) {
            if (this.book.willHighlight(recipeholder)) {
                pRecipeBookPage.recipesShown(list);
                this.animationTime = 15.0F;
                break;
            }
        }
    }

    public RecipeCollection getCollection() {
        return this.collection;
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (!Screen.hasControlDown()) {
            this.time += pPartialTick;
        }

        ResourceLocation resourcelocation;
        if (this.collection.hasCraftable()) {
            if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
                resourcelocation = SLOT_MANY_CRAFTABLE_SPRITE;
            } else {
                resourcelocation = SLOT_CRAFTABLE_SPRITE;
            }
        } else if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
            resourcelocation = SLOT_MANY_UNCRAFTABLE_SPRITE;
        } else {
            resourcelocation = SLOT_UNCRAFTABLE_SPRITE;
        }

        boolean flag = this.animationTime > 0.0F;
        if (flag) {
            float f = 1.0F + 0.1F * (float)Math.sin((double)(this.animationTime / 15.0F * (float) Math.PI));
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate((float)(this.getX() + 8), (float)(this.getY() + 12), 0.0F);
            pGuiGraphics.pose().scale(f, f, 1.0F);
            pGuiGraphics.pose().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)), 0.0F);
            this.animationTime -= pPartialTick;
        }

        pGuiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.width, this.height);
        List<RecipeHolder<?>> list = this.getOrderedRecipes();
        this.currentIndex = Mth.floor(this.time / 30.0F) % list.size();
        ItemStack itemstack = list.get(this.currentIndex).value().getResultItem(this.collection.registryAccess());
        int i = 4;
        if (this.collection.hasSingleResultItem() && this.getOrderedRecipes().size() > 1) {
            pGuiGraphics.renderItem(itemstack, this.getX() + i + 1, this.getY() + i + 1, 0, 10);
            i--;
        }

        pGuiGraphics.renderFakeItem(itemstack, this.getX() + i, this.getY() + i);
        if (flag) {
            pGuiGraphics.pose().popPose();
        }
    }

    private List<RecipeHolder<?>> getOrderedRecipes() {
        List<RecipeHolder<?>> list = this.collection.getDisplayRecipes(true);
        if (!this.book.isFiltering(this.menu)) {
            list.addAll(this.collection.getDisplayRecipes(false));
        }

        return list;
    }

    public boolean isOnlyOption() {
        return this.getOrderedRecipes().size() == 1;
    }

    public RecipeHolder<?> getRecipe() {
        List<RecipeHolder<?>> list = this.getOrderedRecipes();
        return list.get(this.currentIndex);
    }

    public List<Component> getTooltipText() {
        ItemStack itemstack = this.getOrderedRecipes().get(this.currentIndex).value().getResultItem(this.collection.registryAccess());
        List<Component> list = Lists.newArrayList(Screen.getTooltipFromItem(Minecraft.getInstance(), itemstack));
        if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
            list.add(MORE_RECIPES_TOOLTIP);
        }

        return list;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        ItemStack itemstack = this.getOrderedRecipes().get(this.currentIndex).value().getResultItem(this.collection.registryAccess());
        pNarrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("narration.recipe", itemstack.getHoverName()));
        if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
            pNarrationElementOutput.add(
                NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"), Component.translatable("narration.recipe.usage.more")
            );
        } else {
            pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        }
    }

    @Override
    public int getWidth() {
        return 25;
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return pButton == 0 || pButton == 1;
    }
}

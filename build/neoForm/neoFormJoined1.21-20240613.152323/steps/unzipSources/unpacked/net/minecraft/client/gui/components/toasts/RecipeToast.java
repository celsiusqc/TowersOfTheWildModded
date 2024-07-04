package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/recipe");
    private static final long DISPLAY_TIME = 5000L;
    private static final Component TITLE_TEXT = Component.translatable("recipe.toast.title");
    private static final Component DESCRIPTION_TEXT = Component.translatable("recipe.toast.description");
    private final List<RecipeHolder<?>> recipes = Lists.newArrayList();
    private long lastChanged;
    private boolean changed;

    public RecipeToast(RecipeHolder<?> pRecipe) {
        this.recipes.add(pRecipe);
    }

    @Override
    public Toast.Visibility render(GuiGraphics pGuiGraphics, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
        if (this.changed) {
            this.lastChanged = pTimeSinceLastVisible;
            this.changed = false;
        }

        if (this.recipes.isEmpty()) {
            return Toast.Visibility.HIDE;
        } else {
            pGuiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, TITLE_TEXT, 30, 7, -11534256, false);
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, DESCRIPTION_TEXT, 30, 18, -16777216, false);
            RecipeHolder<?> recipeholder = this.recipes
                .get(
                    (int)(
                        (double)pTimeSinceLastVisible
                            / Math.max(1.0, 5000.0 * pToastComponent.getNotificationDisplayTimeMultiplier() / (double)this.recipes.size())
                            % (double)this.recipes.size()
                    )
                );
            ItemStack itemstack = recipeholder.value().getToastSymbol();
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().scale(0.6F, 0.6F, 1.0F);
            pGuiGraphics.renderFakeItem(itemstack, 3, 3);
            pGuiGraphics.pose().popPose();
            pGuiGraphics.renderFakeItem(recipeholder.value().getResultItem(pToastComponent.getMinecraft().level.registryAccess()), 8, 8);
            return (double)(pTimeSinceLastVisible - this.lastChanged) >= 5000.0 * pToastComponent.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE
                : Toast.Visibility.SHOW;
        }
    }

    private void addItem(RecipeHolder<?> pRecipe) {
        this.recipes.add(pRecipe);
        this.changed = true;
    }

    public static void addOrUpdate(ToastComponent pToastComponent, RecipeHolder<?> pRecipe) {
        RecipeToast recipetoast = pToastComponent.getToast(RecipeToast.class, NO_TOKEN);
        if (recipetoast == null) {
            pToastComponent.addToast(new RecipeToast(pRecipe));
        } else {
            recipetoast.addItem(pRecipe);
        }
    }
}

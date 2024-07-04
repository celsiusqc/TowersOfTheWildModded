package net.minecraft.stats;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeBook {
    protected final Set<ResourceLocation> known = Sets.newHashSet();
    protected final Set<ResourceLocation> highlight = Sets.newHashSet();
    private final RecipeBookSettings bookSettings = new RecipeBookSettings();

    public void copyOverData(RecipeBook pOther) {
        this.known.clear();
        this.highlight.clear();
        this.bookSettings.replaceFrom(pOther.bookSettings);
        this.known.addAll(pOther.known);
        this.highlight.addAll(pOther.highlight);
    }

    public void add(RecipeHolder<?> pRecipe) {
        if (!pRecipe.value().isSpecial()) {
            this.add(pRecipe.id());
        }
    }

    protected void add(ResourceLocation pRecipeId) {
        this.known.add(pRecipeId);
    }

    public boolean contains(@Nullable RecipeHolder<?> pRecipe) {
        return pRecipe == null ? false : this.known.contains(pRecipe.id());
    }

    public boolean contains(ResourceLocation pRecipeId) {
        return this.known.contains(pRecipeId);
    }

    public void remove(RecipeHolder<?> pRecipe) {
        this.remove(pRecipe.id());
    }

    protected void remove(ResourceLocation pRecipeId) {
        this.known.remove(pRecipeId);
        this.highlight.remove(pRecipeId);
    }

    public boolean willHighlight(RecipeHolder<?> pRecipe) {
        return this.highlight.contains(pRecipe.id());
    }

    public void removeHighlight(RecipeHolder<?> pRecipe) {
        this.highlight.remove(pRecipe.id());
    }

    public void addHighlight(RecipeHolder<?> pRecipe) {
        this.addHighlight(pRecipe.id());
    }

    protected void addHighlight(ResourceLocation pRecipeId) {
        this.highlight.add(pRecipeId);
    }

    public boolean isOpen(RecipeBookType pBookType) {
        return this.bookSettings.isOpen(pBookType);
    }

    public void setOpen(RecipeBookType pBookType, boolean pOpen) {
        this.bookSettings.setOpen(pBookType, pOpen);
    }

    public boolean isFiltering(RecipeBookMenu<?, ?> pBookMenu) {
        return this.isFiltering(pBookMenu.getRecipeBookType());
    }

    public boolean isFiltering(RecipeBookType pBookType) {
        return this.bookSettings.isFiltering(pBookType);
    }

    public void setFiltering(RecipeBookType pBookType, boolean pFiltering) {
        this.bookSettings.setFiltering(pBookType, pFiltering);
    }

    public void setBookSettings(RecipeBookSettings pSettings) {
        this.bookSettings.replaceFrom(pSettings);
    }

    public RecipeBookSettings getBookSettings() {
        return this.bookSettings.copy();
    }

    public void setBookSetting(RecipeBookType pBookType, boolean pOpen, boolean pFiltering) {
        this.bookSettings.setOpen(pBookType, pOpen);
        this.bookSettings.setFiltering(pBookType, pFiltering);
    }
}

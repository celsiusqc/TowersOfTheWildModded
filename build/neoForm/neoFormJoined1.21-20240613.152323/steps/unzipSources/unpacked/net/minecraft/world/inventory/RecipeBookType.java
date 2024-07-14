package net.minecraft.world.inventory;

@net.neoforged.fml.common.asm.enumextension.NetworkedEnum(net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck.CLIENTBOUND)
public enum RecipeBookType implements net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
    CRAFTING,
    FURNACE,
    BLAST_FURNACE,
    SMOKER;

    RecipeBookType() {
        if (ordinal() >= 4) {
            String name = this.name().toLowerCase(java.util.Locale.ROOT).replace("_", "");
            net.minecraft.stats.RecipeBookSettings.addTagsForType(this, "is" + name + "GuiOpen", "is" + name + "FilteringCraftable");
        }
    }

    public static net.neoforged.fml.common.asm.enumextension.ExtensionInfo getExtensionInfo() {
        return net.neoforged.fml.common.asm.enumextension.ExtensionInfo.nonExtended(RecipeBookType.class);
    }
}

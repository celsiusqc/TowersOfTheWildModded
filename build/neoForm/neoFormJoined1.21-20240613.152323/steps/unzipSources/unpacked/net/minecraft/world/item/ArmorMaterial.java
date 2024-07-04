package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.Ingredient;

public record ArmorMaterial(
    Map<ArmorItem.Type, Integer> defense,
    int enchantmentValue,
    Holder<SoundEvent> equipSound,
    Supplier<Ingredient> repairIngredient,
    List<ArmorMaterial.Layer> layers,
    float toughness,
    float knockbackResistance
) {
    public static final Codec<Holder<ArmorMaterial>> CODEC = BuiltInRegistries.ARMOR_MATERIAL.holderByNameCodec();

    public int getDefense(ArmorItem.Type pType) {
        return this.defense.getOrDefault(pType, 0);
    }

    public static final class Layer {
        private final ResourceLocation assetName;
        private final String suffix;
        private final boolean dyeable;
        private final ResourceLocation innerTexture;
        private final ResourceLocation outerTexture;

        public Layer(ResourceLocation pAssetName, String pSuffix, boolean pDyeable) {
            this.assetName = pAssetName;
            this.suffix = pSuffix;
            this.dyeable = pDyeable;
            this.innerTexture = this.resolveTexture(true);
            this.outerTexture = this.resolveTexture(false);
        }

        public Layer(ResourceLocation pAssetName) {
            this(pAssetName, "", false);
        }

        private ResourceLocation resolveTexture(boolean pInnerTexture) {
            return this.assetName
                .withPath(p_324187_ -> "textures/models/armor/" + this.assetName.getPath() + "_layer_" + (pInnerTexture ? 2 : 1) + this.suffix + ".png");
        }

        public ResourceLocation texture(boolean pInnerTexture) {
            return pInnerTexture ? this.innerTexture : this.outerTexture;
        }

        public boolean dyeable() {
            return this.dyeable;
        }
    }
}

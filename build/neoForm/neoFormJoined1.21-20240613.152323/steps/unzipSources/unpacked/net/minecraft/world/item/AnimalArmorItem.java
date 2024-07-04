package net.minecraft.world.item;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class AnimalArmorItem extends ArmorItem {
    private final ResourceLocation textureLocation;
    @Nullable
    private final ResourceLocation overlayTextureLocation;
    private final AnimalArmorItem.BodyType bodyType;

    public AnimalArmorItem(Holder<ArmorMaterial> pArmorMaterial, AnimalArmorItem.BodyType pBodyType, boolean pHasOverlay, Item.Properties pProperties) {
        super(pArmorMaterial, ArmorItem.Type.BODY, pProperties);
        this.bodyType = pBodyType;
        ResourceLocation resourcelocation = pBodyType.textureLocator.apply(pArmorMaterial.unwrapKey().orElseThrow().location());
        this.textureLocation = resourcelocation.withSuffix(".png");
        if (pHasOverlay) {
            this.overlayTextureLocation = resourcelocation.withSuffix("_overlay.png");
        } else {
            this.overlayTextureLocation = null;
        }
    }

    public ResourceLocation getTexture() {
        return this.textureLocation;
    }

    @Nullable
    public ResourceLocation getOverlayTexture() {
        return this.overlayTextureLocation;
    }

    public AnimalArmorItem.BodyType getBodyType() {
        return this.bodyType;
    }

    @Override
    public SoundEvent getBreakingSound() {
        return this.bodyType.breakingSound;
    }

    /**
     * Checks isDamagable and if it cannot be stacked
     */
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }

    public static enum BodyType {
        EQUESTRIAN(p_323547_ -> p_323547_.withPath(p_323717_ -> "textures/entity/horse/armor/horse_armor_" + p_323717_), SoundEvents.ITEM_BREAK),
        CANINE(p_323678_ -> p_323678_.withPath("textures/entity/wolf/wolf_armor"), SoundEvents.WOLF_ARMOR_BREAK);

        final Function<ResourceLocation, ResourceLocation> textureLocator;
        final SoundEvent breakingSound;

        private BodyType(Function<ResourceLocation, ResourceLocation> pTextureLocator, SoundEvent pBreakingSound) {
            this.textureLocator = pTextureLocator;
            this.breakingSound = pBreakingSound;
        }
    }
}

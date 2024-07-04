package net.minecraft.world.item.armortrim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public class ArmorTrim implements TooltipProvider {
    public static final Codec<ArmorTrim> CODEC = RecordCodecBuilder.create(
        p_337943_ -> p_337943_.group(
                    TrimMaterial.CODEC.fieldOf("material").forGetter(ArmorTrim::material),
                    TrimPattern.CODEC.fieldOf("pattern").forGetter(ArmorTrim::pattern),
                    Codec.BOOL.optionalFieldOf("show_in_tooltip", Boolean.valueOf(true)).forGetter(p_330108_ -> p_330108_.showInTooltip)
                )
                .apply(p_337943_, ArmorTrim::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorTrim> STREAM_CODEC = StreamCodec.composite(
        TrimMaterial.STREAM_CODEC,
        ArmorTrim::material,
        TrimPattern.STREAM_CODEC,
        ArmorTrim::pattern,
        ByteBufCodecs.BOOL,
        p_330107_ -> p_330107_.showInTooltip,
        ArmorTrim::new
    );
    private static final Component UPGRADE_TITLE = Component.translatable(
            Util.makeDescriptionId("item", ResourceLocation.withDefaultNamespace("smithing_template.upgrade"))
        )
        .withStyle(ChatFormatting.GRAY);
    private final Holder<TrimMaterial> material;
    private final Holder<TrimPattern> pattern;
    private final boolean showInTooltip;
    private final Function<Holder<ArmorMaterial>, ResourceLocation> innerTexture;
    private final Function<Holder<ArmorMaterial>, ResourceLocation> outerTexture;

    private ArmorTrim(
        Holder<TrimMaterial> pMaterial,
        Holder<TrimPattern> pPattern,
        boolean pShowInTooltip,
        Function<Holder<ArmorMaterial>, ResourceLocation> pInnerTexture,
        Function<Holder<ArmorMaterial>, ResourceLocation> pOuterTexture
    ) {
        this.material = pMaterial;
        this.pattern = pPattern;
        this.showInTooltip = pShowInTooltip;
        this.innerTexture = pInnerTexture;
        this.outerTexture = pOuterTexture;
    }

    public ArmorTrim(Holder<TrimMaterial> pMaterial, Holder<TrimPattern> pPattern, boolean p_331871_) {
        this.material = pMaterial;
        this.pattern = pPattern;
        this.innerTexture = Util.memoize(p_335286_ -> {
            ResourceLocation resourcelocation = pPattern.value().assetId();
            String s = getColorPaletteSuffix(pMaterial, p_335286_);
            return resourcelocation.withPath(p_266737_ -> "trims/models/armor/" + p_266737_ + "_leggings_" + s);
        });
        this.outerTexture = Util.memoize(p_335283_ -> {
            ResourceLocation resourcelocation = pPattern.value().assetId();
            String s = getColorPaletteSuffix(pMaterial, p_335283_);
            return resourcelocation.withPath(p_266864_ -> "trims/models/armor/" + p_266864_ + "_" + s);
        });
        this.showInTooltip = p_331871_;
    }

    public ArmorTrim(Holder<TrimMaterial> pMaterial, Holder<TrimPattern> pPattern) {
        this(pMaterial, pPattern, true);
    }

    private static String getColorPaletteSuffix(Holder<TrimMaterial> pTrimMaterial, Holder<ArmorMaterial> pArmorMaterial) {
        Map<Holder<ArmorMaterial>, String> map = pTrimMaterial.value().overrideArmorMaterials();
        String s = map.get(pArmorMaterial);
        return s != null ? s : pTrimMaterial.value().assetName();
    }

    public boolean hasPatternAndMaterial(Holder<TrimPattern> pPattern, Holder<TrimMaterial> pMaterial) {
        return pPattern.equals(this.pattern) && pMaterial.equals(this.material);
    }

    public Holder<TrimPattern> pattern() {
        return this.pattern;
    }

    public Holder<TrimMaterial> material() {
        return this.material;
    }

    public ResourceLocation innerTexture(Holder<ArmorMaterial> pArmorMaterial) {
        return this.innerTexture.apply(pArmorMaterial);
    }

    public ResourceLocation outerTexture(Holder<ArmorMaterial> pArmorMaterial) {
        return this.outerTexture.apply(pArmorMaterial);
    }

    @Override
    public boolean equals(Object pOther) {
        return !(pOther instanceof ArmorTrim armortrim)
            ? false
            : this.showInTooltip == armortrim.showInTooltip && this.pattern.equals(armortrim.pattern) && this.material.equals(armortrim.material);
    }

    @Override
    public int hashCode() {
        int i = this.material.hashCode();
        i = 31 * i + this.pattern.hashCode();
        return 31 * i + (this.showInTooltip ? 1 : 0);
    }

    @Override
    public void addToTooltip(Item.TooltipContext pContext, Consumer<Component> pTooltipAdder, TooltipFlag pTooltipFlag) {
        if (this.showInTooltip) {
            pTooltipAdder.accept(UPGRADE_TITLE);
            pTooltipAdder.accept(CommonComponents.space().append(this.pattern.value().copyWithStyle(this.material)));
            pTooltipAdder.accept(CommonComponents.space().append(this.material.value().description()));
        }
    }

    public ArmorTrim withTooltip(boolean pShowInTooltip) {
        return new ArmorTrim(this.material, this.pattern, pShowInTooltip, this.innerTexture, this.outerTexture);
    }
}

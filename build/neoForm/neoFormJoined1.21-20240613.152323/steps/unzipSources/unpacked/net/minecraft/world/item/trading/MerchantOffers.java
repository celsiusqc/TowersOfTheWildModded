package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
    public static final Codec<MerchantOffers> CODEC = MerchantOffer.CODEC.listOf().fieldOf("Recipes").xmap(MerchantOffers::new, Function.identity()).codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffers> STREAM_CODEC = MerchantOffer.STREAM_CODEC
        .apply(ByteBufCodecs.collection(MerchantOffers::new));

    public MerchantOffers() {
    }

    private MerchantOffers(int p_220323_) {
        super(p_220323_);
    }

    private MerchantOffers(Collection<MerchantOffer> p_323676_) {
        super(p_323676_);
    }

    @Nullable
    public MerchantOffer getRecipeFor(ItemStack pStackA, ItemStack pStackB, int pIndex) {
        if (pIndex > 0 && pIndex < this.size()) {
            MerchantOffer merchantoffer1 = this.get(pIndex);
            return merchantoffer1.satisfiedBy(pStackA, pStackB) ? merchantoffer1 : null;
        } else {
            for (int i = 0; i < this.size(); i++) {
                MerchantOffer merchantoffer = this.get(i);
                if (merchantoffer.satisfiedBy(pStackA, pStackB)) {
                    return merchantoffer;
                }
            }

            return null;
        }
    }

    public MerchantOffers copy() {
        MerchantOffers merchantoffers = new MerchantOffers(this.size());

        for (MerchantOffer merchantoffer : this) {
            merchantoffers.add(merchantoffer.copy());
        }

        return merchantoffers;
    }
}

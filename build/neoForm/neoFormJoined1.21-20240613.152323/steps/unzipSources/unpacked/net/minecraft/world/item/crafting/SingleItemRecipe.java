package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public abstract class SingleItemRecipe implements Recipe<SingleRecipeInput> {
    protected final Ingredient ingredient;
    protected final ItemStack result;
    private final RecipeType<?> type;
    private final RecipeSerializer<?> serializer;
    protected final String group;

    public SingleItemRecipe(RecipeType<?> pType, RecipeSerializer<?> pSerializer, String pGroup, Ingredient pIngredient, ItemStack pResult) {
        this.type = pType;
        this.serializer = pSerializer;
        this.group = pGroup;
        this.ingredient = pIngredient;
        this.result = pResult;
    }

    @Override
    public RecipeType<?> getType() {
        return this.type;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.serializer;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> nonnulllist = NonNullList.create();
        nonnulllist.add(this.ingredient);
        return nonnulllist;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    public ItemStack assemble(SingleRecipeInput p_345857_, HolderLookup.Provider p_335463_) {
        return this.result.copy();
    }

    public interface Factory<T extends SingleItemRecipe> {
        T create(String pGroup, Ingredient pIngredient, ItemStack pResult);
    }

    public static class Serializer<T extends SingleItemRecipe> implements RecipeSerializer<T> {
        final SingleItemRecipe.Factory<T> factory;
        private final MapCodec<T> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

        protected Serializer(SingleItemRecipe.Factory<T> pFactory) {
            this.factory = pFactory;
            this.codec = RecordCodecBuilder.mapCodec(
                p_340781_ -> p_340781_.group(
                            Codec.STRING.optionalFieldOf("group", "").forGetter(p_300947_ -> p_300947_.group),
                            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(p_301068_ -> p_301068_.ingredient),
                            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(p_302316_ -> p_302316_.result)
                        )
                        .apply(p_340781_, pFactory::create)
            );
            this.streamCodec = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                p_319737_ -> p_319737_.group,
                Ingredient.CONTENTS_STREAM_CODEC,
                p_319738_ -> p_319738_.ingredient,
                ItemStack.STREAM_CODEC,
                p_319736_ -> p_319736_.result,
                pFactory::create
            );
        }

        @Override
        public MapCodec<T> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
            return this.streamCodec;
        }
    }
}

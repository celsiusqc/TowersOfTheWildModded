package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the stack's name by copying it from somewhere else, such as the killing player.
 */
public class CopyNameFunction extends LootItemConditionalFunction {
    public static final MapCodec<CopyNameFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_298065_ -> commonFields(p_298065_)
                .and(CopyNameFunction.NameSource.CODEC.fieldOf("source").forGetter(p_298068_ -> p_298068_.source))
                .apply(p_298065_, CopyNameFunction::new)
    );
    private final CopyNameFunction.NameSource source;

    private CopyNameFunction(List<LootItemCondition> p_298700_, CopyNameFunction.NameSource p_80178_) {
        super(p_298700_);
        this.source = p_80178_;
    }

    @Override
    public LootItemFunctionType<CopyNameFunction> getType() {
        return LootItemFunctions.COPY_NAME;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.source.param);
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (pContext.getParamOrNull(this.source.param) instanceof Nameable nameable) {
            pStack.set(DataComponents.CUSTOM_NAME, nameable.getCustomName());
        }

        return pStack;
    }

    public static LootItemConditionalFunction.Builder<?> copyName(CopyNameFunction.NameSource pSource) {
        return simpleBuilder(p_298067_ -> new CopyNameFunction(p_298067_, pSource));
    }

    public static enum NameSource implements StringRepresentable {
        THIS("this", LootContextParams.THIS_ENTITY),
        ATTACKING_ENTITY("attacking_entity", LootContextParams.ATTACKING_ENTITY),
        LAST_DAMAGE_PLAYER("last_damage_player", LootContextParams.LAST_DAMAGE_PLAYER),
        BLOCK_ENTITY("block_entity", LootContextParams.BLOCK_ENTITY);

        public static final Codec<CopyNameFunction.NameSource> CODEC = StringRepresentable.fromEnum(CopyNameFunction.NameSource::values);
        private final String name;
        final LootContextParam<?> param;

        private NameSource(String pName, LootContextParam<?> pParam) {
            this.name = pName;
            this.param = pParam;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}

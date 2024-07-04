package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyComponentsFunction extends LootItemConditionalFunction {
    public static final MapCodec<CopyComponentsFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_338127_ -> commonFields(p_338127_)
                .and(
                    p_338127_.group(
                        CopyComponentsFunction.Source.CODEC.fieldOf("source").forGetter(p_331312_ -> p_331312_.source),
                        DataComponentType.CODEC.listOf().optionalFieldOf("include").forGetter(p_338132_ -> p_338132_.include),
                        DataComponentType.CODEC.listOf().optionalFieldOf("exclude").forGetter(p_338126_ -> p_338126_.exclude)
                    )
                )
                .apply(p_338127_, CopyComponentsFunction::new)
    );
    private final CopyComponentsFunction.Source source;
    private final Optional<List<DataComponentType<?>>> include;
    private final Optional<List<DataComponentType<?>>> exclude;
    private final Predicate<DataComponentType<?>> bakedPredicate;

    CopyComponentsFunction(
        List<LootItemCondition> p_330806_,
        CopyComponentsFunction.Source p_330881_,
        Optional<List<DataComponentType<?>>> p_338636_,
        Optional<List<DataComponentType<?>>> p_338680_
    ) {
        super(p_330806_);
        this.source = p_330881_;
        this.include = p_338636_.map(List::copyOf);
        this.exclude = p_338680_.map(List::copyOf);
        List<Predicate<DataComponentType<?>>> list = new ArrayList<>(2);
        p_338680_.ifPresent(p_338129_ -> list.add(p_338134_ -> !p_338129_.contains(p_338134_)));
        p_338636_.ifPresent(p_338131_ -> list.add(p_338131_::contains));
        this.bakedPredicate = Util.allOf(list);
    }

    @Override
    public LootItemFunctionType<CopyComponentsFunction> getType() {
        return LootItemFunctions.COPY_COMPONENTS;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.source.getReferencedContextParams();
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        DataComponentMap datacomponentmap = this.source.get(pContext);
        pStack.applyComponents(datacomponentmap.filter(this.bakedPredicate));
        return pStack;
    }

    public static CopyComponentsFunction.Builder copyComponents(CopyComponentsFunction.Source pSource) {
        return new CopyComponentsFunction.Builder(pSource);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<CopyComponentsFunction.Builder> {
        private final CopyComponentsFunction.Source source;
        private Optional<ImmutableList.Builder<DataComponentType<?>>> include = Optional.empty();
        private Optional<ImmutableList.Builder<DataComponentType<?>>> exclude = Optional.empty();

        Builder(CopyComponentsFunction.Source pSource) {
            this.source = pSource;
        }

        public CopyComponentsFunction.Builder include(DataComponentType<?> pInclude) {
            if (this.include.isEmpty()) {
                this.include = Optional.of(ImmutableList.builder());
            }

            this.include.get().add(pInclude);
            return this;
        }

        public CopyComponentsFunction.Builder exclude(DataComponentType<?> pExclude) {
            if (this.exclude.isEmpty()) {
                this.exclude = Optional.of(ImmutableList.builder());
            }

            this.exclude.get().add(pExclude);
            return this;
        }

        protected CopyComponentsFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyComponentsFunction(
                this.getConditions(), this.source, this.include.map(ImmutableList.Builder::build), this.exclude.map(ImmutableList.Builder::build)
            );
        }
    }

    public static enum Source implements StringRepresentable {
        BLOCK_ENTITY("block_entity");

        public static final Codec<CopyComponentsFunction.Source> CODEC = StringRepresentable.fromValues(CopyComponentsFunction.Source::values);
        private final String name;

        private Source(String pName) {
            this.name = pName;
        }

        public DataComponentMap get(LootContext pContext) {
            switch (this) {
                case BLOCK_ENTITY:
                    BlockEntity blockentity = pContext.getParamOrNull(LootContextParams.BLOCK_ENTITY);
                    return blockentity != null ? blockentity.collectComponents() : DataComponentMap.EMPTY;
                default:
                    throw new MatchException(null, null);
            }
        }

        public Set<LootContextParam<?>> getReferencedContextParams() {
            switch (this) {
                case BLOCK_ENTITY:
                    return Set.of(LootContextParams.BLOCK_ENTITY);
                default:
                    throw new MatchException(null, null);
            }
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}

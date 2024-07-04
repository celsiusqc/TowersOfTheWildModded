package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public interface PoolAliasBinding {
    Codec<PoolAliasBinding> CODEC = BuiltInRegistries.POOL_ALIAS_BINDING_TYPE.byNameCodec().dispatch(PoolAliasBinding::codec, Function.identity());

    void forEachResolved(RandomSource pRandom, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> pStucturePoolKey);

    Stream<ResourceKey<StructureTemplatePool>> allTargets();

    static Direct direct(String pAlias, String pTarget) {
        return direct(Pools.createKey(pAlias), Pools.createKey(pTarget));
    }

    static Direct direct(ResourceKey<StructureTemplatePool> pAlias, ResourceKey<StructureTemplatePool> pTarget) {
        return new Direct(pAlias, pTarget);
    }

    static Random random(String pAlias, SimpleWeightedRandomList<String> pTargets) {
        SimpleWeightedRandomList.Builder<ResourceKey<StructureTemplatePool>> builder = SimpleWeightedRandomList.builder();
        pTargets.unwrap().forEach(p_338103_ -> builder.add(Pools.createKey(p_338103_.data()), p_338103_.getWeight().asInt()));
        return random(Pools.createKey(pAlias), builder.build());
    }

    static Random random(ResourceKey<StructureTemplatePool> pAlias, SimpleWeightedRandomList<ResourceKey<StructureTemplatePool>> pTargets) {
        return new Random(pAlias, pTargets);
    }

    static RandomGroup randomGroup(SimpleWeightedRandomList<List<PoolAliasBinding>> pGroups) {
        return new RandomGroup(pGroups);
    }

    MapCodec<? extends PoolAliasBinding> codec();
}

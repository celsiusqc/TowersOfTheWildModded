package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class PoolAliasBindings {
    public static MapCodec<? extends PoolAliasBinding> bootstrap(Registry<MapCodec<? extends PoolAliasBinding>> pRegistry) {
        Registry.register(pRegistry, "random", Random.CODEC);
        Registry.register(pRegistry, "random_group", RandomGroup.CODEC);
        return Registry.register(pRegistry, "direct", Direct.CODEC);
    }

    public static void registerTargetsAsPools(
        BootstrapContext<StructureTemplatePool> pContext, Holder<StructureTemplatePool> pPool, List<PoolAliasBinding> pPoolAliasBindings
    ) {
        pPoolAliasBindings.stream()
            .flatMap(PoolAliasBinding::allTargets)
            .map(p_312156_ -> p_312156_.location().getPath())
            .forEach(
                p_321475_ -> Pools.register(
                        pContext,
                        p_321475_,
                        new StructureTemplatePool(
                            pPool, List.of(Pair.of(StructurePoolElement.single(p_321475_), 1)), StructureTemplatePool.Projection.RIGID
                        )
                    )
            );
    }
}

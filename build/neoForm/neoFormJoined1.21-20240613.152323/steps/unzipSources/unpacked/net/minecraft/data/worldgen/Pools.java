package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Pools {
    public static final ResourceKey<StructureTemplatePool> EMPTY = createKey("empty");

    public static ResourceKey<StructureTemplatePool> createKey(String pName) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace(pName));
    }

    public static ResourceKey<StructureTemplatePool> parseKey(String p_350360_) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.parse(p_350360_));
    }

    public static void register(BootstrapContext<StructureTemplatePool> pContext, String pName, StructureTemplatePool pPool) {
        pContext.register(createKey(pName), pPool);
    }

    public static void bootstrap(BootstrapContext<StructureTemplatePool> pContext) {
        HolderGetter<StructureTemplatePool> holdergetter = pContext.lookup(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> holder = holdergetter.getOrThrow(EMPTY);
        pContext.register(EMPTY, new StructureTemplatePool(holder, ImmutableList.of(), StructureTemplatePool.Projection.RIGID));
        BastionPieces.bootstrap(pContext);
        PillagerOutpostPools.bootstrap(pContext);
        VillagePools.bootstrap(pContext);
        AncientCityStructurePieces.bootstrap(pContext);
        TrailRuinsStructurePools.bootstrap(pContext);
        TrialChambersStructurePools.bootstrap(pContext);
    }
}

package net.minecraft.world.entity.animal;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class WolfVariants {
    public static final ResourceKey<WolfVariant> PALE = createKey("pale");
    public static final ResourceKey<WolfVariant> SPOTTED = createKey("spotted");
    public static final ResourceKey<WolfVariant> SNOWY = createKey("snowy");
    public static final ResourceKey<WolfVariant> BLACK = createKey("black");
    public static final ResourceKey<WolfVariant> ASHEN = createKey("ashen");
    public static final ResourceKey<WolfVariant> RUSTY = createKey("rusty");
    public static final ResourceKey<WolfVariant> WOODS = createKey("woods");
    public static final ResourceKey<WolfVariant> CHESTNUT = createKey("chestnut");
    public static final ResourceKey<WolfVariant> STRIPED = createKey("striped");
    public static final ResourceKey<WolfVariant> DEFAULT = PALE;

    private static ResourceKey<WolfVariant> createKey(String pName) {
        return ResourceKey.create(Registries.WOLF_VARIANT, ResourceLocation.withDefaultNamespace(pName));
    }

    static void register(BootstrapContext<WolfVariant> pContext, ResourceKey<WolfVariant> pKey, String pName, ResourceKey<Biome> pSpawnBiome) {
        register(pContext, pKey, pName, HolderSet.direct(pContext.lookup(Registries.BIOME).getOrThrow(pSpawnBiome)));
    }

    static void register(BootstrapContext<WolfVariant> pContext, ResourceKey<WolfVariant> pKey, String pName, TagKey<Biome> pSpawnBiomes) {
        register(pContext, pKey, pName, pContext.lookup(Registries.BIOME).getOrThrow(pSpawnBiomes));
    }

    static void register(BootstrapContext<WolfVariant> pContext, ResourceKey<WolfVariant> pKey, String pName, HolderSet<Biome> pSpawnBiomes) {
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("entity/wolf/" + pName);
        ResourceLocation resourcelocation1 = ResourceLocation.withDefaultNamespace("entity/wolf/" + pName + "_tame");
        ResourceLocation resourcelocation2 = ResourceLocation.withDefaultNamespace("entity/wolf/" + pName + "_angry");
        pContext.register(pKey, new WolfVariant(resourcelocation, resourcelocation1, resourcelocation2, pSpawnBiomes));
    }

    public static Holder<WolfVariant> getSpawnVariant(RegistryAccess pRegistryAccess, Holder<Biome> pBiome) {
        Registry<WolfVariant> registry = pRegistryAccess.registryOrThrow(Registries.WOLF_VARIANT);
        return registry.holders()
            .filter(p_332674_ -> p_332674_.value().biomes().contains(pBiome))
            .findFirst()
            .or(() -> registry.getHolder(DEFAULT))
            .or(registry::getAny)
            .orElseThrow();
    }

    public static void bootstrap(BootstrapContext<WolfVariant> pContext) {
        register(pContext, PALE, "wolf", Biomes.TAIGA);
        register(pContext, SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
        register(pContext, SNOWY, "wolf_snowy", Biomes.GROVE);
        register(pContext, BLACK, "wolf_black", Biomes.OLD_GROWTH_PINE_TAIGA);
        register(pContext, ASHEN, "wolf_ashen", Biomes.SNOWY_TAIGA);
        register(pContext, RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
        register(pContext, WOODS, "wolf_woods", Biomes.FOREST);
        register(pContext, CHESTNUT, "wolf_chestnut", Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        register(pContext, STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
    }
}

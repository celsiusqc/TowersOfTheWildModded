package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

/**
 * @deprecated Forge: Use {@link net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider} instead
 */
@Deprecated
public class RegistriesDatapackGenerator implements DataProvider {
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final java.util.function.Predicate<String> namespacePredicate;

    @Deprecated
    public RegistriesDatapackGenerator(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        this(pOutput, pRegistries, null);
    }

    public RegistriesDatapackGenerator(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries, @org.jetbrains.annotations.Nullable java.util.Set<String> modIds) {
        this.namespacePredicate = modIds == null ? namespace -> true : modIds::contains;
        this.registries = pRegistries;
        this.output = pOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        return this.registries
            .thenCompose(
                p_326736_ -> {
                    DynamicOps<JsonElement> dynamicops = p_326736_.createSerializationContext(JsonOps.INSTANCE);
                    return CompletableFuture.allOf(
                        net.neoforged.neoforge.registries.DataPackRegistriesHooks.getDataPackRegistriesWithDimensions()
                            .flatMap(
                                p_256552_ -> this.dumpRegistryCap(pOutput, p_326736_, dynamicops, (RegistryDataLoader.RegistryData<?>)p_256552_).stream()
                            )
                            .toArray(CompletableFuture[]::new)
                    );
                }
            );
    }

    private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
        CachedOutput pOutput, HolderLookup.Provider pRegistries, DynamicOps<JsonElement> pOps, RegistryDataLoader.RegistryData<T> pRegistryData
    ) {
        ResourceKey<? extends Registry<T>> resourcekey = pRegistryData.key();
        return pRegistries.lookup(resourcekey)
            .map(
                p_349921_ -> {
                    PackOutput.PathProvider packoutput$pathprovider = this.output.createRegistryElementsPathProvider(resourcekey);
                    return CompletableFuture.allOf(
                        p_349921_.listElements()
                            .filter(holder -> this.namespacePredicate.test(holder.key().location().getNamespace()))
                            .map(
                                p_256105_ -> dumpValue(
                                        packoutput$pathprovider.json(p_256105_.key().location()),
                                        pOutput,
                                        pOps,
                                        pRegistryData.elementCodec(),
                                        p_256105_.value()
                                    )
                            )
                            .toArray(CompletableFuture[]::new)
                    );
                }
            );
    }

    private static <E> CompletableFuture<?> dumpValue(
        Path pValuePath, CachedOutput pOutput, DynamicOps<JsonElement> pOps, Encoder<E> pEncoder, E pValue
    ) {
        return pEncoder.encodeStart(pOps, pValue)
            .mapOrElse(
                p_351699_ -> DataProvider.saveStable(pOutput, p_351699_, pValuePath),
                p_351701_ -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + pValuePath + "': " + p_351701_.message()))
            );
    }

    @Override
    public String getName() {
        return "Registries";
    }
}

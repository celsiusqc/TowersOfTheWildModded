package net.minecraft.data.advancements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * @deprecated NeoForge: Use {@link net.neoforged.neoforge.common.data.AdvancementProvider} instead,
 *                 provides ease of access for the {@link net.neoforged.neoforge.common.data.ExistingFileHelper} in the generator
 */
@Deprecated
public class AdvancementProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;
    private final List<AdvancementSubProvider> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public AdvancementProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries, List<AdvancementSubProvider> pSubProviders) {
        this.pathProvider = pOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
        this.subProviders = pSubProviders;
        this.registries = pRegistries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        return this.registries.thenCompose(p_323115_ -> {
            Set<ResourceLocation> set = new HashSet<>();
            List<CompletableFuture<?>> list = new ArrayList<>();
            Consumer<AdvancementHolder> consumer = p_339356_ -> {
                if (!set.add(p_339356_.id())) {
                    throw new IllegalStateException("Duplicate advancement " + p_339356_.id());
                } else {
                    Path path = this.pathProvider.json(p_339356_.id());
                    list.add(DataProvider.saveStable(pOutput, p_323115_, Advancement.CODEC, p_339356_.value(), path));// TODO: make conditional
                }
            };

            for (AdvancementSubProvider advancementsubprovider : this.subProviders) {
                advancementsubprovider.generate(p_323115_, consumer);
            }

            return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public final String getName() {
        return "Advancements";
    }
}

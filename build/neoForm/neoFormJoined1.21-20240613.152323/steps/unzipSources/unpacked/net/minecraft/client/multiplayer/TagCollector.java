package net.minecraft.client.multiplayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TagCollector {
    private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap<>();

    public void append(ResourceKey<? extends Registry<?>> pRegistryKey, TagNetworkSerialization.NetworkPayload pNetworkPayload) {
        this.tags.put(pRegistryKey, pNetworkPayload);
    }

    private static void refreshBuiltInTagDependentData() {
        AbstractFurnaceBlockEntity.invalidateCache();
        Blocks.rebuildCache();
    }

    private void applyTags(RegistryAccess pRegistryAccess, Predicate<ResourceKey<? extends Registry<?>>> pFilter) {
        this.tags.forEach((p_326303_, p_326438_) -> {
            if (pFilter.test((ResourceKey<? extends Registry<?>>)p_326303_)) {
                p_326438_.applyToRegistry(pRegistryAccess.registryOrThrow((ResourceKey<? extends Registry<?>>)p_326303_));
            }
        });
    }

    public void updateTags(RegistryAccess pRegistryAccess, boolean pIsMemoryConnection) {
        if (pIsMemoryConnection) {
            this.applyTags(pRegistryAccess, RegistrySynchronization.NETWORKABLE_REGISTRIES::contains);
        } else {
            pRegistryAccess.registries()
                .filter(p_325935_ -> !RegistrySynchronization.NETWORKABLE_REGISTRIES.contains(p_325935_.key()))
                .forEach(p_325919_ -> p_325919_.value().resetTags());
            this.applyTags(pRegistryAccess, p_326446_ -> true);
            refreshBuiltInTagDependentData();
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.TagsUpdatedEvent(pRegistryAccess, true, pIsMemoryConnection));
    }
}

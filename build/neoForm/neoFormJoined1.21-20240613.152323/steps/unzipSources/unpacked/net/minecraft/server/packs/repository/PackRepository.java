package net.minecraft.server.packs.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.server.packs.PackResources;
import net.minecraft.world.flag.FeatureFlagSet;

public class PackRepository {
    private final Set<RepositorySource> sources;
    private Map<String, Pack> available = ImmutableMap.of();
    private List<Pack> selected = ImmutableList.of();

    public PackRepository(RepositorySource... pSources) {
        this.sources = new java.util.LinkedHashSet<>(List.of(pSources)); //Neo: This needs to be a mutable set, so that we can add to it later on.
    }

    public static String displayPackList(Collection<Pack> pPacks) {
        return pPacks.stream()
            .map(p_341571_ -> p_341571_.getId() + (p_341571_.getCompatibility().isCompatible() ? "" : " (incompatible)"))
            .collect(Collectors.joining(", "));
    }

    public void reload() {
        List<String> list = this.selected.stream().map(Pack::getId).collect(ImmutableList.toImmutableList());
        this.available = this.discoverAvailable();
        this.selected = this.rebuildSelected(list);
    }

    private Map<String, Pack> discoverAvailable() {
        // Neo: sort packs within a source by name, between sources according to source order
        Map<String, Pack> map = Maps.newLinkedHashMap();

        for (RepositorySource repositorysource : this.sources) {
            Map<String, Pack> sourceMap = Maps.newTreeMap();
            repositorysource.loadPacks(p_143903_ -> p_143903_.streamSelfAndChildren().forEach(p -> sourceMap.put(p.getId(), p)));
            map.putAll(sourceMap);
        }

        return ImmutableMap.copyOf(map);
    }

    public void setSelected(Collection<String> pIds) {
        this.selected = this.rebuildSelected(pIds);
    }

    public boolean addPack(String pId) {
        Pack pack = this.available.get(pId);
        if (pack != null && !this.selected.contains(pack)) {
            List<Pack> list = Lists.newArrayList(this.selected);
            list.add(pack);
            this.selected = list;
            return true;
        } else {
            return false;
        }
    }

    public boolean removePack(String pId) {
        Pack pack = this.available.get(pId);
        if (pack != null && this.selected.contains(pack)) {
            List<Pack> list = Lists.newArrayList(this.selected);
            list.remove(pack);
            this.selected = list;
            return true;
        } else {
            return false;
        }
    }

    public List<Pack> rebuildSelected(Collection<String> pIds) {
        List<Pack> list = net.neoforged.neoforge.resource.ResourcePackLoader.expandAndRemoveRootChildren(this.getAvailablePacks(pIds), this.available.values());

        for (Pack pack : this.available.values()) {
            if (pack.isRequired() && !list.contains(pack)) {
                int i = pack.getDefaultPosition().insert(list, pack, Pack::selectionConfig, false);
                list.addAll(i + 1, pack.getChildren());
            }
        }

        return ImmutableList.copyOf(list);
    }

    private Stream<Pack> getAvailablePacks(Collection<String> pIds) {
        return pIds.stream().map(this.available::get).filter(Objects::nonNull);
    }

    public Collection<String> getAvailableIds() {
        return this.available.values().stream().filter(p -> !p.isHidden()).map(Pack::getId).collect(ImmutableSet.toImmutableSet());
    }

    public Collection<Pack> getAvailablePacks() {
        return this.available.values();
    }

    public Collection<String> getSelectedIds() {
        return this.selected.stream().filter(p -> !p.isHidden()).map(Pack::getId).collect(ImmutableSet.toImmutableSet());
    }

    public FeatureFlagSet getRequestedFeatureFlags() {
        return this.getSelectedPacks().stream().map(Pack::getRequestedFeatures).reduce(FeatureFlagSet::join).orElse(FeatureFlagSet.of());
    }

    public Collection<Pack> getSelectedPacks() {
        return this.selected;
    }

    @Nullable
    public Pack getPack(String pId) {
        return this.available.get(pId);
    }

    public synchronized void addPackFinder(RepositorySource packFinder) {
        this.sources.add(packFinder);
    }

    public boolean isAvailable(String pId) {
        return this.available.containsKey(pId);
    }

    public List<PackResources> openAllSelected() {
        return this.selected.stream().map(Pack::open).collect(ImmutableList.toImmutableList());
    }
}

package net.minecraft.core.component;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class PatchedDataComponentMap implements DataComponentMap {
    private final DataComponentMap prototype;
    private Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch;
    private boolean copyOnWrite;

    public PatchedDataComponentMap(DataComponentMap pPrototype) {
        this(pPrototype, Reference2ObjectMaps.emptyMap(), true);
    }

    private PatchedDataComponentMap(DataComponentMap pPrototype, Reference2ObjectMap<DataComponentType<?>, Optional<?>> pPatch, boolean pCopyOnWtite) {
        this.prototype = pPrototype;
        this.patch = pPatch;
        this.copyOnWrite = pCopyOnWtite;
    }

    public static PatchedDataComponentMap fromPatch(DataComponentMap pPrototype, DataComponentPatch pPatch) {
        if (isPatchSanitized(pPrototype, pPatch.map)) {
            return new PatchedDataComponentMap(pPrototype, pPatch.map, true);
        } else {
            PatchedDataComponentMap patcheddatacomponentmap = new PatchedDataComponentMap(pPrototype);
            patcheddatacomponentmap.applyPatch(pPatch);
            return patcheddatacomponentmap;
        }
    }

    private static boolean isPatchSanitized(DataComponentMap pPrototype, Reference2ObjectMap<DataComponentType<?>, Optional<?>> pMap) {
        for (Entry<DataComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(pMap)) {
            Object object = pPrototype.get(entry.getKey());
            Optional<?> optional = entry.getValue();
            if (optional.isPresent() && optional.get().equals(object)) {
                return false;
            }

            if (optional.isEmpty() && object == null) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> pComponent) {
        Optional<? extends T> optional = (Optional<? extends T>)this.patch.get(pComponent);
        return (T)(optional != null ? optional.orElse(null) : this.prototype.get(pComponent));
    }

    @Nullable
    public <T> T set(DataComponentType<? super T> pComponent, @Nullable T pValue) {
        net.neoforged.neoforge.common.CommonHooks.validateComponent(pValue);
        this.ensureMapOwnership();
        T t = this.prototype.get((DataComponentType<? extends T>)pComponent);
        Optional<T> optional;
        if (Objects.equals(pValue, t)) {
            optional = (Optional<T>)this.patch.remove(pComponent);
        } else {
            optional = (Optional<T>)this.patch.put(pComponent, Optional.ofNullable(pValue));
        }

        return optional != null ? optional.orElse(t) : t;
    }

    @Nullable
    public <T> T remove(DataComponentType<? extends T> pComponent) {
        this.ensureMapOwnership();
        T t = this.prototype.get(pComponent);
        Optional<? extends T> optional;
        if (t != null) {
            optional = (Optional<? extends T>)this.patch.put(pComponent, Optional.empty());
        } else {
            optional = (Optional<? extends T>)this.patch.remove(pComponent);
        }

        return (T)(optional != null ? optional.orElse(null) : t);
    }

    public void applyPatch(DataComponentPatch pPatch) {
        this.ensureMapOwnership();

        for (Entry<DataComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(pPatch.map)) {
            this.applyPatch(entry.getKey(), entry.getValue());
        }
    }

    private void applyPatch(DataComponentType<?> pComponent, Optional<?> pValue) {
        Object object = this.prototype.get(pComponent);
        if (pValue.isPresent()) {
            if (pValue.get().equals(object)) {
                this.patch.remove(pComponent);
            } else {
                this.patch.put(pComponent, pValue);
            }
        } else if (object != null) {
            this.patch.put(pComponent, Optional.empty());
        } else {
            this.patch.remove(pComponent);
        }
    }

    public void restorePatch(DataComponentPatch pPatch) {
        this.ensureMapOwnership();
        this.patch.clear();
        this.patch.putAll(pPatch.map);
    }

    public void setAll(DataComponentMap pMap) {
        for (TypedDataComponent<?> typeddatacomponent : pMap) {
            typeddatacomponent.applyTo(this);
        }
    }

    private void ensureMapOwnership() {
        if (this.copyOnWrite) {
            this.patch = new Reference2ObjectArrayMap<>(this.patch);
            this.copyOnWrite = false;
        }
    }

    @Override
    public Set<DataComponentType<?>> keySet() {
        if (this.patch.isEmpty()) {
            return this.prototype.keySet();
        } else {
            Set<DataComponentType<?>> set = new ReferenceArraySet<>(this.prototype.keySet());

            for (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(
                this.patch
            )) {
                Optional<?> optional = entry.getValue();
                if (optional.isPresent()) {
                    set.add(entry.getKey());
                } else {
                    set.remove(entry.getKey());
                }
            }

            return set;
        }
    }

    @Override
    public Iterator<TypedDataComponent<?>> iterator() {
        if (this.patch.isEmpty()) {
            return this.prototype.iterator();
        } else {
            List<TypedDataComponent<?>> list = new ArrayList<>(this.patch.size() + this.prototype.size());

            for (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(
                this.patch
            )) {
                if (entry.getValue().isPresent()) {
                    list.add(TypedDataComponent.createUnchecked(entry.getKey(), entry.getValue().get()));
                }
            }

            for (TypedDataComponent<?> typeddatacomponent : this.prototype) {
                if (!this.patch.containsKey(typeddatacomponent.type())) {
                    list.add(typeddatacomponent);
                }
            }

            return list.iterator();
        }
    }

    @Override
    public int size() {
        int i = this.prototype.size();

        for (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(this.patch)) {
            boolean flag = entry.getValue().isPresent();
            boolean flag1 = this.prototype.has(entry.getKey());
            if (flag != flag1) {
                i += flag ? 1 : -1;
            }
        }

        return i;
    }

    public boolean isPatchEmpty() {
        return this.patch.isEmpty();
    }

    public DataComponentPatch asPatch() {
        if (this.patch.isEmpty()) {
            return DataComponentPatch.EMPTY;
        } else {
            this.copyOnWrite = true;
            return new DataComponentPatch(this.patch);
        }
    }

    public PatchedDataComponentMap copy() {
        this.copyOnWrite = true;
        return new PatchedDataComponentMap(this.prototype, this.patch, true);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            if (pOther instanceof PatchedDataComponentMap patcheddatacomponentmap
                && this.prototype.equals(patcheddatacomponentmap.prototype)
                && this.patch.equals(patcheddatacomponentmap.patch)) {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.prototype.hashCode() + this.patch.hashCode() * 31;
    }

    @Override
    public String toString() {
        return "{" + this.stream().map(TypedDataComponent::toString).collect(Collectors.joining(", ")) + "}";
    }
}

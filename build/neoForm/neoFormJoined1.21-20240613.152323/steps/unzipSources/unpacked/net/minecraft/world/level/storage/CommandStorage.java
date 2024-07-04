package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class CommandStorage {
    private static final String ID_PREFIX = "command_storage_";
    private final Map<String, CommandStorage.Container> namespaces = Maps.newHashMap();
    private final DimensionDataStorage storage;

    public CommandStorage(DimensionDataStorage pStorage) {
        this.storage = pStorage;
    }

    private CommandStorage.Container newStorage(String pNamespace) {
        CommandStorage.Container commandstorage$container = new CommandStorage.Container();
        this.namespaces.put(pNamespace, commandstorage$container);
        return commandstorage$container;
    }

    private SavedData.Factory<CommandStorage.Container> factory(String pNamespace) {
        return new SavedData.Factory<>(
            () -> this.newStorage(pNamespace), (p_164844_, p_323732_) -> this.newStorage(pNamespace).load(p_164844_), DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        );
    }

    public CompoundTag get(ResourceLocation pId) {
        String s = pId.getNamespace();
        CommandStorage.Container commandstorage$container = this.storage.get(this.factory(s), createId(s));
        return commandstorage$container != null ? commandstorage$container.get(pId.getPath()) : new CompoundTag();
    }

    public void set(ResourceLocation pId, CompoundTag pNbt) {
        String s = pId.getNamespace();
        this.storage.computeIfAbsent(this.factory(s), createId(s)).put(pId.getPath(), pNbt);
    }

    public Stream<ResourceLocation> keys() {
        return this.namespaces.entrySet().stream().flatMap(p_164841_ -> p_164841_.getValue().getKeys(p_164841_.getKey()));
    }

    private static String createId(String pNamespace) {
        return "command_storage_" + pNamespace;
    }

    static class Container extends SavedData {
        private static final String TAG_CONTENTS = "contents";
        private final Map<String, CompoundTag> storage = Maps.newHashMap();

        CommandStorage.Container load(CompoundTag pCompoundTag) {
            CompoundTag compoundtag = pCompoundTag.getCompound("contents");

            for (String s : compoundtag.getAllKeys()) {
                this.storage.put(s, compoundtag.getCompound(s));
            }

            return this;
        }

        @Override
        public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
            CompoundTag compoundtag = new CompoundTag();
            this.storage.forEach((p_78070_, p_78071_) -> compoundtag.put(p_78070_, p_78071_.copy()));
            pTag.put("contents", compoundtag);
            return pTag;
        }

        public CompoundTag get(String pId) {
            CompoundTag compoundtag = this.storage.get(pId);
            return compoundtag != null ? compoundtag : new CompoundTag();
        }

        public void put(String pId, CompoundTag pNbt) {
            if (pNbt.isEmpty()) {
                this.storage.remove(pId);
            } else {
                this.storage.put(pId, pNbt);
            }

            this.setDirty();
        }

        public Stream<ResourceLocation> getKeys(String pNamespace) {
            return this.storage.keySet().stream().map(p_350257_ -> ResourceLocation.fromNamespaceAndPath(pNamespace, p_350257_));
        }
    }
}

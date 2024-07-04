package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Map;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class DimensionDataStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, SavedData> cache = Maps.newHashMap();
    private final DataFixer fixerUpper;
    private final HolderLookup.Provider registries;
    private final File dataFolder;

    public DimensionDataStorage(File pDataFolder, DataFixer pFixerUpper, HolderLookup.Provider pRegistries) {
        this.fixerUpper = pFixerUpper;
        this.dataFolder = pDataFolder;
        this.registries = pRegistries;
    }

    private File getDataFile(String pName) {
        return new File(this.dataFolder, pName + ".dat");
    }

    public <T extends SavedData> T computeIfAbsent(SavedData.Factory<T> pFactory, String pName) {
        T t = this.get(pFactory, pName);
        if (t != null) {
            return t;
        } else {
            T t1 = (T)pFactory.constructor().get();
            this.set(pName, t1);
            return t1;
        }
    }

    @Nullable
    public <T extends SavedData> T get(SavedData.Factory<T> pFactory, String pName) {
        SavedData saveddata = this.cache.get(pName);
        if (saveddata == net.neoforged.neoforge.common.util.DummySavedData.DUMMY) return null;
        if (saveddata == null && !this.cache.containsKey(pName)) {
            saveddata = this.readSavedData(pFactory.deserializer(), pFactory.type(), pName);
            this.cache.put(pName, saveddata);
        } else if (saveddata == null) {
            this.cache.put(pName, net.neoforged.neoforge.common.util.DummySavedData.DUMMY);
            return null;
        }

        return (T)saveddata;
    }

    @Nullable
    private <T extends SavedData> T readSavedData(BiFunction<CompoundTag, HolderLookup.Provider, T> pReader, @Nullable DataFixTypes pDataFixType, String pFilename) {
        try {
            File file1 = this.getDataFile(pFilename);
            if (file1.exists()) {
                CompoundTag compoundtag = this.readTagFromDisk(pFilename, pDataFixType, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                return pReader.apply(compoundtag.getCompound("data"), this.registries);
            }
        } catch (Exception exception) {
            LOGGER.error("Error loading saved data: {}", pFilename, exception);
        }

        return null;
    }

    public void set(String pName, SavedData pSavedData) {
        this.cache.put(pName, pSavedData);
    }

    public CompoundTag readTagFromDisk(String pFilename, @Nullable DataFixTypes pDataFixType, int pVersion) throws IOException {
        File file1 = this.getDataFile(pFilename);

        CompoundTag compoundtag1;
        try (
            InputStream inputstream = new FileInputStream(file1);
            PushbackInputStream pushbackinputstream = new PushbackInputStream(new FastBufferedInputStream(inputstream), 2);
        ) {
            CompoundTag compoundtag;
            if (this.isGzip(pushbackinputstream)) {
                compoundtag = NbtIo.readCompressed(pushbackinputstream, NbtAccounter.unlimitedHeap());
            } else {
                try (DataInputStream datainputstream = new DataInputStream(pushbackinputstream)) {
                    compoundtag = NbtIo.read(datainputstream);
                }
            }

            if (pDataFixType != null) {
                int i = NbtUtils.getDataVersion(compoundtag, 1343);
                compoundtag1 = pDataFixType.update(this.fixerUpper, compoundtag, i, pVersion);
            } else {
                compoundtag1 = compoundtag;
            }
        }

        return compoundtag1;
    }

    private boolean isGzip(PushbackInputStream pInputStream) throws IOException {
        byte[] abyte = new byte[2];
        boolean flag = false;
        int i = pInputStream.read(abyte, 0, 2);
        if (i == 2) {
            int j = (abyte[1] & 255) << 8 | abyte[0] & 255;
            if (j == 35615) {
                flag = true;
            }
        }

        if (i != 0) {
            pInputStream.unread(abyte, 0, i);
        }

        return flag;
    }

    public void save() {
        this.cache.forEach((p_323449_, p_323450_) -> {
            if (p_323450_ != null) {
                p_323450_.save(this.getDataFile(p_323449_), this.registries);
            }
        });
    }
}

package net.minecraft.world.level.levelgen.structure;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class StructureFeatureIndexSavedData extends SavedData {
    private static final String TAG_REMAINING_INDEXES = "Remaining";
    private static final String TAG_All_INDEXES = "All";
    private final LongSet all;
    private final LongSet remaining;

    public static SavedData.Factory<StructureFeatureIndexSavedData> factory() {
        return new SavedData.Factory<>(
            StructureFeatureIndexSavedData::new, StructureFeatureIndexSavedData::load, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES
        );
    }

    private StructureFeatureIndexSavedData(LongSet pAll, LongSet pRemaining) {
        this.all = pAll;
        this.remaining = pRemaining;
    }

    public StructureFeatureIndexSavedData() {
        this(new LongOpenHashSet(), new LongOpenHashSet());
    }

    public static StructureFeatureIndexSavedData load(CompoundTag p_163535_, HolderLookup.Provider p_323754_) {
        return new StructureFeatureIndexSavedData(new LongOpenHashSet(p_163535_.getLongArray("All")), new LongOpenHashSet(p_163535_.getLongArray("Remaining")));
    }

    @Override
    public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.putLongArray("All", this.all.toLongArray());
        pTag.putLongArray("Remaining", this.remaining.toLongArray());
        return pTag;
    }

    public void addIndex(long pIndex) {
        this.all.add(pIndex);
        this.remaining.add(pIndex);
    }

    public boolean hasStartIndex(long pIndex) {
        return this.all.contains(pIndex);
    }

    public boolean hasUnhandledIndex(long pIndex) {
        return this.remaining.contains(pIndex);
    }

    public void removeIndex(long pIndex) {
        this.remaining.remove(pIndex);
    }

    public LongSet getAll() {
        return this.all;
    }
}

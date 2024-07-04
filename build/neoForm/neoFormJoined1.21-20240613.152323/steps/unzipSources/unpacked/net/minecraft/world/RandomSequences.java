package net.minecraft.world;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class RandomSequences extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final long worldSeed;
    private int salt;
    private boolean includeWorldSeed = true;
    private boolean includeSequenceId = true;
    private final Map<ResourceLocation, RandomSequence> sequences = new Object2ObjectOpenHashMap<>();

    public static SavedData.Factory<RandomSequences> factory(long pSeed) {
        return new SavedData.Factory<>(
            () -> new RandomSequences(pSeed), (p_293846_, p_324262_) -> load(pSeed, p_293846_), DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
        );
    }

    public RandomSequences(long pSeed) {
        this.worldSeed = pSeed;
    }

    public RandomSource get(ResourceLocation pLocation) {
        RandomSource randomsource = this.sequences.computeIfAbsent(pLocation, this::createSequence).random();
        return new RandomSequences.DirtyMarkingRandomSource(randomsource);
    }

    private RandomSequence createSequence(ResourceLocation p_295796_) {
        return this.createSequence(p_295796_, this.salt, this.includeWorldSeed, this.includeSequenceId);
    }

    private RandomSequence createSequence(ResourceLocation pLocation, int pSalt, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
        long i = (pIncludeWorldSeed ? this.worldSeed : 0L) ^ (long)pSalt;
        return new RandomSequence(i, pIncludeSequenceId ? Optional.of(pLocation) : Optional.empty());
    }

    public void forAllSequences(BiConsumer<ResourceLocation, RandomSequence> pAction) {
        this.sequences.forEach(pAction);
    }

    public void setSeedDefaults(int pSalt, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
        this.salt = pSalt;
        this.includeWorldSeed = pIncludeWorldSeed;
        this.includeSequenceId = pIncludeSequenceId;
    }

    @Override
    public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.putInt("salt", this.salt);
        pTag.putBoolean("include_world_seed", this.includeWorldSeed);
        pTag.putBoolean("include_sequence_id", this.includeSequenceId);
        CompoundTag compoundtag = new CompoundTag();
        this.sequences
            .forEach(
                (p_337697_, p_337698_) -> compoundtag.put(
                        p_337697_.toString(), RandomSequence.CODEC.encodeStart(NbtOps.INSTANCE, p_337698_).result().orElseThrow()
                    )
            );
        pTag.put("sequences", compoundtag);
        return pTag;
    }

    private static boolean getBooleanWithDefault(CompoundTag pTag, String pKey, boolean pDefaultValue) {
        return pTag.contains(pKey, 1) ? pTag.getBoolean(pKey) : pDefaultValue;
    }

    public static RandomSequences load(long pSeed, CompoundTag pTag) {
        RandomSequences randomsequences = new RandomSequences(pSeed);
        randomsequences.setSeedDefaults(
            pTag.getInt("salt"),
            getBooleanWithDefault(pTag, "include_world_seed", true),
            getBooleanWithDefault(pTag, "include_sequence_id", true)
        );
        CompoundTag compoundtag = pTag.getCompound("sequences");

        for (String s : compoundtag.getAllKeys()) {
            try {
                RandomSequence randomsequence = RandomSequence.CODEC.decode(NbtOps.INSTANCE, compoundtag.get(s)).result().get().getFirst();
                randomsequences.sequences.put(ResourceLocation.parse(s), randomsequence);
            } catch (Exception exception) {
                LOGGER.error("Failed to load random sequence {}", s, exception);
            }
        }

        return randomsequences;
    }

    public int clear() {
        int i = this.sequences.size();
        this.sequences.clear();
        return i;
    }

    public void reset(ResourceLocation pSequence) {
        this.sequences.put(pSequence, this.createSequence(pSequence));
    }

    public void reset(ResourceLocation pSequence, int pSeed, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
        this.sequences.put(pSequence, this.createSequence(pSequence, pSeed, pIncludeWorldSeed, pIncludeSequenceId));
    }

    class DirtyMarkingRandomSource implements RandomSource {
        private final RandomSource random;

        DirtyMarkingRandomSource(RandomSource pRandom) {
            this.random = pRandom;
        }

        @Override
        public RandomSource fork() {
            RandomSequences.this.setDirty();
            return this.random.fork();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            RandomSequences.this.setDirty();
            return this.random.forkPositional();
        }

        @Override
        public void setSeed(long pSeed) {
            RandomSequences.this.setDirty();
            this.random.setSeed(pSeed);
        }

        @Override
        public int nextInt() {
            RandomSequences.this.setDirty();
            return this.random.nextInt();
        }

        @Override
        public int nextInt(int pBound) {
            RandomSequences.this.setDirty();
            return this.random.nextInt(pBound);
        }

        @Override
        public long nextLong() {
            RandomSequences.this.setDirty();
            return this.random.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            RandomSequences.this.setDirty();
            return this.random.nextBoolean();
        }

        @Override
        public float nextFloat() {
            RandomSequences.this.setDirty();
            return this.random.nextFloat();
        }

        @Override
        public double nextDouble() {
            RandomSequences.this.setDirty();
            return this.random.nextDouble();
        }

        @Override
        public double nextGaussian() {
            RandomSequences.this.setDirty();
            return this.random.nextGaussian();
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else {
                return pOther instanceof RandomSequences.DirtyMarkingRandomSource randomsequences$dirtymarkingrandomsource
                    ? this.random.equals(randomsequences$dirtymarkingrandomsource.random)
                    : false;
            }
        }
    }
}

package net.minecraft.world.item.component;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public final class CustomData {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final CustomData EMPTY = new CustomData(new CompoundTag());
    public static final Codec<CustomData> CODEC = Codec.withAlternative(CompoundTag.CODEC, TagParser.AS_CODEC)
        .xmap(CustomData::new, p_331996_ -> p_331996_.tag);
    public static final Codec<CustomData> CODEC_WITH_ID = CODEC.validate(
        p_331848_ -> p_331848_.getUnsafe().contains("id", 8) ? DataResult.success(p_331848_) : DataResult.error(() -> "Missing id for entity in: " + p_331848_)
    );
    @Deprecated
    public static final StreamCodec<ByteBuf, CustomData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, p_331280_ -> p_331280_.tag);
    private final CompoundTag tag;

    private CustomData(CompoundTag p_331863_) {
        this.tag = p_331863_;
    }

    public static CustomData of(CompoundTag pTag) {
        return new CustomData(pTag.copy());
    }

    public static Predicate<ItemStack> itemMatcher(DataComponentType<CustomData> pComponentType, CompoundTag pTag) {
        return p_332154_ -> {
            CustomData customdata = p_332154_.getOrDefault(pComponentType, EMPTY);
            return customdata.matchedBy(pTag);
        };
    }

    public boolean matchedBy(CompoundTag pTag) {
        return NbtUtils.compareNbt(pTag, this.tag, true);
    }

    public static void update(DataComponentType<CustomData> pComponentType, ItemStack pStack, Consumer<CompoundTag> pUpdater) {
        CustomData customdata = pStack.getOrDefault(pComponentType, EMPTY).update(pUpdater);
        if (customdata.tag.isEmpty()) {
            pStack.remove(pComponentType);
        } else {
            pStack.set(pComponentType, customdata);
        }
    }

    public static void set(DataComponentType<CustomData> pComponentType, ItemStack pStack, CompoundTag pTag) {
        if (!pTag.isEmpty()) {
            pStack.set(pComponentType, of(pTag));
        } else {
            pStack.remove(pComponentType);
        }
    }

    public CustomData update(Consumer<CompoundTag> pUpdater) {
        CompoundTag compoundtag = this.tag.copy();
        pUpdater.accept(compoundtag);
        return new CustomData(compoundtag);
    }

    public void loadInto(Entity pEntity) {
        CompoundTag compoundtag = pEntity.saveWithoutId(new CompoundTag());
        UUID uuid = pEntity.getUUID();
        compoundtag.merge(this.tag);
        pEntity.load(compoundtag);
        pEntity.setUUID(uuid);
    }

    public boolean loadInto(BlockEntity pBlockEntity, HolderLookup.Provider pLevelRegistry) {
        CompoundTag compoundtag = pBlockEntity.saveCustomOnly(pLevelRegistry);
        CompoundTag compoundtag1 = compoundtag.copy();
        compoundtag.merge(this.tag);
        if (!compoundtag.equals(compoundtag1)) {
            try {
                pBlockEntity.loadCustomOnly(compoundtag, pLevelRegistry);
                pBlockEntity.setChanged();
                return true;
            } catch (Exception exception1) {
                LOGGER.warn("Failed to apply custom data to block entity at {}", pBlockEntity.getBlockPos(), exception1);

                try {
                    pBlockEntity.loadCustomOnly(compoundtag1, pLevelRegistry);
                } catch (Exception exception) {
                    LOGGER.warn("Failed to rollback block entity at {} after failure", pBlockEntity.getBlockPos(), exception);
                }
            }
        }

        return false;
    }

    public <T> DataResult<CustomData> update(DynamicOps<Tag> p_346001_, MapEncoder<T> p_331823_, T p_332045_) {
        return p_331823_.encode(p_332045_, p_346001_, p_346001_.mapBuilder()).build(this.tag).map(p_330397_ -> new CustomData((CompoundTag)p_330397_));
    }

    public <T> DataResult<T> read(MapDecoder<T> pDecoder) {
        return this.read(NbtOps.INSTANCE, pDecoder);
    }

    public <T> DataResult<T> read(DynamicOps<Tag> p_346230_, MapDecoder<T> p_344951_) {
        MapLike<Tag> maplike = p_346230_.getMap(this.tag).getOrThrow();
        return p_344951_.decode(p_346230_, maplike);
    }

    public int size() {
        return this.tag.size();
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    public CompoundTag copyTag() {
        return this.tag.copy();
    }

    public boolean contains(String pKey) {
        return this.tag.contains(pKey);
    }

    @Override
    public boolean equals(Object pOther) {
        if (pOther == this) {
            return true;
        } else {
            return pOther instanceof CustomData customdata ? this.tag.equals(customdata.tag) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.tag.hashCode();
    }

    @Override
    public String toString() {
        return this.tag.toString();
    }

    @Deprecated
    public CompoundTag getUnsafe() {
        return this.tag;
    }
}

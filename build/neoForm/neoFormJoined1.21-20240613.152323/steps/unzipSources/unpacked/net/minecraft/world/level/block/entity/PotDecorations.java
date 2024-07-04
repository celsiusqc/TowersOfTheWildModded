package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record PotDecorations(Optional<Item> back, Optional<Item> left, Optional<Item> right, Optional<Item> front) {
    public static final PotDecorations EMPTY = new PotDecorations(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    public static final Codec<PotDecorations> CODEC = BuiltInRegistries.ITEM
        .byNameCodec()
        .sizeLimitedListOf(4)
        .xmap(PotDecorations::new, PotDecorations::ordered);
    public static final StreamCodec<RegistryFriendlyByteBuf, PotDecorations> STREAM_CODEC = ByteBufCodecs.registry(Registries.ITEM)
        .apply(ByteBufCodecs.list(4))
        .map(PotDecorations::new, PotDecorations::ordered);

    private PotDecorations(List<Item> p_331803_) {
        this(getItem(p_331803_, 0), getItem(p_331803_, 1), getItem(p_331803_, 2), getItem(p_331803_, 3));
    }

    public PotDecorations(Item p_331754_, Item p_331488_, Item p_331845_, Item p_330988_) {
        this(List.of(p_331754_, p_331488_, p_331845_, p_330988_));
    }

    private static Optional<Item> getItem(List<Item> pDecorations, int pIndex) {
        if (pIndex >= pDecorations.size()) {
            return Optional.empty();
        } else {
            Item item = pDecorations.get(pIndex);
            return item == Items.BRICK ? Optional.empty() : Optional.of(item);
        }
    }

    public CompoundTag save(CompoundTag pTag) {
        if (this.equals(EMPTY)) {
            return pTag;
        } else {
            pTag.put("sherds", CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
            return pTag;
        }
    }

    public List<Item> ordered() {
        return Stream.of(this.back, this.left, this.right, this.front).map(p_331733_ -> p_331733_.orElse(Items.BRICK)).toList();
    }

    public static PotDecorations load(@Nullable CompoundTag pTag) {
        return pTag != null && pTag.contains("sherds") ? CODEC.parse(NbtOps.INSTANCE, pTag.get("sherds")).result().orElse(EMPTY) : EMPTY;
    }
}

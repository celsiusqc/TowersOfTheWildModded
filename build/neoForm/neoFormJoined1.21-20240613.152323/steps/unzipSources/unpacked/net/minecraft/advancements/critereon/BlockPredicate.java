package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public record BlockPredicate(Optional<HolderSet<Block>> blocks, Optional<StatePropertiesPredicate> properties, Optional<NbtPredicate> nbt) {
    public static final Codec<BlockPredicate> CODEC = RecordCodecBuilder.create(
        p_337342_ -> p_337342_.group(
                    RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("blocks").forGetter(BlockPredicate::blocks),
                    StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(BlockPredicate::properties),
                    NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(BlockPredicate::nbt)
                )
                .apply(p_337342_, BlockPredicate::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPredicate> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.BLOCK)),
        BlockPredicate::blocks,
        ByteBufCodecs.optional(StatePropertiesPredicate.STREAM_CODEC),
        BlockPredicate::properties,
        ByteBufCodecs.optional(NbtPredicate.STREAM_CODEC),
        BlockPredicate::nbt,
        BlockPredicate::new
    );

    public boolean matches(ServerLevel pLevel, BlockPos pPos) {
        if (!pLevel.isLoaded(pPos)) {
            return false;
        } else {
            return !this.matchesState(pLevel.getBlockState(pPos))
                ? false
                : !this.nbt.isPresent() || matchesBlockEntity(pLevel, pLevel.getBlockEntity(pPos), this.nbt.get());
        }
    }

    public boolean matches(BlockInWorld pBlock) {
        return !this.matchesState(pBlock.getState())
            ? false
            : !this.nbt.isPresent() || matchesBlockEntity(pBlock.getLevel(), pBlock.getEntity(), this.nbt.get());
    }

    private boolean matchesState(BlockState pState) {
        return this.blocks.isPresent() && !pState.is(this.blocks.get()) ? false : !this.properties.isPresent() || this.properties.get().matches(pState);
    }

    private static boolean matchesBlockEntity(LevelReader pLevel, @Nullable BlockEntity pBlockEntity, NbtPredicate pNbtPredicate) {
        return pBlockEntity != null && pNbtPredicate.matches(pBlockEntity.saveWithFullMetadata(pLevel.registryAccess()));
    }

    public boolean requiresNbt() {
        return this.nbt.isPresent();
    }

    public static class Builder {
        private Optional<HolderSet<Block>> blocks = Optional.empty();
        private Optional<StatePropertiesPredicate> properties = Optional.empty();
        private Optional<NbtPredicate> nbt = Optional.empty();

        private Builder() {
        }

        public static BlockPredicate.Builder block() {
            return new BlockPredicate.Builder();
        }

        public BlockPredicate.Builder of(Block... pBlocks) {
            this.blocks = Optional.of(HolderSet.direct(Block::builtInRegistryHolder, pBlocks));
            return this;
        }

        public BlockPredicate.Builder of(Collection<Block> pBlocks) {
            this.blocks = Optional.of(HolderSet.direct(Block::builtInRegistryHolder, pBlocks));
            return this;
        }

        public BlockPredicate.Builder of(TagKey<Block> pTag) {
            this.blocks = Optional.of(BuiltInRegistries.BLOCK.getOrCreateTag(pTag));
            return this;
        }

        public BlockPredicate.Builder hasNbt(CompoundTag pNbt) {
            this.nbt = Optional.of(new NbtPredicate(pNbt));
            return this;
        }

        public BlockPredicate.Builder setProperties(StatePropertiesPredicate.Builder pProperties) {
            this.properties = pProperties.build();
            return this;
        }

        public BlockPredicate build() {
            return new BlockPredicate(this.blocks, this.properties, this.nbt);
        }
    }
}

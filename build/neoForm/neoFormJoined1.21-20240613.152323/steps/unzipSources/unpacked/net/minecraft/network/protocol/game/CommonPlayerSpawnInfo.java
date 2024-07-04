package net.minecraft.network.protocol.game;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public record CommonPlayerSpawnInfo(
    Holder<DimensionType> dimensionType,
    ResourceKey<Level> dimension,
    long seed,
    GameType gameType,
    @Nullable GameType previousGameType,
    boolean isDebug,
    boolean isFlat,
    Optional<GlobalPos> lastDeathLocation,
    int portalCooldown
) {
    public CommonPlayerSpawnInfo(RegistryFriendlyByteBuf p_321843_) {
        this(
            DimensionType.STREAM_CODEC.decode(p_321843_),
            p_321843_.readResourceKey(Registries.DIMENSION),
            p_321843_.readLong(),
            GameType.byId(p_321843_.readByte()),
            GameType.byNullableId(p_321843_.readByte()),
            p_321843_.readBoolean(),
            p_321843_.readBoolean(),
            p_321843_.readOptional(FriendlyByteBuf::readGlobalPos),
            p_321843_.readVarInt()
        );
    }

    public void write(RegistryFriendlyByteBuf pBuffer) {
        DimensionType.STREAM_CODEC.encode(pBuffer, this.dimensionType);
        pBuffer.writeResourceKey(this.dimension);
        pBuffer.writeLong(this.seed);
        pBuffer.writeByte(this.gameType.getId());
        pBuffer.writeByte(GameType.getNullableId(this.previousGameType));
        pBuffer.writeBoolean(this.isDebug);
        pBuffer.writeBoolean(this.isFlat);
        pBuffer.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
        pBuffer.writeVarInt(this.portalCooldown);
    }
}
